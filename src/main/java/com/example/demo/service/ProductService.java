package com.example.demo.service;

import com.example.demo.model.Product;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory product catalogue so the demo needs no database. */
@Service
public class ProductService {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Product> findAll() {
        return products.values().stream()
                .sorted(Comparator.comparing(Product::id))
                .toList();
    }

    public Product findById(Long id) {
        Product product = products.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public Product create(Product product) {
        Product saved = product.withId(sequence.incrementAndGet());
        products.put(saved.id(), saved);
        return saved;
    }

    public void delete(Long id) {
        if (products.remove(id) == null) {
            throw new ProductNotFoundException(id);
        }
    }
}
