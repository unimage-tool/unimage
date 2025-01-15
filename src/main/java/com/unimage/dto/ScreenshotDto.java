package com.unimage.dto;

import lombok.AllArgsConstructor;

/**
 * 스크린샷 정보를 담는 DTO입니다.
 */
@AllArgsConstructor
public class ScreenshotDto {
    /**
     * 생성 시 저장한 파일명
     * ex) "logo.png"
     */
    public final String fileName;
    /**
     * 생성 시 저장한 파일경로
     * ex) "C:/Server/Unimage~~"
     */
    public final String filePath;
    /**
     * 생성 시 저장한 날짜
     * ex) "2024-09-23"
     */
    public final String date;
}
