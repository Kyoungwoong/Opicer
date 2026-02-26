package com.opicer.api.prompt.application;

import com.opicer.api.prompt.domain.PromptUseCase;
import com.opicer.api.prompt.domain.PromptVersion;
import com.opicer.api.prompt.infrastructure.PromptVersionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptVersionService {

	private final PromptVersionRepository promptVersionRepository;

	public PromptVersionService(PromptVersionRepository promptVersionRepository) {
		this.promptVersionRepository = promptVersionRepository;
	}

	@Transactional(readOnly = true)
	public List<PromptVersion> findAll() {
		return promptVersionRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<PromptVersion> findById(UUID id) {
		return promptVersionRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public Optional<PromptVersion> findActiveByUseCase(PromptUseCase useCase) {
		return promptVersionRepository.findByUseCaseAndActiveTrue(useCase);
	}

	@Transactional
	public PromptVersion create(PromptUseCase useCase, int version, String name, String template) {
		PromptVersion promptVersion = new PromptVersion(useCase, version, name, template);
		return promptVersionRepository.save(promptVersion);
	}

	@Transactional
	public Optional<PromptVersion> update(UUID id, PromptUseCase useCase, int version,
		String name, String template) {
		return promptVersionRepository.findById(id).map(promptVersion -> {
			promptVersion.update(useCase, version, name, template);
			return promptVersionRepository.save(promptVersion);
		});
	}

	/**
	 * Ensures only one active PromptVersion per useCase.
	 * Deactivates all others in the same useCase before activating this one.
	 */
	@Transactional
	public Optional<PromptVersion> activate(UUID id) {
		return promptVersionRepository.findById(id).map(promptVersion -> {
			promptVersionRepository.deactivateAllByUseCase(promptVersion.getUseCase());
			promptVersion.activate();
			return promptVersionRepository.save(promptVersion);
		});
	}

	@Transactional
	public boolean delete(UUID id) {
		if (!promptVersionRepository.existsById(id)) {
			return false;
		}
		promptVersionRepository.deleteById(id);
		return true;
	}
}
