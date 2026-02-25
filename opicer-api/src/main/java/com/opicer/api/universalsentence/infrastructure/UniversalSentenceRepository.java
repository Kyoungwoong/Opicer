package com.opicer.api.universalsentence.infrastructure;

import com.opicer.api.universalsentence.domain.UniversalSentence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversalSentenceRepository extends JpaRepository<UniversalSentence, UUID> {
	List<UniversalSentence> findByActiveTrue();
	List<UniversalSentence> findByActiveTrueOrderByCreatedAtAscIdAsc();
}
