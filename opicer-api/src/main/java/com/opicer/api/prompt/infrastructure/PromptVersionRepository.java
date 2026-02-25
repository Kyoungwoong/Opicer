package com.opicer.api.prompt.infrastructure;

import com.opicer.api.prompt.domain.PromptUseCase;
import com.opicer.api.prompt.domain.PromptVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, UUID> {

	List<PromptVersion> findByUseCase(PromptUseCase useCase);

	Optional<PromptVersion> findByUseCaseAndActiveTrue(PromptUseCase useCase);

	@Modifying
	@Query("UPDATE PromptVersion p SET p.active = false WHERE p.useCase = :useCase AND p.active = true")
	void deactivateAllByUseCase(PromptUseCase useCase);
}
