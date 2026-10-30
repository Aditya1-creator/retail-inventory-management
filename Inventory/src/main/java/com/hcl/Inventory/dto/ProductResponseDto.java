package com.hcl.Inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String sku;
    private String name;
    private String category;
    private Double price;
    private Integer reorderLevel;
    private Long supplierId;
    private String supplierName;
}
