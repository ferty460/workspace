package com.example.workspace.finance.controller;

import com.example.workspace.auth.model.UserPrincipal;
import com.example.workspace.finance.model.dto.OperationRequestDto;
import com.example.workspace.finance.model.dto.OperationResponseDto;
import com.example.workspace.finance.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @GetMapping
    public ResponseEntity<List<OperationResponseDto>> list(
            @RequestParam Long accountId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(operationService.getAllByAccountId(accountId, principal.user().getId()));
    }

    @GetMapping("/{operationId}")
    public ResponseEntity<OperationResponseDto> get(
            @PathVariable Long operationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                operationService.getById(operationId, principal.user().getId()));
    }

    @PostMapping
    public ResponseEntity<OperationResponseDto> create(
            @RequestParam Long accountId,
            @Valid @RequestBody OperationRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operationService.create(accountId, dto, principal.user().getId()));
    }

    @PatchMapping("/{operationId}")
    public ResponseEntity<OperationResponseDto> update(
            @PathVariable Long operationId,
            @Valid @RequestBody OperationRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(operationService.update(operationId, dto, principal.user().getId()));
    }

    @DeleteMapping("/{operationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long operationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        operationService.delete(operationId, principal.user().getId());
        return ResponseEntity.noContent().build();
    }

}
