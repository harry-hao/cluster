package io.github.harry_hao.cluster;

import lombok.Value;

@Value
public class Node {

    private String id;

    private String metadata;

    private boolean isMySelf;
}
