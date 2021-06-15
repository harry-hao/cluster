package io.github.harry_hao.cluster.consul;


import com.ecwid.consul.v1.ConsulClient;
import io.github.harry_hao.cluster.ClusterConfig;
import io.github.harry_hao.cluster.Node;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Testcontainers
class ConsulClusterTest {
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
    void testLeadershipFailOver() {

        ConsulCluster node1 = consulCluster();

        ConsulCluster node2 = consulCluster();

        node1.join("<1>");

        node1.leader()
                .next()
                .map(Node::getId)
                .as(StepVerifier::create)
                .expectNext(node1.myNodeId())
                .expectComplete()
                .verify(TIMEOUT);

        node2.join("<2>");

        node2.leader()
                .next()
                .map(Node::getId)
                .as(StepVerifier::create)
                .expectNext(node1.myNodeId())
                .expectComplete()
                .verify(TIMEOUT);

        node1.leave();

        node2.leader()
                .map(Node::getId)
                .filter(id -> id.equals(node2.myNodeId()))
                .as(StepVerifier::create)
                .expectNext(node2.myNodeId())
                .thenCancel()
                .verify(TIMEOUT);

        node2.leave();
    }

    @Test
    void testMembers() {
        ConsulCluster node1 = consulCluster();

        ConsulCluster node2 = consulCluster();

        node1.join( "♘");

        node2.join("♞");

        List<Node> members1 = new ArrayList<>();
        members1.add(new Node(node1.myNodeId(), "♘",  true));
        members1.add(new Node(node2.myNodeId(), "♞",  false));
        members1.sort(Comparator.comparing(Node::getId));

        List<Node> members2 = new ArrayList<>();
        members2.add(new Node(node1.myNodeId(), "♘",  false));
        members2.add(new Node(node2.myNodeId(), "♞",  true));
        members2.sort(Comparator.comparing(Node::getId));

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
    private ConsulCluster consulCluster() {
        ConsulClient client = new ConsulClient(this.consulHost, this.consulPort);

        ClusterConfig config = ClusterConfig.builder("test")
                .withDriverProperty("consul.host", this.consulHost)
                .withDriverProperty("consul.port", String.format("%d", this.consulPort))
                .build();

        return new ConsulCluster(client, new ConsulCodec(), config);
    }

}