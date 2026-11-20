package com.example.workspace.finance.repository;

import com.example.workspace.finance.model.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    @Query("""
        SELECT o FROM Operation o
        JOIN FETCH o.account a
        WHERE o.id = :id AND a.owner.id = :ownerId
    """)
    Optional<Operation> findByIdAndAccountOwnerId(
            @Param("id") Long id,
            @Param("ownerId") Long ownerId
    );

    @Query("""
        SELECT o FROM Operation o
        JOIN o.account a
        WHERE a.id = :accountId AND a.owner.id = :ownerId
        ORDER BY o.id DESC
    """)
    List<Operation> findAllByAccountAndOwner(
            @Param("accountId") Long accountId,
            @Param("ownerId") Long ownerId
    );

}
