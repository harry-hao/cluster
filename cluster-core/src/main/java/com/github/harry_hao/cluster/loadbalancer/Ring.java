package com.github.harry_hao.cluster.loadbalancer;

import com.github.harry_hao.cluster.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.SortedMap;
import java.util.TreeMap;

@Slf4j
public class Ring {

    private final int replicas;

    private final SortedMap<Integer, Node> nodes = new TreeMap<>();

    public Ring(int replicas) {
        this.replicas = replicas;
        log.debug("ring initialized | replicas={}", replicas);
    }

    public void add(Node node) {
        for (int i = 0; i < replicas; i++) {
            this.nodes.put(MurmurHash.hash32(node.getId()+i), node);
        }
        log.debug("ring added node | node={}", node);
    }

    public void remove(Node node) {
        for (int i = 0; i < replicas; i++) {
            this.nodes.remove(MurmurHash.hash32(node.getId()+i));
        }
        log.debug("ring removed node | node={}", node);
    }

    public void clear() {
        this.nodes.clear();
    }

    public Node get(String key) {
        if (this.nodes.isEmpty()) {
            log.debug("ring is empty");
            return null;
        }

        int hash = MurmurHash.hash32(key);
        if (!this.nodes.containsKey(hash)) {
            SortedMap<Integer, Node> tailMap = this.nodes.tailMap(hash);
            hash = tailMap.isEmpty() ? this.nodes.firstKey() : tailMap.firstKey();
        }
        return this.nodes.get(hash);
    }

    public SortedMap<Integer, Node> nodes() {
        return this.nodes;
    }

}