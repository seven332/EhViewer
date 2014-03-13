package com.hippo.ehviewer.network;

class StopRequestException extends Exception {
    
    private final int mFinalStatus;

    public StopRequestException(int finalStatus, String message) {
        super(message);
        mFinalStatus = finalStatus;
    }

    public StopRequestException(int finalStatus, Throwable t) {
        super(t);
        mFinalStatus = finalStatus;
    }

    public StopRequestException(int finalStatus, String message, Throwable t) {
        super(message, t);
        mFinalStatus = finalStatus;
    }
    
    public int getFinalStatus() {
        return mFinalStatus;
    }

    public static StopRequestException throwUnhandledHttpError(int code, String message)
            throws StopRequestException {
        final String error = "Unhandled HTTP response: " + code + " " + message;
        throw new StopRequestException(code, error);
    }
}
