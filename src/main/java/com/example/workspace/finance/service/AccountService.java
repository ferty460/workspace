package com.example.workspace.finance.service;

import com.example.workspace.auth.model.User;
import com.example.workspace.core.exception.NotFoundException;
import com.example.workspace.finance.exception.AccountAlreadyExistsException;
import com.example.workspace.finance.model.Account;
import com.example.workspace.finance.model.dto.AccountDetailsDto;
import com.example.workspace.finance.model.dto.AccountRequestDto;
import com.example.workspace.finance.model.dto.AccountResponseDto;
import com.example.workspace.finance.repository.AccountRepository;
import com.example.workspace.finance.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final OperationRepository operationRepository;

    @Transactional
    public AccountDetailsDto create(AccountRequestDto dto, User owner) {
        String name = dto.name().trim();
        Long userId = owner.getId();

        if (accountRepository.existsByOwnerIdAndNameIgnoreCase(userId, name)) {
            throw new AccountAlreadyExistsException(name);
        }

        Account account = new Account();
        account.setName(name);
        account.setMoney(BigDecimal.ZERO);
        account.setOwner(owner);

        Account saved = accountRepository.save(account);
        log.info("Account created: id={}, ownerId={}", saved.getId(), userId);

        return toDetailsDto(saved);
    }

    @Transactional(readOnly = true)
    public AccountDetailsDto get(Long accountId, Long userId) {
        Account account = loadOwned(accountId, userId);
        return toDetailsDto(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getByOwnerId(Long userId) {
        return accountRepository.findAllByOwnerIdOrderByNameAsc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AccountResponseDto edit(Long accountId, AccountRequestDto dto, Long userId) {
        Account account = loadOwned(accountId, userId);
        String newName = dto.name().trim();

        if (account.getName().equalsIgnoreCase(newName)) {
            return toDto(account);
        }

        if (accountRepository.existsByOwnerIdAndNameIgnoreCase(userId, newName)) {
            throw new AccountAlreadyExistsException(newName);
        }

        account.setName(newName);
        account.setMoney(dto.balance());

        return toDto(account);
    }

    @Transactional
    public void delete(Long accountId, Long userId) {
        Account account = loadOwned(accountId, userId);

        operationRepository.deleteAll(account.getOperations());
        accountRepository.delete(account);
        log.info("Account deleted: id={}, ownerId={}", accountId, userId);
    }

    private Account loadOwned(Long accountId, Long userId) {
        return accountRepository.findByIdAndOwnerId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account with id " + accountId + " not found"));
    }

    private AccountResponseDto toDto(Account a) {
        return new AccountResponseDto(a.getId(), a.getName(), a.getMoney());
    }

    private AccountDetailsDto toDetailsDto(Account saved) {
        return new AccountDetailsDto(
                saved.getId(),
                saved.getName(),
                saved.getMoney(),
                saved.getOperations().size()
        );
    }

}
