package com.unimage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScreenshotDto {
    private final String fileName; // 생성시 저장한 파일 명 ex) "logo.png"
    private final String filePath; // 생성시 저장한 경로 ex) "C:/Server/Unimage~~"
    private final String date; // 생성 날짜 ex) "2024-09-23"
}
