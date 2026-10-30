package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.WarehouseRequestDto;
import com.hcl.Inventory.dto.WarehouseResponseDto;
import com.hcl.Inventory.entity.Warehouse;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<WarehouseResponseDto> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public WarehouseResponseDto getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        return mapToResponseDto(warehouse);
    }

    @Transactional
    public WarehouseResponseDto createWarehouse(WarehouseRequestDto dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setLocation(dto.getLocation());

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return mapToResponseDto(savedWarehouse);
    }

    @Transactional
    public WarehouseResponseDto updateWarehouse(Long id, WarehouseRequestDto dto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        warehouse.setName(dto.getName());
        warehouse.setLocation(dto.getLocation());

        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        return mapToResponseDto(updatedWarehouse);
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        warehouseRepository.delete(warehouse);
    }

    private WarehouseResponseDto mapToResponseDto(Warehouse warehouse) {
        return new WarehouseResponseDto(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getLocation()
        );
    }
}
