package com.unimage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

/** 스크린샷 정보를 담는 DTO입니다. */
@AllArgsConstructor
public class ScreenshotDto {

  /** 생성 시 저장한 파일명 ex) "logo.png" */
  @JsonProperty
  public final String fileName;
}
