package com.kov.techuserservice;

import org.springframework.boot.SpringApplication;

public class TestTechUserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TechUserServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
