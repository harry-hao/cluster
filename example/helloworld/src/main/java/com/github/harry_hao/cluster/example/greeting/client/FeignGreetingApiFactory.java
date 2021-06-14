package com.github.harry_hao.cluster.example.greeting.client;

import reactivefeign.webclient.WebReactiveFeign;

public class FeignGreetingApiFactory {
    public FeignGreetingApi create(String url) {
        return WebReactiveFeign.<FeignGreetingApi>builder()
                .target(FeignGreetingApi.class, url);
    }
}
