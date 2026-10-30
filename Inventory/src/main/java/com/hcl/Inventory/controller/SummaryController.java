package com.hcl.Inventory.controller;

import com.hcl.Inventory.dto.SummaryResponseDto;
import com.hcl.Inventory.service.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public ResponseEntity<SummaryResponseDto> getSummary() {
        return ResponseEntity.ok(summaryService.getSummary());
    }
}
