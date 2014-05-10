package com.hippo.ehviewer.ehclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetailUrlParser {
    
    public int gid;
    public String token;
    
    public boolean parser(String url) {
        Pattern p = Pattern.compile("/(\\d+)/(\\w+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            gid = Integer.parseInt(m.group(1));
            token = m.group(2);
            return true;
        } else {
            return false;
        }
    }
}
