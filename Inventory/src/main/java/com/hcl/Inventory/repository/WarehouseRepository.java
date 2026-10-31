package com.hcl.Inventory.repository;

import com.hcl.Inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select w from Warehouse w where w.id = :id")
    java.util.Optional<Warehouse> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
