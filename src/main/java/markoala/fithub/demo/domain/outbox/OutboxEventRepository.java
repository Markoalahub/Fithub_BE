package markoala.fithub.demo.domain.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            select e
            from OutboxEvent e
            where e.status = :status
              and e.nextRetryAt <= :now
            order by e.createdAt asc
            """)
    List<OutboxEvent> findDueEvents(
            @Param("status") OutboxEventStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
