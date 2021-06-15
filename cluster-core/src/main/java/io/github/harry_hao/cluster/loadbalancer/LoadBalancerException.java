package io.github.harry_hao.cluster.loadbalancer;

import io.github.harry_hao.cluster.exception.ClusterException;

public class LoadBalancerException extends ClusterException {
    public LoadBalancerException() {
    }

    public LoadBalancerException(String message) {
        super(message);
    }

    public LoadBalancerException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadBalancerException(Throwable cause) {
        super(cause);
    }

    public LoadBalancerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
