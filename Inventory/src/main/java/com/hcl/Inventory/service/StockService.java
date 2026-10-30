package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.LowStockResponseDto;
import com.hcl.Inventory.dto.StockRequestDto;
import com.hcl.Inventory.dto.StockResponseDto;
import com.hcl.Inventory.entity.Product;
import com.hcl.Inventory.entity.Stock;
import com.hcl.Inventory.entity.Warehouse;
import com.hcl.Inventory.exception.InsufficientStockException;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.StockRepository;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public StockService(StockRepository stockRepository,
                        ProductRepository productRepository,
                        WarehouseRepository warehouseRepository) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public List<StockResponseDto> getAllStock() {
        return stockRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public StockResponseDto addStock(StockRequestDto dto) {
        Product product = productRepository.findByIdForUpdate(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + dto.getProductId()));

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + dto.getWarehouseId()));

        Stock stock = stockRepository.findForUpdate(dto.getProductId(), dto.getWarehouseId())
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setProduct(product);
                    newStock.setWarehouse(warehouse);
                    newStock.setQuantity(0);
                    return newStock;
                });

        if (dto.getQuantity() > Integer.MAX_VALUE - stock.getQuantity()) {
            throw new IllegalArgumentException("Stock quantity exceeds the supported maximum");
        }
        stock.setQuantity(stock.getQuantity() + dto.getQuantity());
        Stock savedStock = stockRepository.save(stock);

        return mapToResponseDto(savedStock);
    }

    @Transactional
    public StockResponseDto deductStock(StockRequestDto dto) {
        Product product = productRepository.findByIdForUpdate(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + dto.getProductId()));

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + dto.getWarehouseId()));

        Stock stock = stockRepository.findForUpdate(dto.getProductId(), dto.getWarehouseId())
                .orElseThrow(() -> new InsufficientStockException("No stock record exists for product " + product.getName() + " in warehouse " + warehouse.getName()));

        if (stock.getQuantity() < dto.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock available. Current quantity: " + stock.getQuantity() + ", requested deduction: " + dto.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - dto.getQuantity());
        Stock updatedStock = stockRepository.save(stock);

        return mapToResponseDto(updatedStock);
    }

    public List<LowStockResponseDto> getLowStockProducts() {
        List<Product> products = productRepository.findAll();
        List<Object[]> totals = stockRepository.findTotalStockPerProduct();

        Map<Long, Long> productStockMap = new HashMap<>();
        for (Object[] row : totals) {
            Long productId = (Long) row[0];
            Long totalQty = (Long) row[1];
            productStockMap.put(productId, totalQty != null ? totalQty : 0L);
        }

        List<LowStockResponseDto> lowStockList = new ArrayList<>();
        for (Product product : products) {
            int currentTotal = productStockMap.getOrDefault(product.getId(), 0L).intValue();
            if (currentTotal <= product.getReorderLevel()) {
                lowStockList.add(new LowStockResponseDto(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        currentTotal,
                        product.getReorderLevel()
                ));
            }
        }

        return lowStockList;
    }

    private StockResponseDto mapToResponseDto(Stock stock) {
        return new StockResponseDto(
                stock.getId(),
                stock.getProduct().getId(),
                stock.getProduct().getName(),
                stock.getProduct().getSku(),
                stock.getWarehouse().getId(),
                stock.getWarehouse().getName(),
                stock.getQuantity()
        );
    }
}
