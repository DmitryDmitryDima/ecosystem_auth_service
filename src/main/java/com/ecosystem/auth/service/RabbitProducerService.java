package com.ecosystem.auth.service;

import com.ecosystem.auth.dto.events.UserCreationEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class RabbitProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${users.main_events}")
    private String USERS_EXCHANGE_NAME;



    public void sendMessage(String message){
        try {


            rabbitTemplate.send(USERS_EXCHANGE_NAME, "", new Message(message.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    // выброс исключения означает, что сообщение не было обработано как нужно - регистрация отменяется
    public void generateUserCreationEvent(UserCreationEvent event) throws Exception{
        MessagePostProcessor postProcessor = (message )-> {
            message.getMessageProperties().setHeader("event_type", "user_creation");
            return message;
        };

        rabbitTemplate.convertAndSend(USERS_EXCHANGE_NAME, "", event, postProcessor);


    }
}
