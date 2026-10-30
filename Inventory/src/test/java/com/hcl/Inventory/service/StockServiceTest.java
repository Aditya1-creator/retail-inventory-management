package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.LowStockResponseDto;
import com.hcl.Inventory.dto.StockRequestDto;
import com.hcl.Inventory.dto.StockResponseDto;
import com.hcl.Inventory.entity.Product;
import com.hcl.Inventory.entity.Stock;
import com.hcl.Inventory.entity.Warehouse;
import com.hcl.Inventory.exception.InsufficientStockException;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.StockRepository;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockServiceTest {

    private StockRepository stockRepository;
    private ProductRepository productRepository;
    private WarehouseRepository warehouseRepository;
    private StockService stockService;

    private Product testProduct;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        productRepository = mock(ProductRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        stockService = new StockService(stockRepository, productRepository, warehouseRepository);

        testProduct = new Product(1L, "SKU-1", "Laptop", "Tech", 1000.0, 10, null);
        testWarehouse = new Warehouse(1L, "Main WH", "Delhi");
    }

    @Test
    void testAddStockNewRecord() {
        StockRequestDto dto = new StockRequestDto(1L, 1L, 20);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepository.findForUpdate(1L, 1L)).thenReturn(Optional.empty());

        Stock savedStock = new Stock(10L, testProduct, testWarehouse, 20);
        when(stockRepository.save(any(Stock.class))).thenReturn(savedStock);

        StockResponseDto response = stockService.addStock(dto);

        assertNotNull(response);
        assertEquals(20, response.getQuantity());
        assertEquals("Laptop", response.getProductName());
    }

    @Test
    void testDeductStockSuccess() {
        StockRequestDto dto = new StockRequestDto(1L, 1L, 5);
        Stock existingStock = new Stock(10L, testProduct, testWarehouse, 20);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepository.findForUpdate(1L, 1L)).thenReturn(Optional.of(existingStock));

        Stock updatedStock = new Stock(10L, testProduct, testWarehouse, 15);
        when(stockRepository.save(any(Stock.class))).thenReturn(updatedStock);

        StockResponseDto response = stockService.deductStock(dto);

        assertNotNull(response);
        assertEquals(15, response.getQuantity());
    }

    @Test
    void testDeductStockInsufficient() {
        StockRequestDto dto = new StockRequestDto(1L, 1L, 50);
        Stock existingStock = new Stock(10L, testProduct, testWarehouse, 20);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockRepository.findForUpdate(1L, 1L)).thenReturn(Optional.of(existingStock));

        assertThrows(InsufficientStockException.class, () -> stockService.deductStock(dto));
    }

    @Test
    void testGetLowStockProducts() {
        when(productRepository.findAll()).thenReturn(List.of(testProduct));
        List<Object[]> mockTotals = new java.util.ArrayList<>();
        mockTotals.add(new Object[]{1L, 5L});
        when(stockRepository.findTotalStockPerProduct()).thenReturn(mockTotals);

        List<LowStockResponseDto> lowStockList = stockService.getLowStockProducts();

        assertEquals(1, lowStockList.size());
        assertEquals("Laptop", lowStockList.get(0).getProductName());
        assertEquals(5, lowStockList.get(0).getTotalQuantity());
        assertEquals(10, lowStockList.get(0).getReorderLevel());
    }
}
