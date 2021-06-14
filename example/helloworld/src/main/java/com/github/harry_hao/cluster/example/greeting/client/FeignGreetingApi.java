package com.github.harry_hao.cluster.example.greeting.client;

import com.github.harry_hao.cluster.example.greeting.GreetingApi;
import feign.Param;
import feign.RequestLine;
import reactor.core.publisher.Mono;

public interface FeignGreetingApi extends GreetingApi {
    @Override
    @RequestLine("GET /hello?name={name}")
    Mono<String> hello(@Param("name") String name);
}
