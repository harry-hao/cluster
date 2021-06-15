package io.github.harry_hao.cluster.example.greeting;

import io.github.harry_hao.cluster.Cluster;
import io.github.harry_hao.cluster.ClusterConfig;
import io.github.harry_hao.cluster.example.greeting.client.FeignGreetingApiFactory;
import io.github.harry_hao.cluster.example.greeting.service.GreetingService;
import io.github.harry_hao.cluster.loadbalancer.DefaultLoadBalancer;
import io.github.harry_hao.cluster.loadbalancer.LoadBalancer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GreetingConfiguration {
    @Bean
    public Cluster cluster() {
        ClusterConfig config = ClusterConfig.builder("greeting")
                .driverName("consul")
                .withDriverProperty("consul.host", "localhost")
                .withDriverProperty("consul.port", "8500")
                .build();
        return Cluster.create(config);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public LoadBalancer loadBalancer() {
        return new DefaultLoadBalancer(cluster(), 3);
    }

    @Bean
    public GreetingService greetingApi() {
        return new GreetingService(loadBalancer(), new FeignGreetingApiFactory());
    }
}
