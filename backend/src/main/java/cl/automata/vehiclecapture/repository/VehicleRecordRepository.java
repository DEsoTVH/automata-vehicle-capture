package cl.automata.vehiclecapture.repository;

import cl.automata.vehiclecapture.domain.RecordStatus;
import cl.automata.vehiclecapture.domain.VehicleRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface VehicleRecordRepository extends JpaRepository<VehicleRecord, UUID>, JpaSpecificationExecutor<VehicleRecord> {

    List<VehicleRecord> findByStatusOrderByCapturedAtAsc(RecordStatus status);

    List<VehicleRecord> findByVinAndIdNot(String vin, UUID id);

    long countByCapturedAtGreaterThanEqual(Instant since);

    @Query("select v.status, count(v) from VehicleRecord v group by v.status")
    List<Object[]> countGroupedByStatus();
}
