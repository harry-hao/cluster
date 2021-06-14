package com.github.harry_hao.cluster.loadbalancer;

import com.github.harry_hao.cluster.Cluster;
import com.github.harry_hao.cluster.Node;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * DefaultLoadBalancer use ConsistentHashing to load balance.
 */
@Slf4j
public class DefaultLoadBalancer implements LoadBalancer {
    private Cluster cluster;

    private Ring ring;

    private Scheduler scheduler;

    private Disposable myWork;

    public DefaultLoadBalancer(Cluster cluster, int replicaNumber) {
        this.cluster = cluster;
        this.ring = new Ring(replicaNumber);
        this.scheduler = Schedulers.single();
    }

    @Override
    public Mono<Node> loadBalance(String key) {
        return Mono.fromSupplier(() -> this.ring.get(key))
                .switchIfEmpty(Mono.error(new LoadBalancerException("no target available")))
                .subscribeOn(this.scheduler)
                .publishOn(Schedulers.immediate());
    }

    @Override
    public void start() {
        this.myWork = this.cluster.members()
                .publishOn(this.scheduler)
                .doOnNext(members -> updateRing(members))
                .subscribe();
    }

    @Override
    public void stop() {
        if (this.myWork == null) {
            return;
        }
        this.myWork.dispose();
    }

    private void updateRing(List<Node> members) {
        this.ring.clear();
        for (Node member : members) {
            this.ring.add(member);
        }
    }
}
