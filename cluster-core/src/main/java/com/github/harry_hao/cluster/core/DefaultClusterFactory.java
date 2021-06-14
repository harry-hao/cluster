package com.github.harry_hao.cluster.core;

import com.github.harry_hao.cluster.Cluster;
import com.github.harry_hao.cluster.ClusterConfig;
import com.github.harry_hao.cluster.ClusterFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.ServiceLoader;

@Slf4j
public class DefaultClusterFactory implements ClusterFactory {

    ServiceLoader<ClusterFactorySpi> loader = ServiceLoader.load(ClusterFactorySpi.class);

    private ClusterFactorySpi findClusterFactoryByName(String driverName) {
        this.loader.reload();
        Iterator<ClusterFactorySpi> it = this.loader.iterator();
        while (it.hasNext()) {
            ClusterFactorySpi factory = it.next();
            if (factory.name().equals(driverName)) {
                log.info("found cluster factory {}", driverName);
                return factory;
            }
        }
        log.warn("cluster factory {} not found", driverName);
        return null;
    }


    @Override
    public Cluster create(ClusterConfig config) {
        ClusterFactorySpi spi = findClusterFactoryByName(config.getDriverName());
        return spi.create(config);
    }
}
