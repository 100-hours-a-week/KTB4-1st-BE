package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.image.entity.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class S3ImageUrlResolver implements ImageUrlResolver {

	private final S3ImageObjectService s3ImageObjectService;

	@Override
	public String resolve(Image image) {
		return image.getObjectKey() == null
				? image.getImageUrl()
				: s3ImageObjectService.presignedReadUrl(image.getObjectKey());
	}
}
