package com.example.workspace.finance.controller;

import com.example.workspace.auth.model.UserPrincipal;
import com.example.workspace.finance.model.dto.TransferRequestDto;
import com.example.workspace.finance.model.dto.TransferResponseDto;
import com.example.workspace.finance.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponseDto> transfer(
            @Valid @RequestBody TransferRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.transfer(dto, principal.user().getId()));
    }

    @GetMapping
    public ResponseEntity<List<TransferResponseDto>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(transferService.getAll(principal.user().getId()));
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponseDto> get(
            @PathVariable Long transferId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(transferService.get(transferId, principal.user().getId()));
    }

}
