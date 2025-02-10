package com.unimage.dto;

import lombok.RequiredArgsConstructor;

/** 스크린샷 정보를 담는 DTO입니다. */
@RequiredArgsConstructor
public class ScreenshotDto {

  /** 생성 시 저장한 파일명 ex) "logo.png" */
  public final String fileName;
}
