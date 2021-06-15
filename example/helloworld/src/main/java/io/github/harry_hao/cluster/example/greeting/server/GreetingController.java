package io.github.harry_hao.cluster.example.greeting.server;

import io.github.harry_hao.cluster.example.greeting.GreetingApi;
import io.github.harry_hao.cluster.example.greeting.service.GreetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GreetingController implements GreetingApi {

    @Autowired
    private GreetingService greetingService;

    @Override
    @GetMapping("/hello")
    public Mono<String> hello(@RequestParam String name) {
        return this.greetingService.hello(name);
    }

}
