package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.SummaryResponseDto;
import com.hcl.Inventory.entity.OrderStatus;
import com.hcl.Inventory.repository.OrderRepository;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.SupplierRepository;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SummaryServiceTest {

    private ProductRepository productRepository;
    private SupplierRepository supplierRepository;
    private WarehouseRepository warehouseRepository;
    private OrderRepository orderRepository;
    private StockService stockService;
    private SummaryService summaryService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        orderRepository = mock(OrderRepository.class);
        stockService = mock(StockService.class);
        summaryService = new SummaryService(productRepository, supplierRepository, warehouseRepository, orderRepository, stockService);
    }

    @Test
    void testGetSummary() {
        when(productRepository.count()).thenReturn(10L);
        when(supplierRepository.count()).thenReturn(3L);
        when(warehouseRepository.count()).thenReturn(2L);
        when(stockService.getLowStockProducts()).thenReturn(Collections.emptyList());
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(1L);
        when(orderRepository.countByStatus(OrderStatus.CONFIRMED)).thenReturn(5L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(2L);

        SummaryResponseDto summary = summaryService.getSummary();

        assertNotNull(summary);
        assertEquals(10L, summary.getTotalProducts());
        assertEquals(3L, summary.getTotalSuppliers());
        assertEquals(2L, summary.getTotalWarehouses());
        assertEquals(0L, summary.getLowStockCount());
        assertEquals(1L, summary.getPendingOrders());
        assertEquals(5L, summary.getConfirmedOrders());
        assertEquals(2L, summary.getCancelledOrders());
    }
}
