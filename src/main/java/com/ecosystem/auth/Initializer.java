package com.ecosystem.auth;

import com.ecosystem.auth.model.User;
import com.ecosystem.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Initializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Override
    public void run(String... args) throws Exception {

        Optional<User> check = userRepository.findByUsername("admin");
        if (check.isEmpty()){
            User admin = new User();
            admin.setPassword(encoder.encode("12345678"));
            admin.setUsername("admin");
            admin.setRole("ADMIN");

            userRepository.save(admin);
        }



    }
}
