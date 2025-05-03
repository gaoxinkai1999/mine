package com.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用于封装最新 Web 和 Native 版本信息的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestVersionsDto {

    private String webVersion;
    private String webReleaseNotes; // Added for Web release notes
    private String nativeVersion;
    private String nativeReleaseNotes; // Added for Native release notes
    private String apkUrl;

}
