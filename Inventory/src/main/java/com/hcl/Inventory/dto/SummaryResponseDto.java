package com.hcl.Inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponseDto {
    private Long totalProducts;
    private Long totalSuppliers;
    private Long totalWarehouses;
    private Long lowStockCount;
    private Long pendingOrders;
    private Long confirmedOrders;
    private Long cancelledOrders;
}
