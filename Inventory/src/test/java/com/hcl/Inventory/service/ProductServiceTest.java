package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.ProductRequestDto;
import com.hcl.Inventory.dto.ProductResponseDto;
import com.hcl.Inventory.entity.Product;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository productRepository;
    private SupplierRepository supplierRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        productService = new ProductService(productRepository, supplierRepository);
    }

    @Test
    void testCreateProduct() {
        ProductRequestDto requestDto = new ProductRequestDto("SKU-100", "Laptop", "Electronics", 999.99, 5, null);
        Product savedProduct = new Product(1L, "SKU-100", "Laptop", "Electronics", 999.99, 5, null);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponseDto responseDto = productService.createProduct(requestDto);

        assertNotNull(responseDto);
        assertEquals(1L, responseDto.getId());
        assertEquals("SKU-100", responseDto.getSku());
        assertEquals("Laptop", responseDto.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testGetProductByIdSuccess() {
        Product product = new Product(1L, "SKU-100", "Laptop", "Electronics", 999.99, 5, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponseDto responseDto = productService.getProductById(1L);

        assertNotNull(responseDto);
        assertEquals(1L, responseDto.getId());
        assertEquals("Laptop", responseDto.getName());
    }

    @Test
    void testGetProductByIdNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void testGetAllProducts() {
        Product product1 = new Product(1L, "SKU-100", "Laptop", "Electronics", 999.99, 5, null);
        Product product2 = new Product(2L, "SKU-101", "Mouse", "Electronics", 19.99, 10, null);

        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        List<ProductResponseDto> list = productService.getAllProducts();

        assertEquals(2, list.size());
    }

    @Test
    void testDeleteProduct() {
        Product product = new Product(1L, "SKU-100", "Laptop", "Electronics", 999.99, 5, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).delete(product);
    }
}
