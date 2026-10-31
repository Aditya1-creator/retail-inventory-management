package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.WarehouseRequestDto;
import com.hcl.Inventory.dto.WarehouseResponseDto;
import com.hcl.Inventory.entity.Warehouse;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.WarehouseRepository;
import com.hcl.Inventory.repository.StockRepository;
import com.hcl.Inventory.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

    public WarehouseService(WarehouseRepository warehouseRepository, StockRepository stockRepository,
                            OrderRepository orderRepository) {
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.orderRepository = orderRepository;
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
        Warehouse warehouse = warehouseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        if (orderRepository.countItemsForWarehouse(id) > 0) {
            throw new IllegalStateException("Cannot delete this warehouse because it is used in an order, including cancelled orders. Keep it to preserve order history. You can edit its name or location instead.");
        }
        var stock = stockRepository.findForUpdateByWarehouseId(id);
        if (stock.stream().anyMatch(item -> item.getQuantity() > 0)) {
            throw new IllegalStateException("Cannot delete this warehouse because it still contains stock. Review its quantities on the Inventory page. Deletion requires zero stock and no order history.");
        }
        stockRepository.deleteAll(stock);
        stockRepository.flush();
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
