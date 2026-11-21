package com.example.workspace.finance.repository;

import com.example.workspace.finance.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByOwnerId(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.owner.id = :ownerId")
    Optional<Account> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    @Query("SELECT COALESCE(SUM(a.money), 0) FROM Account a WHERE a.owner.id = :ownerId")
    BigDecimal sumMoneyByOwnerId(@Param("ownerId") Long ownerId);

}
