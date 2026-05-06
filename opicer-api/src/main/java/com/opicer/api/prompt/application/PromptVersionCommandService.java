package com.opicer.api.prompt.application;

import com.opicer.api.prompt.domain.PromptUseCase;
import com.opicer.api.prompt.domain.PromptVersion;
import com.opicer.api.prompt.infrastructure.PromptVersionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptVersionCommandService {

	private final PromptVersionRepository promptVersionRepository;

	public PromptVersionCommandService(PromptVersionRepository promptVersionRepository) {
		this.promptVersionRepository = promptVersionRepository;
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
