package io.github.harry_hao.cluster.example.greeting;

import reactor.core.publisher.Mono;

public interface GreetingApi {
    Mono<String> hello(String name);
}
