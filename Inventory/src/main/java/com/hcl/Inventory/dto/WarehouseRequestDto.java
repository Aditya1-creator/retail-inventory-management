package com.hcl.Inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRequestDto {

    @NotBlank(message = "Warehouse name is required")
    private String name;

    private String location;
}
