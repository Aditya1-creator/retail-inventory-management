package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.*;
import com.hcl.Inventory.entity.Order;
import com.hcl.Inventory.entity.OrderItem;
import com.hcl.Inventory.entity.OrderStatus;
import com.hcl.Inventory.entity.Product;
import com.hcl.Inventory.entity.Warehouse;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.OrderRepository;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockService stockService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        WarehouseRepository warehouseRepository,
                        StockService stockService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockService = stockService;
    }

    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponseDto(order);
    }

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto dto) {
        Order order = new Order();
        order.setCustomerName(dto.getCustomerName());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());

        double totalAmount = 0.0;
        List<OrderItem> items = new ArrayList<>();

        // Acquire product locks in a consistent order across concurrent requests.
        List<OrderItemRequestDto> sortedItems = dto.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequestDto::getProductId)).toList();
        for (OrderItemRequestDto itemDto : sortedItems) {
            Product product = productRepository.findByIdForUpdate(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));
            if (product.isArchived()) {
                throw new IllegalStateException("This product is archived. Restore it before creating a new order.");
            }

            Warehouse warehouse = warehouseRepository.findById(itemDto.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + itemDto.getWarehouseId()));

            // Deduct stock for this item (will throw InsufficientStockException if stock is insufficient)
            stockService.deductStock(new StockRequestDto(
                    itemDto.getProductId(),
                    itemDto.getWarehouseId(),
                    itemDto.getQuantity()
            ));

            double lineTotal = product.getPrice() * itemDto.getQuantity();
            totalAmount += lineTotal;

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setWarehouse(warehouse);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            items.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(items);

        Order savedOrder = orderRepository.save(order);
        return mapToResponseDto(savedOrder);
    }

    @Transactional
    public OrderResponseDto cancelOrder(Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        // Restore stock for each item in the order
        for (OrderItem item : order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId())).toList()) {
            stockService.addStock(new StockRequestDto(
                    item.getProduct().getId(),
                    item.getWarehouse().getId(),
                    item.getQuantity()
            ));
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);
        return mapToResponseDto(updatedOrder);
    }

    private OrderResponseDto mapToResponseDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getWarehouse().getId(),
                        item.getWarehouse().getName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .collect(Collectors.toList());

        return new OrderResponseDto(
                order.getId(),
                order.getCustomerName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemDtos
        );
    }
}
