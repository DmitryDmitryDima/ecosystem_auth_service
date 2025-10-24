package com.ecosystem.auth.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {


    /*
    внутреннее значение не передается никому
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // хешируется так же, как и пароль. У юзера хранится значение без хеширования
    @Column(unique = true)
    private String token;


    @Column
    private Instant createdAt;

    // время, после которого будет считаться просроченным
    @Column
    private Instant expiredAt;


    // если true, токен считается отозванным
    // при просроченности токен становится revoked, после чего удаляется фоновым процессом
    @Column
    private boolean revoked;









    // может быть несколько refresh токенов, к примеру в ситуации, когда имеем несколько устройств
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;


}
