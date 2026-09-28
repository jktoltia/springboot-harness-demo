package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductNotFoundException;
import com.example.demo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ProductController.class, InfoController.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void listReturnsProducts() throws Exception {
        when(productService.findAll()).thenReturn(List.of(new Product(1L, "Pen", new BigDecimal("1.50"))));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pen"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(productService.findById(9L)).thenThrow(new ProductNotFoundException(9L));

        mockMvc.perform(get("/api/products/9")).andExpect(status().isNotFound());
    }

    @Test
    void createReturns201() throws Exception {
        when(productService.create(any())).thenReturn(new Product(1L, "Pen", new BigDecimal("1.50")));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pen\",\"price\":1.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/1")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new ProductNotFoundException(5L)).when(productService).delete(5L);

        mockMvc.perform(delete("/api/products/5")).andExpect(status().isNotFound());
    }

    @Test
    void infoReturnsVersion() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("springboot-harness-demo"));
    }
}
