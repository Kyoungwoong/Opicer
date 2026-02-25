package com.opicer.api.universalsentence.application;

import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import com.opicer.api.universalsentence.infrastructure.UniversalSentenceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniversalSentenceService {

	private static final int MAX_RANDOM_SIZE = 4;

	private final UniversalSentenceRepository universalSentenceRepository;
	private final Clock clock;

	public UniversalSentenceService(UniversalSentenceRepository universalSentenceRepository, Clock clock) {
		this.universalSentenceRepository = universalSentenceRepository;
		this.clock = clock;
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

	@Transactional(readOnly = true)
	public List<UniversalSentence> findDailySet() {
		List<UniversalSentence> activeSentences = universalSentenceRepository.findByActiveTrueOrderByCreatedAtAscIdAsc();
		if (activeSentences.isEmpty()) {
			throw new ApiException(ErrorCode.UNIVERSAL_SENTENCE_DAILY_NOT_READY,
				"No active universal sentences");
		}

		EnumMap<UniversalSentenceType, List<UniversalSentence>> grouped =
			new EnumMap<>(UniversalSentenceType.class);
		for (UniversalSentenceType type : UniversalSentenceType.values()) {
			grouped.put(type, new ArrayList<>());
		}
		for (UniversalSentence sentence : activeSentences) {
			grouped.get(sentence.getType()).add(sentence);
		}

		long dayIndex = LocalDate.now(clock).toEpochDay();
		List<UniversalSentence> dailySet = new ArrayList<>();
		for (UniversalSentenceType type : UniversalSentenceType.values()) {
			List<UniversalSentence> typed = grouped.get(type);
			if (typed == null || typed.isEmpty()) {
				throw new ApiException(ErrorCode.UNIVERSAL_SENTENCE_DAILY_NOT_READY,
					"Missing type for daily set: " + type);
			}
			int index = Math.floorMod(Objects.hash(dayIndex, type.name()), typed.size());
			dailySet.add(typed.get(index));
		}
		return dailySet;
	}

	public UniversalSentence getOrThrow(UUID id) {
		return universalSentenceRepository.findById(id)
			.orElseThrow(() -> new ApiException(ErrorCode.UNIVERSAL_SENTENCE_NOT_FOUND,
				"Universal sentence not found: " + id));
	}
}
