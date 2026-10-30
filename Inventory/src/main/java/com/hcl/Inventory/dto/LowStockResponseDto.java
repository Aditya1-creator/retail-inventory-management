package com.hcl.Inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockResponseDto {
    private Long productId;
    private String productSku;
    private String productName;
    private Integer totalQuantity;
    private Integer reorderLevel;
}
