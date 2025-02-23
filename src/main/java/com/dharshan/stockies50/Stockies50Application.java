package com.dharshan.stockies50;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class Stockies50Application {
	public static void main(String[] args) {
		SpringApplication.run(Stockies50Application.class, args);
	}
}