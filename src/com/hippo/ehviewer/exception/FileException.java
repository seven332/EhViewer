/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    
    
    // TODO
    public String getMessage() {
        switch (mErrorCode) {
        case MKDIR_ERROR:
            return "mkdir error";
        case CREATE_FILE_ERROR:
            return "create file error";
        case NOT_DIR:
            return "it is not a dir";
        case NOT_FILE:
            return "it is not a file";
        case CANNOT_EXECUTE:
            return "can not execute";
        case CANNOT_READ:
            return "can not read";
        case CANNOT_WRITE:
            return "can not write";
        default:
            return "unknown error";
        }
    }
    
}
