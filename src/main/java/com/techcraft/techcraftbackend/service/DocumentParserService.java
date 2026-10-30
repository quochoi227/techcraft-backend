package com.techcraft.techcraftbackend.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentParserService {
    String parse(MultipartFile file);
}
