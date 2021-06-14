package com.github.harry_hao.cluster.example.greeting;

import com.github.harry_hao.cluster.Cluster;
import com.github.harry_hao.cluster.ClusterConfig;
import com.github.harry_hao.cluster.example.greeting.client.FeignGreetingApi;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import reactivefeign.webclient.WebReactiveFeign;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@SpringBootApplication
public class GreetingApplication implements ApplicationListener<ReactiveWebServerInitializedEvent> {
    public static void main(String[] args) {
        SpringApplication.run(GreetingApplication.class, args);
    }

    @Autowired
    private Cluster cluster;

    @SneakyThrows
    @Override
    public void onApplicationEvent(ReactiveWebServerInitializedEvent reactiveWebServerInitializedEvent) {

        String myAddress = InetAddress.getLocalHost().getHostAddress();
        int myPort = reactiveWebServerInitializedEvent.getWebServer().getPort();
        String url = String.format("http://%s:%d", myAddress, myPort);
        this.cluster.join(url);
    }
}
