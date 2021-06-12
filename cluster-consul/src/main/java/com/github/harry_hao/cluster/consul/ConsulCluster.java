package com.github.harry_hao.cluster.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;
import com.github.harry_hao.cluster.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class ConsulCluster implements Cluster {

    private String name;

    private ConsulClient client;

    private ConsulCodec codec;

    private Duration sessionTimeout;

    private Duration sessionRenewPeriod;

    private Duration leaderCheckInterval;

    private Duration leaderCheckFallback;

    private Duration memberRefreshInterval;

    private Scheduler myThread;

    private Sinks.Many<ConsulNode> leaderSink;

    private Flux<Node> leaderFlux;

    private Sinks.Many<List<ConsulNode>> memberSink;

    private Flux<List<Node>> memberFlux;

    private Disposable disposable;

    public ConsulCluster(String name, ConsulClient client, ConsulCodec codec,
                         Duration sessionTimeout, Duration sessionRenewPeriod,
                         Duration leaderCheckInterval, Duration leaderCheckFallback,
                         Duration memberRefreshInterval, String myId, Scheduler myThread) {
        this.name = name;
        this.client = client;
        this.codec = codec;
        this.sessionTimeout = sessionTimeout;
        this.sessionRenewPeriod = sessionRenewPeriod;
        this.leaderCheckInterval = leaderCheckInterval;
        this.leaderCheckFallback = leaderCheckFallback;
        this.memberRefreshInterval = memberRefreshInterval;
        this.myThread = myThread;
        this.leaderSink = Sinks.many().replay().latest();
        this.leaderFlux = null; // will be set in join
        this.memberSink = Sinks.many().replay().latest();
        this.memberFlux = null; // will be set in join
    }


    @Override
    public void join(String myId, String metadata) {
        this.leaderFlux = this.leaderSink.asFlux()
                .map(node -> new Node(node.getId(), node.getMetadata(), node.getId().equals(myId)));

        this.memberFlux = this.memberSink.asFlux()
                .map(nodes -> {
                    List<Node> result = new ArrayList<>(nodes.size());
                    for (ConsulNode node : nodes) {
                        result.add(new Node(node.getId(), node.getMetadata(), node.getId().equals(myId)));
                    }
                    return result;
                });

        this.disposable = createSession()
                .flatMap(session -> {
                    ConsulNode mySelf = new ConsulNode(myId, metadata);

                    Mono<Void> sessionLoop = Flux.interval(this.sessionRenewPeriod)
                            .flatMap(i -> renewSession(session))
                            .log()
                            .then();

                    Mono<Void> leaderLoop = Flux.interval(this.leaderCheckInterval)
                            .concatMap(i -> getOrTakeLeadership(session, mySelf))
                            .distinctUntilChanged()
                            .doOnNext(leader -> {
                                Sinks.EmitResult result = this.leaderSink.tryEmitNext(leader);
                                if (result.isSuccess()) {
                                    log.warn("fail to emit leader | result={}", result);
                                }
                            })
                            .then();

                    Mono<Void> memberLoop = Flux.interval(this.memberRefreshInterval)
                            .concatMap(i -> getMembers(session))
                            .distinctUntilChanged()
                            .doOnNext(members -> {
                                Sinks.EmitResult result = this.memberSink.tryEmitNext(members);
                                if (!result.isSuccess()) {
                                    log.warn("fail to emit members | result={}", result);
                                }
                            })
                            .then();

                    return registerMySelf(session, mySelf)
                            .log("registerNode")
                            .then(Mono.when(sessionLoop, leaderLoop, memberLoop))
                            .doOnCancel(() -> destroySession(session).subscribe());
                }).subscribe();
    }

    @Override
    public void leave() {
        if (this.disposable != null) {
            this.disposable.dispose();
        }
    }

    @Override
    public Flux<List<Node>> members() {
        return this.memberFlux;
    }

    @Override
    public Flux<Node> leader() {
        return this.leaderFlux;
    }

    private Mono<String> createSession() {
        return Mono.<String>create(sink -> {
            String name = String.format("%s/%s", this.name, ManagementFactory.getRuntimeMXBean().getName());
            // todo: investigate to use finer time unit then seconds
            String ttl = String.format("%ss", this.sessionTimeout.getSeconds());

            NewSession newSession = new NewSession();
            newSession.setName(name);
            newSession.setTtl(ttl);
            newSession.setBehavior(Session.Behavior.DELETE);

            log.debug("create session | name={}", name);
            String session = this.client.sessionCreate(newSession, QueryParams.DEFAULT).getValue();
            log.debug("create session success | name={} session={}", name, session);

            sink.success(session);
        }).checkpoint("create session");
    }

    private Mono<Void> renewSession(String session) {
        return Mono.<Void>fromRunnable(() -> {
            log.debug("renew session | session={}", session);
            this.client.renewSession(session, QueryParams.DEFAULT);
            log.debug("renew session success | session={}", session);
        })
                .checkpoint(String.format("renew session `%s`", session));
    }

    private Mono<Void> destroySession(String session) {
        return Mono.<Void>fromRunnable(() -> {
            log.debug("destroy session | session={}", session);
            this.client.sessionDestroy(session, QueryParams.DEFAULT);
            log.debug("destroy session success | session={}", session);
        })
                .checkpoint(String.format("destroy session `%s`", session));
    }

    private Mono<Void> registerMySelf(String session, ConsulNode mySelf) {
        return Mono.<Void>fromRunnable(() -> {
            PutParams params = new PutParams();
            params.setAcquireSession(session);
            log.debug("register node | session={} node={}", session, mySelf);
            this.client.setKVValue(nodeKey(mySelf.getId()), this.codec.encode(mySelf), params);
            log.debug("register node success | session={} node={}", session, mySelf);
        }).checkpoint("register myself");
    }

    private Mono<ConsulNode> getCurrentLeader(String session) {
        return Mono.fromSupplier(() -> {
            String key = leaderKey();
            log.debug("get current leader | session={} key={}", session, key);
            GetValue result = this.client.getKVValue(key).getValue();
            if (result == null) {
                log.debug("get current leader failed | session={}", session);
                return null;
            }
            String leaderNode = result.getDecodedValue();
            log.debug("get current leader success | session={} leader={}", session, leaderNode);
            return leaderNode;
        }).flatMap(id -> readNode(id)).checkpoint("get current leader");
    }

    private Mono<ConsulNode> acquireLeadership(String session, ConsulNode mySelf) {
        return Mono.fromSupplier(() -> {
            String key = leaderKey();
            String value = mySelf.getId();
            PutParams params = new PutParams();
            params.setAcquireSession(session);

            log.debug("acquire leadership | session={} self={}", session, mySelf);
            boolean success = this.client.setKVValue(key, value, params).getValue();
            if (!success) {
                log.debug("acquire leadership failed | session={} self={}", session);
                return null;
            }
            log.debug("acquire leadership success | session={} self={}", session);
            return mySelf;
        }).checkpoint("take leadership");
    }

    private Mono<ConsulNode> getOrTakeLeadership(String session, ConsulNode mySelf) {
        return getCurrentLeader(session)
                .switchIfEmpty(acquireLeadership(session, mySelf))
                .repeatWhenEmpty(i -> {
                    long nanos = ThreadLocalRandom.current().nextLong(this.leaderCheckFallback.toNanos());
                    return Mono.delay(Duration.ofNanos(nanos));
            }).checkpoint(String.format("get or take leadership of cluster `%s`", this.name));
    }

    private Mono<List<ConsulNode>> getMembers(String session) {
        return Mono.fromSupplier(() -> {

            log.debug("get members | session={}", session);

            List<GetValue> response = this.client.getKVValues(nodePrefix()).getValue();

            if (response == null) {
                log.debug("get members failed | session={}", session);
                return null;
            }

            List<ConsulNode> members = new ArrayList<>();
            for (GetValue entry : response) {
                members.add(this.codec.decode(entry.getDecodedValue()));
            }

            members.sort(Comparator.comparing(ConsulNode::getId));

            log.debug("get members success | session={} members={}", session, members);
            return members;
        }).checkpoint(String.format("get cluster `%s` members", this.name));
    }

    private Mono<ConsulNode> readNode(String id) {
        return Mono.<ConsulNode>fromSupplier(() -> {
            GetValue result = this.client.getKVValue(nodeKey(id)).getValue();
            if (result == null) {
                return null;
            }
            return this.codec.decode(result.getDecodedValue());
        }).checkpoint(String.format("read cluster `%s` member `%s`", this.name, id));
    }

    private String nodePrefix() {
        return String.format("%s/node", this.name);
    }

    private String nodeKey(String id) {
        return String.format("%s/%s", nodePrefix(), id);
    }

    private String leaderKey() {
        return String.format("%s/leader", this.name);
    }
}
