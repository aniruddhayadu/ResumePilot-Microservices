package com.resumepilot.export.aws;

public interface S3StorageService {
	String uploadFile(String fileName, byte[] fileData, String contentType);
}