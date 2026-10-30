package com.hcl.Inventory.repository;

import com.hcl.Inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    // A locking read sees the latest committed quantity under MySQL REPEATABLE READ.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id = :productId and s.warehouse.id = :warehouseId")
    Optional<Stock> findForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);
    List<Stock> findByProductId(Long productId);

    @Query("SELECT s.product.id, SUM(s.quantity) FROM Stock s GROUP BY s.product.id")
    List<Object[]> findTotalStockPerProduct();
}
