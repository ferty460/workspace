package com.example.workspace.finance.repository;

import com.example.workspace.finance.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Account> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.owner.id = :ownerId")
    Optional<Account> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);

}
