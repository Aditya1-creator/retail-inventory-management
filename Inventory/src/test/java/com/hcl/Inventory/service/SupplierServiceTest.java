package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.SupplierRequestDto;
import com.hcl.Inventory.dto.SupplierResponseDto;
import com.hcl.Inventory.entity.Supplier;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SupplierServiceTest {

    private SupplierRepository supplierRepository;
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierRepository = mock(SupplierRepository.class);
        supplierService = new SupplierService(supplierRepository);
    }

    @Test
    void testCreateSupplier() {
        SupplierRequestDto requestDto = new SupplierRequestDto("Tech Supplies", "tech@supplies.com", "1234567890");
        Supplier savedSupplier = new Supplier(1L, "Tech Supplies", "tech@supplies.com", "1234567890");

        when(supplierRepository.save(any(Supplier.class))).thenReturn(savedSupplier);

        SupplierResponseDto responseDto = supplierService.createSupplier(requestDto);

        assertNotNull(responseDto);
        assertEquals(1L, responseDto.getId());
        assertEquals("Tech Supplies", responseDto.getName());
    }

    @Test
    void testGetSupplierByIdSuccess() {
        Supplier supplier = new Supplier(1L, "Tech Supplies", "tech@supplies.com", "1234567890");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        SupplierResponseDto responseDto = supplierService.getSupplierById(1L);

        assertNotNull(responseDto);
        assertEquals(1L, responseDto.getId());
        assertEquals("Tech Supplies", responseDto.getName());
    }

    @Test
    void testGetSupplierByIdNotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierService.getSupplierById(99L));
    }

    @Test
    void testGetAllSuppliers() {
        Supplier s1 = new Supplier(1L, "Supplier A", "a@test.com", "111");
        Supplier s2 = new Supplier(2L, "Supplier B", "b@test.com", "222");

        when(supplierRepository.findAll()).thenReturn(List.of(s1, s2));

        List<SupplierResponseDto> list = supplierService.getAllSuppliers();

        assertEquals(2, list.size());
    }

    @Test
    void testDeleteSupplier() {
        Supplier supplier = new Supplier(1L, "Tech Supplies", "tech@supplies.com", "1234567890");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deleteSupplier(1L);

        verify(supplierRepository, times(1)).delete(supplier);
    }
}
