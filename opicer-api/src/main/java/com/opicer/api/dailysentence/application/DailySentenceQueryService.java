package com.opicer.api.dailysentence.application;

import com.opicer.api.dailysentence.domain.DailySentence;
import com.opicer.api.dailysentence.infrastructure.DailySentenceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailySentenceQueryService {

	private final DailySentenceRepository dailySentenceRepository;

	public DailySentenceQueryService(DailySentenceRepository dailySentenceRepository) {
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
}
