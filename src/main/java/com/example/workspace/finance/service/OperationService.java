package com.example.workspace.finance.service;

import com.example.workspace.core.exception.NotFoundException;
import com.example.workspace.finance.model.Account;
import com.example.workspace.finance.model.Operation;
import com.example.workspace.finance.model.dto.OperationRequestDto;
import com.example.workspace.finance.model.dto.OperationResponseDto;
import com.example.workspace.finance.repository.AccountRepository;
import com.example.workspace.finance.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<OperationResponseDto> getAllByAccountId(Long accountId, Long userId) {
        loadOwnedAccount(accountId, userId);

        return operationRepository.findAllByAccountAndOwner(accountId, userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperationResponseDto getById(Long operationId, Long userId) {
        Operation op = operationRepository.findByIdAndAccountOwnerId(operationId, userId)
                .orElseThrow(() -> new NotFoundException("Operation not found"));
        return toDto(op);
    }

    @Transactional
    public OperationResponseDto create(Long accountId, OperationRequestDto dto, Long userId) {
        Account account = loadOwnedAccountForUpdate(accountId, userId);

        Operation op = new Operation();
        op.setAccount(account);
        op.setType(dto.type());
        op.setAmount(dto.amount());
        op.setDescription(dto.description());

        operationRepository.save(op);
        applyDelta(account, op.signedAmount());

        return toDto(op);
    }

    @Transactional
    public OperationResponseDto update(Long operationId, OperationRequestDto dto, Long userId) {
        Operation op = operationRepository.findByIdAndAccountOwnerId(operationId, userId)
                .orElseThrow(() -> new NotFoundException("Operation not found"));

        BigDecimal oldSigned = op.signedAmount();

        Account oldAccount = op.getAccount();
        Account targetAccount = oldAccount;
        if (dto.accountId() != null && !dto.accountId().equals(oldAccount.getId())) {
            targetAccount = loadOwnedAccountForUpdate(dto.accountId(), userId);
            op.setAccount(targetAccount);
        }

        op.setType(dto.type());
        op.setAmount(dto.amount());
        op.setDescription(dto.description());

        BigDecimal newSigned = op.signedAmount();

        if (oldAccount.getId().equals(targetAccount.getId())) {
            applyDelta(targetAccount, newSigned.subtract(oldSigned));
        } else {
            applyDelta(oldAccount, oldSigned.negate());
            applyDelta(targetAccount, newSigned);
        }

        return toDto(op);
    }

    @Transactional
    public void delete(Long operationId, Long userId) {
        Operation op = operationRepository.findByIdAndAccountOwnerId(operationId, userId)
                .orElseThrow(() -> new NotFoundException("Operation not found"));

        applyDelta(op.getAccount(), op.signedAmount().negate());
        operationRepository.delete(op);
    }

    private OperationResponseDto toDto(Operation op) {
        return new OperationResponseDto(
                op.getId(),
                op.getAccount().getId(),
                op.getType().name(),
                op.getAmount(),
                op.getDescription()
        );
    }

    private void applyDelta(Account account, BigDecimal delta) {
        if (delta.signum() == 0) return;
        account.setMoney(account.getMoney().add(delta));
    }

    private Account loadOwnedAccountForUpdate(Long accountId, Long userId) {
        return accountRepository.findByIdAndOwnerIdForUpdate(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private Account loadOwnedAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndOwnerId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

}
