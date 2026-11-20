package com.techcraft.techcraftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudinaryUploadResponse {

    private String publicId;
    private String url;
    private String secureUrl;
    private String format;
    private Integer width;
    private Integer height;
    private Long bytes;
    private String originalFilename;
    private String createdAt;
}
