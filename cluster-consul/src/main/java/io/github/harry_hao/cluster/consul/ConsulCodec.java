package io.github.harry_hao.cluster.consul;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class ConsulCodec {

    private ObjectMapper objectMapper;

    public ConsulCodec() {
        this.objectMapper = new ObjectMapper();
    }

    public ConsulCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public String encode(ConsulNode node) {
        return this.objectMapper.writeValueAsString(node);
    }

    @SneakyThrows
    public ConsulNode decode(String str) {
        return this.objectMapper.readValue(str, ConsulNode.class);
    }
}
