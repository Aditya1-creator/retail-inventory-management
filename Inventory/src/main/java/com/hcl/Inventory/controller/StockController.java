package com.hcl.Inventory.controller;

import com.hcl.Inventory.dto.LowStockResponseDto;
import com.hcl.Inventory.dto.StockRequestDto;
import com.hcl.Inventory.dto.StockResponseDto;
import com.hcl.Inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<StockResponseDto>> getAllStock() {
        return ResponseEntity.ok(stockService.getAllStock());
    }

    @PostMapping("/add")
    public ResponseEntity<StockResponseDto> addStock(@Valid @RequestBody StockRequestDto dto) {
        return ResponseEntity.ok(stockService.addStock(dto));
    }

    @PostMapping("/deduct")
    public ResponseEntity<StockResponseDto> deductStock(@Valid @RequestBody StockRequestDto dto) {
        return ResponseEntity.ok(stockService.deductStock(dto));
    }

    @GetMapping("/low")
    public ResponseEntity<List<LowStockResponseDto>> getLowStockProducts() {
        return ResponseEntity.ok(stockService.getLowStockProducts());
    }
}
