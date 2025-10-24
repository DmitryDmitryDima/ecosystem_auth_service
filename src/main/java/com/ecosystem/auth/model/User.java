package com.ecosystem.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


/*
user uuid - корневая сущность для всей экосистемы
 */
@Entity
@Table(name="users")

@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    // USER, ADMIN
    // Роль будет важна при формировании ответа от микросервисов - базируясь на ней, микросервисы будут знать, какой именно ответ давать
    @Column(nullable = false)
    private String role;


    // in future - возможность авторизации через телеграм, с возможностью подключения специальных ботов и mini app
    @Column(unique = true)
    private String telegramId;


}
