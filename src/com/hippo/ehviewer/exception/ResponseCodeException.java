package com.hippo.ehviewer.exception;

public class ResponseCodeException extends Exception {
    
    private static final long serialVersionUID = 1L;
    private static final String eMsg = "Error response code";
    private int mResponseCode;
    
    public ResponseCodeException(int responseCode) {
        this(responseCode, eMsg);
    }
    
    public ResponseCodeException(int responseCode, String message) {
        super(message);
        mResponseCode = responseCode;
    }
    
    public int getResponseCode() {
        return mResponseCode;
    }
}
