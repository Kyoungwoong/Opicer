package com.opicer.api.evaluationguide.application;

import com.opicer.api.evaluation.application.EvaluationGuideNote;
import com.opicer.api.evaluationguide.domain.EvaluationGuide;
import com.opicer.api.evaluationguide.infrastructure.EvaluationGuideRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationGuideQueryService {

	private final EvaluationGuideRepository repository;

	public EvaluationGuideQueryService(EvaluationGuideRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<EvaluationGuide> findAll() {
		return repository.findAllByOrderByKindAscVersionDesc();
	}

	/** The active guide of each kind, in kind order, so the prompt text is stable for caching. */
	@Transactional(readOnly = true)
	public List<EvaluationGuideNote> activeNotes() {
		return repository.findByActiveTrue().stream()
			.sorted(Comparator.comparing(EvaluationGuide::getKind))
			.map(g -> new EvaluationGuideNote(g.getKind(), g.getVersion(), g.getContentMd()))
			.toList();
	}
}
