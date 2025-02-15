package com.unimage.exception;

public class CreateBackUpFileException extends Exception {

  public enum ExceptionType {
    UNSUPPORTED_OPERATION_EXCEPTION,
    SOCKET_EXCEPTION,
    INTERRUPTED_BY_TIMEOUT_EXCEPTION,
    IO_EXCEPTION
  }

  public CreateBackUpFileException(String message, Throwable cause) {
    super(message, cause);
  }
}
