package com.opicer.api.opic.presentation;

import com.opicer.api.opic.application.OpicSyncService;
import com.opicer.api.opic.application.OpicSyncWriter;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Manual trigger for the opic.or.kr sync; works even when the scheduled job is disabled. */
@RestController
@RequestMapping("/api/admin/opic-sync")
public class AdminOpicSyncController {

	private final OpicSyncService opicSyncService;

	public AdminOpicSyncController(OpicSyncService opicSyncService) {
		this.opicSyncService = opicSyncService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<SyncResponse>> sync(@RequestParam(defaultValue = "all") String target) {
		boolean schedules = "all".equals(target) || "schedules".equals(target);
		boolean centers = "all".equals(target) || "centers".equals(target);
		if (!schedules && !centers) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "target must be all, schedules or centers");
		}

		Integer scheduleCount = schedules ? opicSyncService.syncSchedules() : null;
		OpicSyncWriter.CenterSyncResult centerResult = centers ? opicSyncService.syncCenters() : null;
		return ResponseEntity.ok(ApiResponse.ok("OPIC_SYNCED", new SyncResponse(
			scheduleCount,
			centerResult == null ? null : centerResult.upserted(),
			centerResult == null ? null : centerResult.deactivated()
		)));
	}

	public record SyncResponse(Integer schedulesUpserted, Integer centersUpserted, Integer centersDeactivated) {}
}
