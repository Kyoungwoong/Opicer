package com.opicer.api.dailysentence.application;

import com.opicer.api.dailysentence.domain.DailySentence;
import com.opicer.api.dailysentence.infrastructure.DailySentenceRepository;
import com.opicer.api.shared.domain.OpicLevel;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailySentenceService {

	private final DailySentenceRepository dailySentenceRepository;

	public DailySentenceService(DailySentenceRepository dailySentenceRepository) {
		this.dailySentenceRepository = dailySentenceRepository;
	}

	@Transactional(readOnly = true)
	public List<DailySentence> findAll() {
		return dailySentenceRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<DailySentence> findById(UUID id) {
		return dailySentenceRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public Optional<DailySentence> findByDate(LocalDate date) {
		return dailySentenceRepository.findByDate(date);
	}

	/**
	 * @throws DuplicateDateException if the date already exists
	 */
	@Transactional
	public DailySentence create(LocalDate date, String text, OpicLevel level, String audioUrl) {
		if (dailySentenceRepository.existsByDate(date)) {
			throw new DuplicateDateException(date);
		}
		DailySentence sentence = new DailySentence(date, text, level, audioUrl);
		return dailySentenceRepository.save(sentence);
	}

	/**
	 * @throws DuplicateDateException if the new date conflicts with another record
	 */
	@Transactional
	public Optional<DailySentence> update(UUID id, LocalDate date, String text, OpicLevel level,
		String audioUrl, boolean active) {
		return dailySentenceRepository.findById(id).map(sentence -> {
			if (dailySentenceRepository.existsByDateAndIdNot(date, id)) {
				throw new DuplicateDateException(date);
			}
			sentence.update(date, text, level, audioUrl, active);
			return dailySentenceRepository.save(sentence);
		});
	}

	@Transactional
	public boolean delete(UUID id) {
		if (!dailySentenceRepository.existsById(id)) {
			return false;
		}
		dailySentenceRepository.deleteById(id);
		return true;
	}

	public static class DuplicateDateException extends RuntimeException {
		public DuplicateDateException(LocalDate date) {
			super("DailySentence already exists for date: " + date);
		}
	}
}
