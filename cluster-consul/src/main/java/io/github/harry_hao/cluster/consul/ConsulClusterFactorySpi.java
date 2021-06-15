package io.github.harry_hao.cluster.consul;

import com.ecwid.consul.v1.ConsulClient;
import io.github.harry_hao.cluster.Cluster;
import io.github.harry_hao.cluster.ClusterConfig;
import io.github.harry_hao.cluster.core.ClusterFactorySpi;

public class ConsulClusterFactorySpi implements ClusterFactorySpi {
    @Override
    public String name() {
        return "consul";
    }

    @Override
    public Cluster create(ClusterConfig config) {

        String host = config.getDriverProperty("consul.host");
        int port = Integer.parseInt(config.getDriverProperty("consul.port"));

        ConsulClient client = new ConsulClient(host, port);
        return new ConsulCluster(client, new ConsulCodec(), config);
    }
}
