package com.github.harry_hao.cluster.core;

import com.github.harry_hao.cluster.ClusterFactory;

public interface ClusterFactorySpi extends ClusterFactory {
    String name();
}
