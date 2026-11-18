package br.com.nevvesdev.inventory.concurrency;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "inventory.stock.strategy=conditional-update")
class ConditionalUpdateStockConcurrencyTest extends AbstractStockConcurrencyTest {
}