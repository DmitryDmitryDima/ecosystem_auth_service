package com.ecosystem.auth.config;


import org.springframework.amqp.core.FanoutExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {

    @Value("${users.main_events}")
    private String USERS_EXCHANGE_NAME;

    @Bean
    public FanoutExchange usersExchange(){
        return new FanoutExchange(USERS_EXCHANGE_NAME);
    }
}
