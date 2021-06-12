package com.github.harry_hao.cluster.consul;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ConsulNode {

    private String id;

    private String metadata;

    @JsonCreator
    public ConsulNode(@JsonProperty("id") String id,
                      @JsonProperty("metadata") String metadata) {
        this.id = id;
        this.metadata = metadata;
    }
}
