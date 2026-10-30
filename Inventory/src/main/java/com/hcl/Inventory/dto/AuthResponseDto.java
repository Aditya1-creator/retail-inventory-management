package com.hcl.Inventory.dto;

import com.hcl.Inventory.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private Role role;
    private String name;
}
