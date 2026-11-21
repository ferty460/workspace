package com.example.workspace.finance.controller;

import com.example.workspace.auth.model.UserPrincipal;
import com.example.workspace.finance.model.dto.AccountDetailsDto;
import com.example.workspace.finance.model.dto.AccountRequestDto;
import com.example.workspace.finance.model.dto.AccountResponseDto;
import com.example.workspace.finance.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponseDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.getByOwnerId(principal.user().getId()));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDetailsDto> get(@PathVariable Long accountId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.get(accountId, principal.user().getId()));
    }

    @GetMapping("/total-balance")
    public ResponseEntity<BigDecimal> getTotalBalance(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getTotalBalance(principal.user().getId()));
    }

    @PostMapping
    public ResponseEntity<AccountResponseDto> create(
            @Valid @RequestBody AccountRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.create(dto, principal.user()));
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<AccountResponseDto> edit(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.edit(accountId, dto, principal.user().getId()));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(@PathVariable Long accountId, @AuthenticationPrincipal UserPrincipal principal) {
        accountService.delete(accountId, principal.user().getId());
        return ResponseEntity.noContent().build();
    }

}
