package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.image.entity.Image;

@FunctionalInterface
public interface ImageUrlResolver {

	String resolve(Image image);
}
