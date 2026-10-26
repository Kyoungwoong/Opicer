package com.opicer.api.evaluationguide.infrastructure;

import com.opicer.api.evaluation.domain.EvaluationGuideKind;
import com.opicer.api.evaluationguide.domain.EvaluationGuide;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationGuideRepository extends JpaRepository<EvaluationGuide, UUID> {

	List<EvaluationGuide> findAllByOrderByKindAscVersionDesc();

	List<EvaluationGuide> findByActiveTrue();

	@Query("select coalesce(max(g.version), 0) from EvaluationGuide g where g.kind = :kind")
	int maxVersion(@Param("kind") EvaluationGuideKind kind);

	/** Runs before activating another version, so the active_kind unique column never sees two rows. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update EvaluationGuide g set g.active = false, g.activeKind = null where g.kind = :kind and g.active = true")
	int deactivateKind(@Param("kind") EvaluationGuideKind kind);
}
