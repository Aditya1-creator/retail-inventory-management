package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.SummaryResponseDto;
import com.hcl.Inventory.entity.OrderStatus;
import com.hcl.Inventory.repository.OrderRepository;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.SupplierRepository;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;

@Service
public class SummaryService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;
    private final StockService stockService;

    public SummaryService(ProductRepository productRepository,
                          SupplierRepository supplierRepository,
                          WarehouseRepository warehouseRepository,
                          OrderRepository orderRepository,
                          StockService stockService) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderRepository = orderRepository;
        this.stockService = stockService;
    }

    public SummaryResponseDto getSummary() {
        long totalProducts = productRepository.count();
        long totalSuppliers = supplierRepository.count();
        long totalWarehouses = warehouseRepository.count();
        long lowStockCount = stockService.getLowStockProducts().size();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        return new SummaryResponseDto(
                totalProducts,
                totalSuppliers,
                totalWarehouses,
                lowStockCount,
                pendingOrders,
                confirmedOrders,
                cancelledOrders
        );
    }
}
