package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.OrderItemRequestDto;
import com.hcl.Inventory.dto.OrderRequestDto;
import com.hcl.Inventory.dto.OrderResponseDto;
import com.hcl.Inventory.dto.StockRequestDto;
import com.hcl.Inventory.entity.*;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.OrderRepository;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private WarehouseRepository warehouseRepository;
    private StockService stockService;
    private OrderService orderService;

    private Product testProduct;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        stockService = mock(StockService.class);
        orderService = new OrderService(orderRepository, productRepository, warehouseRepository, stockService);

        testProduct = new Product(1L, "SKU-001", "Gaming Laptop", "Electronics", 1200.0, 5, null);
        testWarehouse = new Warehouse(1L, "Central Warehouse", "New York");
    }

    @Test
    void testCreateOrderSuccess() {
        OrderItemRequestDto itemDto = new OrderItemRequestDto(1L, 1L, 2);
        OrderRequestDto requestDto = new OrderRequestDto("John Doe", List.of(itemDto));

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));

        Order savedOrder = new Order(1L, "John Doe", OrderStatus.CONFIRMED, 2400.0, LocalDateTime.now(), new ArrayList<>());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDto response = orderService.createOrder(requestDto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getCustomerName());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        assertEquals(2400.0, response.getTotalAmount());
        verify(stockService, times(1)).deductStock(any(StockRequestDto.class));
    }

    @Test
    void testCancelOrderSuccess() {
        OrderItem item = new OrderItem(1L, null, testProduct, testWarehouse, 2, 1200.0);
        Order existingOrder = new Order(1L, "John Doe", OrderStatus.CONFIRMED, 2400.0, LocalDateTime.now(), List.of(item));

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto response = orderService.cancelOrder(1L);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(stockService, times(1)).addStock(any(StockRequestDto.class));
    }

    @Test
    void testGetOrderByIdNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
    }
}
