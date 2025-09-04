package com.esprit.formation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity
@EnableScheduling
@EnableAspectJAutoProxy
@EnableAsync
@EnableRetry
@EnableJpaAuditing

public class FormationApplication {

	public static void main(String[] args) {
		SpringApplication.run(FormationApplication.class, args);
	}

}
