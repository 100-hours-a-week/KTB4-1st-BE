package com.example.KTB_Agile_backend.item.price.persistence;

import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface ItemPriceEstimationOutboxEventRepository extends JpaRepository<ItemPriceEstimationOutboxEvent, Long> {

	@Transactional(readOnly = true)
	@Query("select event from ItemPriceEstimationOutboxEvent event where event.id > :afterId and not exists "
			+ "(select state.id from OutboxPollingRelayState state where state.eventId = event.eventId) "
			+ "order by event.id")
	List<ItemPriceEstimationOutboxEvent> findUnpublishedAfterId(@Param("afterId") Long afterId, Pageable pageable);

	Optional<ItemPriceEstimationOutboxEvent> findByEventId(String eventId);
}
