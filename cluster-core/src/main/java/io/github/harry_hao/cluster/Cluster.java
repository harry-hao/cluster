package io.github.harry_hao.cluster;

import reactor.core.publisher.Flux;

import java.util.List;

public interface Cluster {
    /**
     * Join the cluster with metadata.
     *
     * @param metadata the metadata.
     */
    void join(String metadata);

    /**
     * Leave the cluster.
     */
    void leave();

    /**
     * List members of the cluster.
     *
     * @return A {@code Flux} of member list.
     */
    Flux<List<Node>> members();

    /**
     * Get leader of the cluster.
     *
     * @return A {@code Flux} of leader.
     */
    Flux<Node> leader();

    /**
     * Create a cluster.
     *
     * @param config the config, @see {@code ClusterConfig}.
     * @return A {@code Cluster}.
     */
    static Cluster create(ClusterConfig config) {
        return ClusterFactory.DEFAULT.create(config);
    }
}
