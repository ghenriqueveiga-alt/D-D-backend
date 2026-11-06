package com.dd.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
		title = "D-D - API de Venda de Itens",
		version = "0.0.1",
		description = "Backend para cadastro e venda de itens"
))
public class DDBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DDBackendApplication.class, args);
	}

}
