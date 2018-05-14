package io.elastest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan("io.elastest")
public class ElSpringbootApp {
    public static void main(String[] args) throws Exception {
        new SpringApplication(ElSpringbootApp.class).run(args);
    }

}
