package br.com.nevvesdev.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void deveCalcularTotalEConfirmarPedido() {
        UUID notebook = UUID.randomUUID();
        UUID mouse = UUID.randomUUID();

        Order order = Order.place(List.of(
                new OrderLine(notebook, 2, new BigDecimal("4500.00")),
                new OrderLine(mouse, 1, new BigDecimal("99.90"))
        ));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("9099.90");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getCreatedAt()).isNotNull();
    }

    @Test
    void deveGuardarPrecoUnitarioEQuantidadeDeCadaItem() {
        UUID productId = UUID.randomUUID();

        Order order = Order.place(List.of(new OrderLine(productId, 3, new BigDecimal("10.00"))));

        OrderItem item = order.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(item.getSubtotal()).isEqualByComparingTo("30.00");
    }

    @Test
    void deveRejeitarPedidoSemItens() {
        assertThatThrownBy(() -> Order.place(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarItemComQuantidadeZero() {
        assertThatThrownBy(() -> Order.place(List.of(new OrderLine(UUID.randomUUID(), 0, BigDecimal.TEN))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarItemComPrecoInvalido() {
        assertThatThrownBy(() -> Order.place(List.of(new OrderLine(UUID.randomUUID(), 1, BigDecimal.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}