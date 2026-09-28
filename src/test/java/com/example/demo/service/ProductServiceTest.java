package com.example.demo.service;

import com.example.demo.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceTest {

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService();
    }

    @Test
    void createAssignsIncrementingIds() {
        Product first = service.create(new Product(null, "Pen", new BigDecimal("1.50")));
        Product second = service.create(new Product(null, "Book", new BigDecimal("12.00")));

        assertThat(first.id()).isEqualTo(1L);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(service.findAll()).containsExactly(first, second);
    }

    @Test
    void findByIdReturnsProduct() {
        Product saved = service.create(new Product(null, "Pen", new BigDecimal("1.50")));

        assertThat(service.findById(saved.id())).isEqualTo(saved);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteRemovesProduct() {
        Product saved = service.create(new Product(null, "Pen", new BigDecimal("1.50")));

        service.delete(saved.id());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void deleteThrowsWhenMissing() {
        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(ProductNotFoundException.class);
    }
}
