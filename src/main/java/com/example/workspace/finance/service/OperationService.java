package com.example.workspace.finance.service;

import com.example.workspace.core.exception.NotFoundException;
import com.example.workspace.finance.model.Account;
import com.example.workspace.finance.model.Operation;
import com.example.workspace.finance.model.dto.OperationRequestDto;
import com.example.workspace.finance.model.dto.OperationResponseDto;
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
public class OperationService {

    private final OperationRepository operationRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<OperationResponseDto> getAllByAccountId(Long accountId, Long userId) {
        if (!accountRepository.existsByIdAndOwnerId(accountId, userId)) {
            throw new NotFoundException("Account not found");
        }

        return operationRepository.findAllByAccountAndOwner(accountId, userId).stream()
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

        Operation saved = operationRepository.save(op);
        applyDelta(account, saved.signedAmount());

        log.info("Operation created: id={}, accountId={}, type={}, amount={}",
                saved.getId(), accountId, saved.getType(), saved.getAmount());
        return toDto(saved);
    }

    @Transactional
    public OperationResponseDto update(Long operationId, OperationRequestDto dto, Long userId) {
        Operation op = operationRepository.findByIdAndAccountOwnerId(operationId, userId)
                .orElseThrow(() -> new NotFoundException("Operation not found"));

        BigDecimal oldSigned = op.signedAmount();
        Long oldAccountId = op.getAccount().getId();
        boolean moving = dto.accountId() != null && !dto.accountId().equals(oldAccountId);

        if (moving) {
            Long first = Math.min(oldAccountId, dto.accountId());
            Long second = Math.max(oldAccountId, dto.accountId());
            Account firstAcc = loadOwnedAccountForUpdate(first, userId);
            Account secondAcc = loadOwnedAccountForUpdate(second, userId);

            Account oldAccount = first.equals(oldAccountId) ? firstAcc : secondAcc;
            Account targetAccount = first.equals(dto.accountId()) ? firstAcc : secondAcc;

            op.setAccount(targetAccount);
            op.setType(dto.type());
            op.setAmount(dto.amount());
            op.setDescription(dto.description());

            BigDecimal newSigned = op.signedAmount();
            applyDelta(oldAccount, oldSigned.negate());
            applyDelta(targetAccount, newSigned);

            log.info("Operation moved: id={}, {} -> {}", operationId, oldAccountId, targetAccount.getId());
            return toDto(op);
        }

        Account account = loadOwnedAccountForUpdate(oldAccountId, userId);
        op.setType(dto.type());
        op.setAmount(dto.amount());
        op.setDescription(dto.description());

        BigDecimal newSigned = op.signedAmount();
        applyDelta(account, newSigned.subtract(oldSigned));

        log.info("Operation updated: id={}, accountId={}", operationId, account.getId());
        return toDto(op);
    }

    @Transactional
    public void delete(Long operationId, Long userId) {
        Operation op = operationRepository.findByIdAndAccountOwnerId(operationId, userId)
                .orElseThrow(() -> new NotFoundException("Operation not found"));

        Account account = loadOwnedAccountForUpdate(op.getAccount().getId(), userId);
        applyDelta(account, op.signedAmount().negate());
        operationRepository.delete(op);

        log.info("Operation deleted: id={}, accountId={}", operationId, account.getId());
    }

    private OperationResponseDto toDto(Operation op) {
        return new OperationResponseDto(
                op.getId(),
                op.getAccount().getId(),
                op.getType(),
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

}
