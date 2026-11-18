package br.com.nevvesdev.inventory.concurrency;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "inventory.stock.strategy=optimistic-lock")
class OptimisticLockStockConcurrencyTest extends AbstractStockConcurrencyTest {
}