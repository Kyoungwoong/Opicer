package com.opicer.api.opic.presentation;

import com.opicer.api.opic.application.OpicCenterCommandService;
import com.opicer.api.opic.application.OpicCenterCommandService.CenterInput;
import com.opicer.api.opic.application.OpicCenterCommandService.Saved;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-entered OPIc test centers (ROLE_ADMIN via the /api/admin/** rule). Centers are deactivated, never deleted. */
@RestController
@RequestMapping("/api/admin/opic-centers")
public class AdminOpicCenterController {

	private final OpicCenterCommandService commandService;

	public AdminOpicCenterController(OpicCenterCommandService commandService) {
		this.commandService = commandService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<CenterResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTER_LIST_OK",
			commandService.findAll().stream().map(c -> CenterResponse.from(c, null)).toList()));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CenterResponse>> create(@Valid @RequestBody CenterRequest request) {
		Saved saved = commandService.create(request.toInput());
		return ResponseEntity.status(201)
			.body(ApiResponse.created("OPIC_CENTER_CREATED", CenterResponse.from(saved.center(), saved.warning())));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<CenterResponse>> update(@PathVariable UUID id,
		@Valid @RequestBody CenterRequest request) {
		Saved saved = commandService.update(id, request.toInput());
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTER_UPDATED", CenterResponse.from(saved.center(), saved.warning())));
	}

	@PostMapping("/{id}/activate")
	public ResponseEntity<ApiResponse<CenterResponse>> activate(@PathVariable UUID id) {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTER_ACTIVATED",
			CenterResponse.from(commandService.setActive(id, true), null)));
	}

	@PostMapping("/{id}/deactivate")
	public ResponseEntity<ApiResponse<CenterResponse>> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTER_DEACTIVATED",
			CenterResponse.from(commandService.setActive(id, false), null)));
	}

	public record CenterRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Size(max = 500) String address,
		@Size(max = 255) String phone,
		@Size(max = 255) String subwayLines,
		@Size(max = 2000) String directions,
		@Size(max = 32) String regionName,
		Double latitude,
		Double longitude
	) {
		CenterInput toInput() {
			return new CenterInput(name, address, phone, subwayLines, directions, regionName, latitude, longitude);
		}
	}

	/** @param geocodeWarning set when the center was saved without coordinates */
	public record CenterResponse(
		UUID id,
		String name,
		String address,
		Double latitude,
		Double longitude,
		String phone,
		String subwayLines,
		String directions,
		String regionName,
		boolean active,
		Instant updatedAt,
		String geocodeWarning
	) {
		static CenterResponse from(OpicTestCenter c, String warning) {
			return new CenterResponse(c.getId(), c.getName(), c.getAddress(), c.getLatitude(), c.getLongitude(),
				c.getPhone(), c.getSubwayLines(), c.getDirections(), c.getRegionName(), c.isActive(), c.getSyncedAt(),
				warning);
		}
	}
}
