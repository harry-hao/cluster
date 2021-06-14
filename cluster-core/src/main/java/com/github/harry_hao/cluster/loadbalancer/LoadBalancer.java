package com.github.harry_hao.cluster.loadbalancer;

import com.github.harry_hao.cluster.Node;
import reactor.core.publisher.Mono;

public interface LoadBalancer {
    /**
     * Choose a node to serve.
     * Will emit LoadBalanceException if no target available.
     */
    Mono<Node> loadBalance(String key);

    /**
     * Start the load balancer.
     */
    void start();

    /**
     * Stop the load balancer.
     */
    void stop();

}
