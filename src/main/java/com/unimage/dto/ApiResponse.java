package com.unimage.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * API 응답을 나타내는 클래스입니다.
 *
 * @param <T> 응답 데이터의 타입
 */
public class ApiResponse<T> extends ResponseEntity<T> {

  /** 응답 메시지 */
  public final String message;

  /**
   * ApiResponse 생성자
   *
   * @param body    응답 데이터
   * @param message 응답 메시지
   * @param status  응답 상태코드
   */
  private ApiResponse(T body, String message, HttpStatus status) {
    super(body, status);
    this.message = message;
  }

  /**
   * 반환될 데이터와 함께 성공적인 응답을 생성하는 static 메서드
   *
   * @param <T>  응답 데이터 유형
   * @param body 응답 데이터
   * @return 성공적인 응답 객체
   */
  public static <T> ApiResponse<T> success(T body) {
    return new ApiResponse<>(body, null, HttpStatus.OK);
  }

  /**
   * 반환될 데이터 없이 성공적인 응답을 생성하는 static 메서드
   *
   * @param <T> 응답 데이터 유형
   * @return 성공적인 응답 객체
   */
  public static <T> ApiResponse<T> success() {
    return new ApiResponse<>(null, null, HttpStatus.OK);
  }

  /**
   * 실패 응답을 생성하는 static 메서드
   *
   * @param <T>     응답 데이터 유형
   * @param message 실패 메시지
   * @param status  응답 상태코드
   * @return 실패 응답 객체
   */
  public static <T> ApiResponse<T> error(String message, HttpStatus status) {
    return new ApiResponse<>(null, message, status);
  }

  /**
   * 실패 응답을 생성하는 static 메서드
   *
   * @param <T>     응답 데이터 유형
   * @param body    응답 데이터
   * @param message 실패 메시지
   * @param status  응답 상태코드
   * @return 실패 응답 객체
   */
  public static <T> ApiResponse<T> error(T body, String message, HttpStatus status) {
    return new ApiResponse<>(body, message, status);
  }
}
