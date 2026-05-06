package com.opicer.api.universalsentence.application;

import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import com.opicer.api.universalsentence.infrastructure.UniversalSentenceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniversalSentenceCommandService {

	private final UniversalSentenceRepository universalSentenceRepository;

	public UniversalSentenceCommandService(UniversalSentenceRepository universalSentenceRepository) {
		this.universalSentenceRepository = universalSentenceRepository;
	}

	@Transactional
	public UniversalSentence create(UniversalSentenceType type, String title, String sentence, List<String> tags,
		boolean active) {
		UniversalSentence universalSentence = new UniversalSentence(type, title, sentence, tags, active);
		return universalSentenceRepository.save(universalSentence);
	}

	@Transactional
	public Optional<UniversalSentence> update(UUID id, UniversalSentenceType type, String title, String sentence,
		List<String> tags, boolean active) {
		return universalSentenceRepository.findById(id)
			.map(existing -> {
				existing.update(type, title, sentence, tags, active);
				return universalSentenceRepository.save(existing);
			});
	}

	@Transactional
	public boolean delete(UUID id) {
		if (!universalSentenceRepository.existsById(id)) {
			return false;
		}
		universalSentenceRepository.deleteById(id);
		return true;
	}
}
