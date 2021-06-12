package com.github.harry_hao.cluster;

import reactor.core.publisher.Flux;

import java.util.List;

public interface Cluster {
    void join(String myId, String metadata);
    void leave();
    Flux<List<Node>> members();
    Flux<Node> leader();
}
