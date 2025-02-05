package com.unimage.controller;

import com.unimage.dto.ApiResponse;
import com.unimage.dto.ScreenshotDto;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.IIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 스크린샷 파일의 저장, 조회, 수정, 삭제와 같은 작업을 처리하는 API를 제공합니다.
 *
 * <p>주요 기능:</p>
 *
 * <ul>
 *     <li>전달된 한 장의 스크린샷 저장.</li>
 *     <li>저장된 모든 스크린샷 조회.</li>
 *     <li>선택한 하나의 스크린샷 조회.</li>
 *     <li>스크린샷의 파일명 수정.</li>
 *     <li>리스트 단위로 선택된 스크린샷 삭제, 실패 시 자동 복원.</li>
 * </ul>
 */
@RestController
@RequestMapping("/screenshot")
public class ScreenshotController {

  private static final Logger logger = LoggerFactory.getLogger(ScreenshotController.class);
  private static final String UPLOAD_DIR = "C:/Server/Unimage/screenshot/";
  private static final String BACKUP_DIR = UPLOAD_DIR + "backup/";

  /**
   * 전달된 스크린샷 파일을 지정된 파일명으로 저장합니다. 파일명이 null로 전달될 경우 UUID를 생성하여 저장합니다.
   *
   * @param file     저장할 스크린샷 파일
   * @param filename 저장할 파일명 (null일 경우 UUID 부여)
   * @return {@link ApiResponse}를 통해 성공 시 201 CREATED와 생성된 {@link ScreenshotDto} 반환, 실패 시 상태 코드와 에러
   * 메시지 반환, {@link ScreenshotDto}는 스크린샷의 파일명을 포함
   */
  @PostMapping("/upload")
  public ApiResponse<ScreenshotDto> uploadScreenshot(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "filename", required = false) String filename) {
    if (file == null || file.isEmpty()) {
      return ApiResponse.error("Screenshot file is empty", HttpStatus.BAD_REQUEST);
    }

    if (filename == null || filename.isEmpty()) {
      filename = UUID.randomUUID().toString();
    }

    if (new File(UPLOAD_DIR + filename).exists()) {
      return ApiResponse.error(filename + " already exists", HttpStatus.CONFLICT);
    }

    File uploadDir = new File(UPLOAD_DIR);
    if (!uploadDir.exists()) {
      uploadDir.mkdirs();
    }

    try {
      file.transferTo(new File(UPLOAD_DIR + filename));
      return ApiResponse.success(new ScreenshotDto(filename), HttpStatus.CREATED);
    } catch (IIOException e) {
      e.printStackTrace();
      return ApiResponse.error("File is damaged or not supported", HttpStatus.BAD_REQUEST);
    } catch (ClosedChannelException e) {
      e.printStackTrace();
      return ApiResponse.error("File stream is closed", HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (FileSystemException e) {
      e.printStackTrace();
      return ApiResponse.error("File needs permission or is locked",
          HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (SocketException e) {
      e.printStackTrace();
      return ApiResponse.error("Network error occurred", HttpStatus.SERVICE_UNAVAILABLE);
    } catch (InterruptedByTimeoutException e) {
      e.printStackTrace();
      return ApiResponse.error("Timeout occurred", HttpStatus.GATEWAY_TIMEOUT);
    } catch (InterruptedIOException e) {
      e.printStackTrace();
      return ApiResponse.error("Interrupt occurred: try it again", HttpStatus.SERVICE_UNAVAILABLE);
    } catch (IOException e) {
      e.printStackTrace();
      return ApiResponse.error("Unexpected error " + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (IllegalStateException e) {
      e.printStackTrace();
      return ApiResponse.error("File already stored", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 저장된 파일들을 날짜순으로 전부 불러옵니다.
   *
   * @return {@link ApiResponse}를 통해 성공 시 200 OK와 {@link List<ScreenshotDto>} 반환, 실패 시 상태 코드와 에러 메시지
   * 반환 {@link ScreenshotDto}는 스크린샷의 파일명을 포함
   */
  @GetMapping("/all")
  public ApiResponse<List<ScreenshotDto>> getAllScreenshots() {
    File[] files = new File(UPLOAD_DIR).listFiles();

    if (files == null) {
      return ApiResponse.error("Can't load screenshots", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    List<ScreenshotDto> screenshotList = Arrays.stream(files)
        .filter(File::isFile)
        .map(file -> new ScreenshotDto(file.getName()))
        .collect(Collectors.toList());
    return ApiResponse.success(screenshotList, HttpStatus.OK);
  }

  /**
   * 지정된 파일 정보를 불러옵니다.
   *
   * @param filename 반환할 파일의 파일명
   * @return {@link ApiResponse}를 통해 성공 시 200 OK와 {@link ScreenshotDto} 반환, 실패 시 상태 코드와 에러 메시지 반환
   * {@link ScreenshotDto}는 스크린샷의 파일명을 포함
   */
  @GetMapping("/{filename}")
  public ApiResponse<ScreenshotDto> getScreenshot(@PathVariable String filename) {
    if (filename == null || filename.isEmpty()) {
      return ApiResponse.error("File name needs at least 1 character", HttpStatus.BAD_REQUEST);
    }

    if (!new File(UPLOAD_DIR + filename).exists()) {
      return ApiResponse.error(filename + " does not exist", HttpStatus.NOT_FOUND);
    }

    return ApiResponse.success(new ScreenshotDto(filename), HttpStatus.OK);
  }

  /**
   * 지정된 파일의 파일명을 수정합니다.
   *
   * @param filename    기존 파일명
   * @param newFilename 새 파일명
   * @return {@link ApiResponse}를 통해 성공 시 200 OK와 {@link ScreenshotDto} 반환, 실패 시 상태 코드와 에러 메시지 반환
   * {@link ScreenshotDto}는 스크린샷의 파일명을 포함
   */
  @PutMapping("/modify")
  public ApiResponse<ScreenshotDto> modifyScreenshot(
      @RequestParam("filename") String filename,
      @RequestParam("newFilename") String newFilename) {
    if (filename == null || filename.isEmpty()) {
      return ApiResponse.error("File name needs at least 1 character", HttpStatus.BAD_REQUEST);
    }

    if (newFilename == null || newFilename.isEmpty()) {
      return ApiResponse.error("New file name needs at least 1 character", HttpStatus.BAD_REQUEST);
    }

    File originalFile = new File(UPLOAD_DIR + filename);
    File newFile = new File(UPLOAD_DIR + newFilename);
    if (!originalFile.exists()) {
      return ApiResponse.error(filename + " does not exist", HttpStatus.NOT_FOUND);
    }
    if (newFile.exists()) {
      return ApiResponse.error(newFilename + " already exists", HttpStatus.CONFLICT);
    }

    if (originalFile.renameTo(newFile)) {
      return ApiResponse.success(new ScreenshotDto(newFilename), HttpStatus.OK);
    } else {
      return ApiResponse.error(filename + " is locked", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 전달된 파일 리스트를 삭제합니다. 삭제 중 오류 발생 시 모든 파일을 복원합니다.
   *
   * @param fileList 삭제할 파일명을 담고있는 리스트
   * @return {@link ApiResponse}를 통해 성공 시 204 NO_CONTENT 반환, 실패 시 상태 코드, 에러 메시지,
   * {@link List<String>} 반환 {@link List<String>}는 각 파일들의 삭제 결과를 포함
   */
  @DeleteMapping("/delete")
  public ApiResponse<List<String>> deleteScreenshot(
      @RequestParam("fileList") List<String> fileList) {
    if (fileList == null || fileList.isEmpty()) {
      return ApiResponse.error("File list needs at least 1 file", HttpStatus.BAD_REQUEST);
    }

    // 리스트 내의 파일 이름의 유효성 검사
    for (int i = 0; i < fileList.size(); i++) {
      String filename = fileList.get(i);
      if (filename == null || filename.isEmpty()) {
        return ApiResponse.error("Index " + i + " data in file list needs at least 1 character",
            HttpStatus.BAD_REQUEST);
      }

      if (!new File(UPLOAD_DIR + filename).exists()) {
        return ApiResponse.error(filename + "doesn't exist", HttpStatus.NOT_FOUND);
      }
    }

    // 파일 백업
    File backupDir = new File(BACKUP_DIR);
    if (!backupDir.exists()) {
      backupDir.mkdirs();
    }

    List<File> backupFiles = new ArrayList<>();
    for (String filename : fileList) {
      File originalFile = new File(UPLOAD_DIR + filename);
      File backupFile = new File(BACKUP_DIR + filename);

      try {
        Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        backupFiles.add(backupFile);
      } catch (UnsupportedOperationException e) {
        e.printStackTrace();
        logger.error("Back up failed file: {}. Reason: {} read-only. Exception: {}", filename,
            filename, e.getMessage());
      } catch (SocketException e) {
        e.printStackTrace();
        logger.error("Back up failed file: {}. Reason: Network error. Exception: {}", filename,
            e.getMessage());
      } catch (InterruptedByTimeoutException e) {
        e.printStackTrace();
        logger.error("Back up failed file: {}. Reason: Timeout. Exception: {}", filename,
            e.getMessage());
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("Back up failed file: {}. Reason: Unexpected. Exception: {}", filename,
            e.getMessage());
      }
    }
    if (backupFiles.size() != fileList.size()) {
      deleteBackupFiles(backupFiles);
      return ApiResponse.error("Error occurred making back up file. Try it later",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 파일 제거 성공여부를 기록하여 실패한 경우 있을 시, 각 파일의 삭제 가능 여부 반환에 사용
    List<String> deleteResults = new ArrayList<>();
    boolean isDeleteSuccessful = true;
    for (String filename : fileList) {
      if (new File(UPLOAD_DIR + filename).delete()) {
        deleteResults.add(filename + "deletion available");
      } else {
        deleteResults.add(filename + "deletion not available");
        isDeleteSuccessful = false;
      }
    }

    // 리스트 단위 삭제 성공 시 백업 파일들 삭제, 실패 시 복원 진행 및 복원 실패 로그에 기록
    if (isDeleteSuccessful) {
      deleteBackupFiles(backupFiles);
      return ApiResponse.success(HttpStatus.NO_CONTENT);
    } else {
      for (File backupFile : backupFiles) {
        File file = new File(UPLOAD_DIR + backupFile.getName());
        boolean isCopySuccessful = false;

        if (file.exists()) {
          if (!backupFile.delete()) {
            logger.error(
                "Failed to delete back up file: {}. This file already exists at {}",
                backupFile.getName(), UPLOAD_DIR);
          }
        } else {
          try {
            Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            isCopySuccessful = true;
          } catch (UnsupportedOperationException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: {} read-only. Exception: {}",
                backupFile.getName(), backupFile.getName(), e.getMessage());
          } catch (SocketException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: Network error. Exception: {}",
                backupFile.getName(), e.getMessage());
          } catch (InterruptedByTimeoutException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: Timeout. Exception: {}",
                backupFile.getName(), e.getMessage());
          } catch (IOException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: Unexpected. Exception: {}",
                backupFile.getName(), e.getMessage());
          } finally {
            if (isCopySuccessful && !backupFile.delete()) {
              logger.error(
                  "Failed to delete back up file: {}. This file already restored at {}",
                  backupFile.getName(), UPLOAD_DIR);
            }
          }
        }
      }
    }

    return ApiResponse.error(deleteResults, "Some files are using so can't be deleted",
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private void deleteBackupFiles(List<File> backupFiles) {
    for (File backupFile : backupFiles) {
      if (!backupFile.delete()) {
        logger.error("Failed to delete back up file while deleting all back up files: {}",
            backupFile.getName());
      }
    }
  }
}
