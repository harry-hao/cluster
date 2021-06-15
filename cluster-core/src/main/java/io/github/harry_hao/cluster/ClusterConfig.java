package io.github.harry_hao.cluster;

import lombok.Value;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Value
public class ClusterConfig {

    public static ClusterConfig DEFAULT = new Builder("default").build();

    /**
     * The cluster driver's name. Currently, only consul supported.
     */
    private String driverName;

    /**
     * The cluster name, must be unique.
     */
    private String clusterName;

    /**
     * How long does session get released if not renewed.
     */
    private Duration sessionTimeout;

    /**
     * How often we renew the session.
     */
    private Duration sessionRenewPeriod;

    /**
     * How often we check leadership.
     */
    private Duration leaderCheckInterval;

    /**
     * The fallback time when error happens in leader check.
     */
    private Duration leaderCheckFallback;

    /**
     * How often we update member list.
     */
    private Duration memberRefreshInterval;

    private Map<String, String> driverProperties;

    protected ClusterConfig(String driverName, String clusterName, Duration sessionTimeout, Duration sessionRenewPeriod,
                            Duration leaderCheckInterval, Duration leaderCheckFallback,
                            Duration memberRefreshInterval, Map<String, String> driverProperties) {
        this.driverName = driverName;
        this.clusterName = clusterName;
        this.sessionTimeout = sessionTimeout;
        this.sessionRenewPeriod = sessionRenewPeriod;
        this.leaderCheckInterval = leaderCheckInterval;
        this.leaderCheckFallback = leaderCheckFallback;
        this.memberRefreshInterval = memberRefreshInterval;
        this.driverProperties = driverProperties;
    }

    public String getDriverProperty(String key) {
        return this.driverProperties.get(key);
    }

    public static Builder builder(String clusterName) {
        return new Builder(clusterName);
    }

    public static class Builder {

        private String driverName = "consul";

        private String clusterName;

        private Duration sessionTimeout;

        private Duration sessionRenewPeriod;

        private Duration leaderCheckInterval;

        private Duration leaderCheckFallback;

        private Duration memberRefreshInterval;

        private Map<String, String> driverProperties;

        public Builder(String clusterName) {
            this.clusterName = clusterName;
            this.sessionTimeout = Duration.ofSeconds(60);
            this.sessionRenewPeriod = Duration.ofSeconds(20);
            this.leaderCheckInterval = Duration.ofSeconds(10);
            this.leaderCheckFallback = Duration.ofSeconds(3);
            this.memberRefreshInterval = Duration.ofSeconds(10);
            this.driverProperties = new HashMap<>();
        }

        public Builder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public Builder sessionTimeout(Duration sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder sessionRenewPeriod(Duration sessionRenewPeriod) {
            this.sessionRenewPeriod = sessionRenewPeriod;
            return this;
        }

        public Builder leaderCheckInterval(Duration leaderCheckInterval) {
            this.leaderCheckInterval = leaderCheckInterval;
            return this;
        }

        public Builder leaderCheckFallback(Duration leaderCheckFallback) {
            this.leaderCheckFallback = leaderCheckFallback;
            return this;
        }

        public Builder memberRefreshInterval(Duration memberRefreshInterval) {
            this.memberRefreshInterval = memberRefreshInterval;
            return this;
        }

        public Builder driverProperties(Map<String, String> driverProperties) {
            this.driverProperties.putAll(driverProperties);
            return this;
        }

        public Builder withDriverProperty(String key, String value) {
            this.driverProperties.put(key, value);
            return this;
        }

        public ClusterConfig build() {
            return new ClusterConfig(this.driverName, this.clusterName, this.sessionTimeout, this.sessionRenewPeriod,
                    this.leaderCheckInterval, this.leaderCheckFallback, this.memberRefreshInterval,
                    this.driverProperties);
        }
    }
}
