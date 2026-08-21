package com.nynaromanoff.inventory_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Desativado na pipeline de CI/CD para evitar dependência de banco de dados real no boot")
class InventoryServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
