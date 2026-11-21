package com.example.workspace.finance.repository;

import com.example.workspace.finance.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("""
        SELECT t FROM Transfer t
        JOIN FETCH t.fromAccount fa
        JOIN FETCH t.toAccount ta
        WHERE t.id = :id
          AND fa.owner.id = :ownerId
          AND ta.owner.id = :ownerId
    """)
    Optional<Transfer> findByIdAndOwnerId(
            @Param("id") Long id,
            @Param("ownerId") Long ownerId
    );

    @Query("""
        SELECT t FROM Transfer t
        JOIN FETCH t.fromAccount fa
        JOIN FETCH t.toAccount ta
        WHERE fa.owner.id = :ownerId AND ta.owner.id = :ownerId
        ORDER BY t.createdAt DESC
    """)
    List<Transfer> findAllByOwnerId(@Param("ownerId") Long ownerId);

}
