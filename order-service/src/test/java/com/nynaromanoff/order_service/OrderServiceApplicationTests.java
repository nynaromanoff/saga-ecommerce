package com.nynaromanoff.order_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Desativado na pipeline de CI/CD para evitar dependência de banco de dados real no boot")
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
