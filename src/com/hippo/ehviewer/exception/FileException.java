package com.hippo.ehviewer.exception;

public class FileException extends Exception {
    
    public static final int MKDIR_ERROR = 0x0;
    public static final int CREATE_FILE_ERROR = 0x1;
    public static final int NOT_DIR = 0x2;
    public static final int NOT_FILE = 0x3;
    public static final int CANNOT_EXECUTE = 0x4;
    public static final int CANNOT_READ = 0x5;
    public static final int CANNOT_WRITE = 0x6;
    
    private int mErrorCode;
    
    private static final long serialVersionUID = 1L;
    
    public FileException(int errorCode) {
        mErrorCode = errorCode;
    }
    
    public int getErrorCode() {
        return mErrorCode;
    }
    
}
