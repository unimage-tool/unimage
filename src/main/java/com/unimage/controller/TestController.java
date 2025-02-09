package com.unimage.controller;

import com.unimage.dto.ApiResponse;
import com.unimage.dto.ScreenshotDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TestController {

  @GetMapping("/")
  public ApiResponse<ScreenshotDto> showTempPage() {
    return ApiResponse.success(new ScreenshotDto("test"), HttpStatus.OK);
  }
}
