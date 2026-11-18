package br.com.nevvesdev.inventory.concurrency;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "inventory.stock.strategy=pessimistic-lock")
class PessimisticLockStockConcurrencyTest extends AbstractStockConcurrencyTest {
}