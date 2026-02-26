package com.opicer.api.shared.infrastructure;

import com.opicer.api.config.StorageProperties;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalAudioStorage implements AudioStorage {

	private final StorageProperties storageProperties;

	public LocalAudioStorage(StorageProperties storageProperties) {
		this.storageProperties = storageProperties;
	}

	@Override
	public String save(MultipartFile audio) {
		try {
			Path root = Path.of(storageProperties.getGoodAnswerDir()).toAbsolutePath().normalize();
			Files.createDirectories(root);
			String extension = resolveExtension(audio.getOriginalFilename());
			String filename = UUID.randomUUID() + extension;
			Path target = root.resolve(filename);
			Files.copy(audio.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
			return storageProperties.getGoodAnswerUrlPrefix() + "/" + filename;
		} catch (IOException e) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to store audio file");
		}
	}

	private String resolveExtension(String originalFilename) {
		if (originalFilename == null) return ".webm";
		int dot = originalFilename.lastIndexOf('.');
		if (dot < 0 || dot == originalFilename.length() - 1) return ".webm";
		return originalFilename.substring(dot);
	}
}
