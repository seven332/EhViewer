package com.hippo.ehviewer.network;

public class Constant {
    
    public static final int HTTP_TEMP_REDIRECT = 307;
    
    public static String defaultUserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76 Safari/537.36";
    public static String userAgent = System.getProperty("http.agent", defaultUserAgent);
    public static final int DEFAULT_TIMEOUT = 5 * 1000;
    public static final int MAX_REDIRECTS = 5;
    
    public static final int BUFFER_SIZE = 4096;
    
}
