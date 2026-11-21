package com.example.workspace.finance.service;

import com.example.workspace.core.exception.NotFoundException;
import com.example.workspace.finance.exception.InsufficientFundsException;
import com.example.workspace.finance.exception.SameAccountTransferException;
import com.example.workspace.finance.model.Account;
import com.example.workspace.finance.model.Transfer;
import com.example.workspace.finance.model.dto.TransferRequestDto;
import com.example.workspace.finance.model.dto.TransferResponseDto;
import com.example.workspace.finance.repository.AccountRepository;
import com.example.workspace.finance.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public TransferResponseDto transfer(TransferRequestDto dto, Long userId) {
        Long fromId = dto.fromAccountId();
        Long toId = dto.toAccountId();

        if (fromId.equals(toId)) {
            throw new SameAccountTransferException(dto.fromAccountId());
        }

        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);

        Account firstAcc = loadOwnedForUpdate(firstId, userId);
        Account secondAcc = loadOwnedForUpdate(secondId, userId);

        Account from = firstId.equals(fromId) ? firstAcc : secondAcc;
        Account to = firstId.equals(toId) ? firstAcc : secondAcc;

        if (from.getMoney().compareTo(dto.amount()) < 0) {
            throw new InsufficientFundsException(from.getId(), from.getMoney(), dto.amount());
        }

        Transfer transfer = new Transfer();
        transfer.setFromAccount(from);
        transfer.setToAccount(to);
        transfer.setAmount(dto.amount());
        transfer.setDescription(dto.description());
        transfer.setCreatedAt(Instant.now());
        Transfer saved = transferRepository.save(transfer);

        from.setMoney(from.getMoney().subtract(dto.amount()));
        to.setMoney(to.getMoney().add(dto.amount()));

        log.info("Transfer completed: id={}, from={}, to={}, amount={}", saved.getId(), fromId, toId, dto.amount());

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TransferResponseDto> getAll(Long userId) {
        return transferRepository.findAllByOwnerId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferResponseDto get(Long transferId, Long userId) {
        Transfer transfer = transferRepository.findByIdAndOwnerId(transferId, userId)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));
        return toDto(transfer);
    }

    private TransferResponseDto toDto(Transfer transfer) {
        return new TransferResponseDto(
                transfer.getId(),
                transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(),
                transfer.getAmount(),
                transfer.getDescription(),
                transfer.getCreatedAt()
        );
    }

    private Account loadOwnedForUpdate(Long accountId, Long userId) {
        return accountRepository.findByIdAndOwnerIdForUpdate(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

}