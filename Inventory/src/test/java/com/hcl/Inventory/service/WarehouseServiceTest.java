package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.WarehouseRequestDto;
import com.hcl.Inventory.dto.WarehouseResponseDto;
import com.hcl.Inventory.entity.Warehouse;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarehouseServiceTest {

    private WarehouseRepository warehouseRepository;
    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        warehouseService = new WarehouseService(warehouseRepository,
                mock(com.hcl.Inventory.repository.StockRepository.class),
                mock(com.hcl.Inventory.repository.OrderRepository.class));
    }

    @Test
    void testCreateWarehouse() {
        WarehouseRequestDto requestDto = new WarehouseRequestDto("Main Hub", "New York");
        Warehouse savedWarehouse = new Warehouse(1L, "Main Hub", "New York");

        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(savedWarehouse);

        WarehouseResponseDto responseDto = warehouseService.createWarehouse(requestDto);

        assertNotNull(responseDto);
        assertEquals(1L, responseDto.getId());
        assertEquals("Main Hub", responseDto.getName());
    }

    @Test
    void testGetWarehouseByIdSuccess() {
        Warehouse warehouse = new Warehouse(1L, "Main Hub", "New York");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        WarehouseResponseDto responseDto = warehouseService.getWarehouseById(1L);

        assertNotNull(responseDto);
        assertEquals(1L, responseDto.getId());
        assertEquals("Main Hub", responseDto.getName());
    }

    @Test
    void testGetWarehouseByIdNotFound() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> warehouseService.getWarehouseById(99L));
    }

    @Test
    void testGetAllWarehouses() {
        Warehouse w1 = new Warehouse(1L, "North Hub", "Chicago");
        Warehouse w2 = new Warehouse(2L, "South Hub", "Dallas");

        when(warehouseRepository.findAll()).thenReturn(List.of(w1, w2));

        List<WarehouseResponseDto> list = warehouseService.getAllWarehouses();

        assertEquals(2, list.size());
    }

    @Test
    void testDeleteWarehouse() {
        Warehouse warehouse = new Warehouse(1L, "Main Hub", "New York");
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(warehouse));

        warehouseService.deleteWarehouse(1L);

        verify(warehouseRepository, times(1)).delete(warehouse);
    }
}
