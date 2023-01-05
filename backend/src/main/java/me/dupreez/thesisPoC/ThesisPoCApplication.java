package me.dupreez.thesisPoC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@PropertySource({ "classpath:application.properties", "classpath:specific.properties"})
@SpringBootApplication
public class ThesisPoCApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThesisPoCApplication.class, args);
	}

}

