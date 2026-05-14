package com.resumepilot.export.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

	private final AmazonS3 s3Client;

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Override
	public String uploadFile(String fileName, byte[] fileData, String contentType) {
		try {
			log.info("Uploading PDF to AWS S3 Bucket: {}", bucketName);

			InputStream inputStream = new ByteArrayInputStream(fileData);

			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(fileData.length);
			metadata.setContentType(contentType);

			s3Client.putObject(new PutObjectRequest(bucketName, fileName, inputStream, metadata));

			String fileUrl = s3Client.getUrl(bucketName, fileName).toString();
			log.info("Upload successful! Public URL: {}", fileUrl);

			return fileUrl;

		} catch (Exception e) {
			log.error("Failed to upload file to S3: {}", fileName, e);
			throw new RuntimeException("S3 Upload Failed", e);
		}
	}
}