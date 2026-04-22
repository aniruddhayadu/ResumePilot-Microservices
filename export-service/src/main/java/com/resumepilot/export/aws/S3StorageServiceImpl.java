package com.resumepilot.export.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class S3StorageServiceImpl implements S3StorageService {

	@Override
	public String uploadFile(String fileName, byte[] fileData, String contentType) {
		log.info("Mocking S3 Upload for file: {} of size: {} bytes", fileName, fileData.length);
		try {
			// Real AWS logic goes here later
			String fileUrl = "https://s3.amazonaws.com/resumepilot-bucket/" + fileName;
			log.info("Upload successful! Mock URL: {}", fileUrl);
			return fileUrl;
		} catch (Exception e) {
			log.error("Failed to upload file to S3: {}", fileName, e);
			throw new RuntimeException("S3 Upload Failed", e);
		}
	}
}