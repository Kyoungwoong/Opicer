package com.opicer.api.evaluationguide.application;

import com.opicer.api.evaluation.domain.EvaluationGuideKind;
import com.opicer.api.evaluationguide.domain.EvaluationGuide;
import com.opicer.api.evaluationguide.infrastructure.EvaluationGuideRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationGuideCommandService {

	private final EvaluationGuideRepository repository;

	public EvaluationGuideCommandService(EvaluationGuideRepository repository) {
		this.repository = repository;
	}

	/** Adds the next version of {@code kind}, inactive until activated. */
	@Transactional
	public EvaluationGuide create(EvaluationGuideKind kind, String title, String contentMd) {
		checkLength(contentMd);
		int version = repository.maxVersion(kind) + 1;
		return repository.save(new EvaluationGuide(kind, version, title.trim(), contentMd));
	}

	@Transactional
	public EvaluationGuide update(UUID id, String title, String contentMd) {
		checkLength(contentMd);
		EvaluationGuide guide = find(id);
		if (guide.isActive()) {
			throw new ApiException(ErrorCode.EVALUATION_GUIDE_ACTIVE);
		}
		guide.edit(title.trim(), contentMd);
		return guide;
	}

	/** Makes this version the one evaluations use for its kind; the previously active version is switched off. */
	@Transactional
	public EvaluationGuide activate(UUID id) {
		EvaluationGuideKind kind = find(id).getKind();
		// Clears the persistence context, so the guide is loaded again below.
		repository.deactivateKind(kind);
		EvaluationGuide guide = find(id);
		guide.activate();
		return guide;
	}

	/** Evaluations stop using this kind until another version is activated. */
	@Transactional
	public EvaluationGuide deactivate(UUID id) {
		EvaluationGuide guide = find(id);
		guide.deactivate();
		return guide;
	}

	@Transactional
	public void delete(UUID id) {
		EvaluationGuide guide = find(id);
		if (guide.isActive()) {
			throw new ApiException(ErrorCode.EVALUATION_GUIDE_ACTIVE);
		}
		repository.delete(guide);
	}

	private EvaluationGuide find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.EVALUATION_GUIDE_NOT_FOUND));
	}

	/** The request DTO checks this too; the service guards every caller. */
	private static void checkLength(String contentMd) {
		if (contentMd == null || contentMd.isBlank() || contentMd.length() > EvaluationGuide.MAX_CONTENT_LENGTH) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
				"contentMd must be 1-" + EvaluationGuide.MAX_CONTENT_LENGTH + " characters");
		}
	}
}
