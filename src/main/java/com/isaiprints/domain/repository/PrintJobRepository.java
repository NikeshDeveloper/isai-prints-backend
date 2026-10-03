package com.isaiprints.domain.repository;

import com.isaiprints.domain.entity.PrintJob;
import com.isaiprints.domain.enums.PrintJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrintJobRepository extends JpaRepository<PrintJob, UUID> {
    List<PrintJob> findByStatus(PrintJobStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM PrintJob j WHERE j.id = :id AND j.status = :status")
    Optional<PrintJob> findByIdAndStatusForUpdate(UUID id, PrintJobStatus status);
    
    long countByStatus(PrintJobStatus status);
}
