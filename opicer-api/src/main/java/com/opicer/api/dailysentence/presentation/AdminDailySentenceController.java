package com.opicer.api.dailysentence.presentation;

import com.opicer.api.dailysentence.application.DailySentenceService;
import com.opicer.api.dailysentence.domain.DailySentence;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/daily-sentences")
public class AdminDailySentenceController {

	private final DailySentenceService dailySentenceService;

	public AdminDailySentenceController(DailySentenceService dailySentenceService) {
		this.dailySentenceService = dailySentenceService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<DailySentenceResponse>>> list() {
		List<DailySentenceResponse> sentences = dailySentenceService.findAll().stream()
			.map(DailySentenceResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("DAILY_SENTENCE_LIST_OK", sentences));
	}

	@PostMapping
	public ResponseEntity<Object> create(@Valid @RequestBody DailySentenceRequest request) {
		DailySentence sentence = dailySentenceService.create(
			request.date(), request.text(), request.level(), request.audioUrl());
		return ResponseEntity.status(201)
			.body(ApiResponse.created("DAILY_SENTENCE_CREATED", DailySentenceResponse.from(sentence)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Object> update(@PathVariable UUID id,
		@Valid @RequestBody DailySentenceRequest request) {
		return dailySentenceService.update(id, request.date(), request.text(), request.level(),
				request.audioUrl(), request.active() != null ? request.active() : true)
			.<ResponseEntity<Object>>map(s -> ResponseEntity.ok(
				ApiResponse.ok("DAILY_SENTENCE_UPDATED", DailySentenceResponse.from(s))))
			.orElseThrow(() -> new ApiException(
				ErrorCode.SENTENCE_NOT_FOUND,
				"DailySentence not found: " + id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Object> delete(@PathVariable UUID id) {
		if (dailySentenceService.delete(id)) {
			return ResponseEntity.ok(ApiResponse.ok("DAILY_SENTENCE_DELETED", null));
		}
		throw new ApiException(
			ErrorCode.SENTENCE_NOT_FOUND,
			"DailySentence not found: " + id);
	}

	public record DailySentenceRequest(
		@NotNull LocalDate date,
		@NotBlank String text,
		@NotNull OpicLevel level,
		String audioUrl,
		Boolean active
	) {}

	public record DailySentenceResponse(
		UUID id,
		LocalDate date,
		String text,
		OpicLevel level,
		String audioUrl,
		boolean active,
		Instant createdAt,
		Instant updatedAt
	) {
		static DailySentenceResponse from(DailySentence s) {
			return new DailySentenceResponse(s.getId(), s.getDate(), s.getText(), s.getLevel(),
				s.getAudioUrl(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
		}
	}
}
