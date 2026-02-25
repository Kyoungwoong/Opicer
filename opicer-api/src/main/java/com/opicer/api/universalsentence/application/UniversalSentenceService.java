package com.opicer.api.universalsentence.application;

import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import com.opicer.api.universalsentence.infrastructure.UniversalSentenceRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniversalSentenceService {

	private static final int MAX_RANDOM_SIZE = 4;

	private final UniversalSentenceRepository universalSentenceRepository;

	public UniversalSentenceService(UniversalSentenceRepository universalSentenceRepository) {
		this.universalSentenceRepository = universalSentenceRepository;
	}

	@Transactional(readOnly = true)
	public List<UniversalSentence> findAll() {
		return universalSentenceRepository.findAll();
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

	@Transactional(readOnly = true)
	public List<UniversalSentence> findRandom(int size) {
		List<UniversalSentence> candidates = new ArrayList<>(universalSentenceRepository.findByActiveTrue());
		if (candidates.isEmpty()) {
			return List.of();
		}
		Collections.shuffle(candidates);
		int limit = size <= 0 ? MAX_RANDOM_SIZE : Math.min(size, MAX_RANDOM_SIZE);
		return candidates.subList(0, Math.min(limit, candidates.size()));
	}

	public UniversalSentence getOrThrow(UUID id) {
		return universalSentenceRepository.findById(id)
			.orElseThrow(() -> new ApiException(ErrorCode.UNIVERSAL_SENTENCE_NOT_FOUND,
				"Universal sentence not found: " + id));
	}
}
