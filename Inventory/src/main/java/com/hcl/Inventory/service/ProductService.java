package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.ProductRequestDto;
import com.hcl.Inventory.dto.ProductResponseDto;
import com.hcl.Inventory.entity.Product;
import com.hcl.Inventory.entity.Supplier;
import com.hcl.Inventory.exception.ResourceNotFoundException;
import com.hcl.Inventory.repository.ProductRepository;
import com.hcl.Inventory.repository.SupplierRepository;
import com.hcl.Inventory.repository.StockRepository;
import com.hcl.Inventory.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

    public ProductService(ProductRepository productRepository, SupplierRepository supplierRepository,
                          StockRepository stockRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.stockRepository = stockRepository;
        this.orderRepository = orderRepository;
    }

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponseDto(product);
    }

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new IllegalStateException("This SKU is already in use. Enter a different SKU or restore the archived product.");
        }
        Supplier supplier = null;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + dto.getSupplierId()));
        }

        Product product = new Product();
        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setReorderLevel(dto.getReorderLevel());
        product.setSupplier(supplier);

        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto dto) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.findBySku(dto.getSku()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new IllegalStateException("This SKU is already in use. Enter a different SKU."); });

        Supplier supplier = null;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + dto.getSupplierId()));
        }

        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setReorderLevel(dto.getReorderLevel());
        product.setSupplier(supplier);

        Product updatedProduct = productRepository.save(product);
        return mapToResponseDto(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (orderRepository.countItemsForProduct(id) > 0) {
            throw new IllegalStateException("This product is used in an order, including cancelled orders. Use Archive to preserve its order history.");
        }
        var stock = stockRepository.findByProductId(id);
        if (stock.stream().anyMatch(item -> item.getQuantity() > 0)) {
            throw new IllegalStateException("This product still has stock. Deduct the remaining stock before deleting, or use Archive.");
        }
        stockRepository.deleteAll(stock);
        stockRepository.flush();
        productRepository.delete(product);
    }

    @Transactional
    public ProductResponseDto setArchived(Long id, boolean archived) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setArchived(archived);
        return mapToResponseDto(productRepository.save(product));
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setCategory(product.getCategory());
        dto.setPrice(product.getPrice());
        dto.setReorderLevel(product.getReorderLevel());
        dto.setArchived(product.isArchived());
        if (product.getSupplier() != null) {
            dto.setSupplierId(product.getSupplier().getId());
            dto.setSupplierName(product.getSupplier().getName());
        }
        return dto;
    }
}
