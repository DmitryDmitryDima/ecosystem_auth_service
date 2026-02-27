package com.ecosystem.auth;

import com.ecosystem.auth.model.User;
import com.ecosystem.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@SpringBootTest
class AuthApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {

		Pageable pageable = PageRequest.of(0, 5);
		Page<User> page = userRepository.findByUsernameStartsWith(pageable, "admin");

		page.forEach(entity->System.out.println(entity.getUsername()));
	}

}
