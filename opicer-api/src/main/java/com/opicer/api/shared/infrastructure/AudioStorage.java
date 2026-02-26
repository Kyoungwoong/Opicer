package com.opicer.api.shared.infrastructure;

import org.springframework.web.multipart.MultipartFile;

public interface AudioStorage {
	String save(MultipartFile audio);
}
