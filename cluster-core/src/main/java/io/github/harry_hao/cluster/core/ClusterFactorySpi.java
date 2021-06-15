package io.github.harry_hao.cluster.core;

import io.github.harry_hao.cluster.ClusterFactory;

public interface ClusterFactorySpi extends ClusterFactory {
    String name();
}
