package com.unimage.exception;

import lombok.Getter;

@Getter
public class CreateBackUpFileException extends Exception {

  private String readOnlyBackUpFile;
  private String networkTimeout;
  private String asynchronousTimeout;
  private String unspecified;

  public enum Cause {
    READ_ONLY_BACK_UP_FILE,
    NETWORK_TIMEOUT,
    ASYNCHRONOUS_TIMEOUT,
    UNSPECIFIED
  }

  public CreateBackUpFileException(Cause cause, String filename, Throwable e) {
    super(e);
    switch (cause) {
      case READ_ONLY_BACK_UP_FILE -> this.readOnlyBackUpFile =
          "Error: Unable to back up " + filename + " because it is read-only.\n"
              + "Solution: Check " + filename
              + " has write permissions. Maybe you can use File.setWritable(true).";
      case NETWORK_TIMEOUT -> this.networkTimeout =
          "Error: Unable to back up " + filename + " because network delay occurred.\n"
              + "Solution: Check your network connection.";
      case ASYNCHRONOUS_TIMEOUT -> this.asynchronousTimeout =
          "Error: Unable to back up " + filename + " because of asynchronous task time limit.\n" +
              "Solution: Consider increasing time limit or optimizing file system performance.";
      case UNSPECIFIED -> this.unspecified =
          "Error: Unable to back up " + filename + " because of unexpected reason.\n"
              + "Solution: Ask the administrator to look up for the reason.";
    }
  }
}
