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
public class PromptVersionQueryService {

	private final PromptVersionRepository promptVersionRepository;

	public PromptVersionQueryService(PromptVersionRepository promptVersionRepository) {
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
}
