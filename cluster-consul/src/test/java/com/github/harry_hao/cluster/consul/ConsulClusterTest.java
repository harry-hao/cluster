package com.github.harry_hao.cluster.consul;


import com.ecwid.consul.v1.ConsulClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.harry_hao.cluster.Node;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Testcontainers
public class ConsulClusterTest {
    Duration TIMEOUT = Duration.ofSeconds(15);

    @SuppressWarnings("rawtypes")
    @Container
    GenericContainer consul = new GenericContainer(DockerImageName.parse("consul")).withExposedPorts(8500);

    String consulHost;
    int consulPort;

    @BeforeEach
    public void setup() {
        this.consulHost = this.consul.getHost();
        this.consulPort = this.consul.getFirstMappedPort();
    }


    @Test
    public void testLeadershipFailOver() {

        ConsulClient client1 = consulClient();

        ConsulClient client2 = consulClient();

        ConsulCluster node1 = consulCluster(client1, "node1");

        ConsulCluster node2 = consulCluster(client2, "node2");

        node1.join("node1", "☃︎");

        node1.leader()
                .next()
                .map(Node::getId)
                .as(StepVerifier::create)
                .expectNext("node1")
                .expectComplete()
                .verify(TIMEOUT);

        node2.join("node2", "♛");

        node2.leader()
                .next()
                .map(Node::getId)
                .as(StepVerifier::create)
                .expectNext("node1")
                .expectComplete()
                .verify(TIMEOUT);

        node1.leave();

        node2.leader()
                .map(Node::getId)
                .filter(id -> id.equals("node2"))
                .as(StepVerifier::create)
                .expectNext("node2")
                .thenCancel()
                .verify(TIMEOUT);

        node2.leave();
    }

    @Test
    public void testMembers() {
        ConsulClient client1 = consulClient();

        ConsulClient client2 = consulClient();

        ConsulCluster node1 = consulCluster(client1, "node1");

        ConsulCluster node2 = consulCluster(client2, "node2");

        node1.join("node1", "♘");

        node2.join("node2", "♞");

        List<Node> members1 = new ArrayList<>();
        members1.add(new Node("node1", "♘",  true));
        members1.add(new Node("node2", "♞",  false));

        List<Node> members2 = new ArrayList<>();
        members2.add(new Node("node1", "♘",  false));
        members2.add(new Node("node2", "♞",  true));

        node1.members()
                .filter(members -> members.size() == 2) // it could be one if node2 joined but not discovered yet
                .next().as(StepVerifier::create)
                .expectNext(members1)
                .expectComplete()
                .verify(TIMEOUT);
        node2.members()
                .next().as(StepVerifier::create)
                .expectNext(members2)
                .expectComplete()
                .verify(TIMEOUT);

    }

    @NotNull
    private ConsulCluster consulCluster(ConsulClient client, String id) {
        return new ConsulCluster("test", client, codec(),
                sessionTimeout(), sessionRefreshPeriod(), leaderCheckInterval(),
                leaderCheckFallback(), memberRefreshInterval(), id, Schedulers.newSingle(id));
    }

    private ConsulClient consulClient() {
        return new ConsulClient(this.consulHost, this.consulPort);
    }

    ConsulCodec codec() {
        return new ConsulCodec(new ObjectMapper());
    }

    Duration sessionTimeout() {
        return Duration.ofSeconds(10);
    }

    Duration sessionRefreshPeriod() {
        return Duration.ofSeconds(3);
    }

    Duration leaderCheckInterval() {
        return Duration.ofSeconds(1);
    }

    Duration leaderCheckFallback() {
        return Duration.ofSeconds(1);
    }

    Duration memberRefreshInterval() {
        return Duration.ofSeconds(3);
    }

}