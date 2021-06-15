package io.github.harry_hao.cluster;

import io.github.harry_hao.cluster.core.DefaultClusterFactory;

public interface ClusterFactory {
    ClusterFactory DEFAULT = new DefaultClusterFactory();

    Cluster create(ClusterConfig config);
}
