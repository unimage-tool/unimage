package com.unimage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
public class ScreenshotDto {
    private String fileName;
    private String filePath;
    private String date;
}
