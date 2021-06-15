package io.github.harry_hao.cluster.example.greeting.service;

import io.github.harry_hao.cluster.example.greeting.GreetingApi;
import io.github.harry_hao.cluster.example.greeting.client.FeignGreetingApi;
import io.github.harry_hao.cluster.example.greeting.client.FeignGreetingApiFactory;
import io.github.harry_hao.cluster.loadbalancer.LoadBalancer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class GreetingService implements GreetingApi {

    private LoadBalancer loadBalancer;
    private FeignGreetingApiFactory apiFactory;

    public GreetingService(LoadBalancer loadBalancer, FeignGreetingApiFactory apiFactory) {
        this.loadBalancer = loadBalancer;
        this.apiFactory = apiFactory;
    }

    @Override
    public Mono<String> hello(String name) {
        return loadBalancer.loadBalance(name)
                .flatMap(node -> {
                    if (node.isMySelf()) {
                        log.debug("handle hello request my self");
                        return doHello(name);
                    }
                    log.debug("redirect hello request to {}", node.getMetadata());
                    FeignGreetingApi api = this.apiFactory.create(node.getMetadata());
                    return api.hello(name);
                });
    }

    private Mono<String> doHello(String name) {
        return Mono.just(String.format("Hello, %s", name));
    }

}
