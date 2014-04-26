package com.hippo.ehviewer.network;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hippo.ehviewer.util.Log;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefCookieHelper implements CookieStore {
    private static final String TAG = "PrefCookieHelper";
    private final String PREF_NAME = "cookies";
    
    private final SharedPreferences cookiePref;
    private Map<String, String> cookies;
    
    public PrefCookieHelper(Context context) {
        cookiePref = context.getSharedPreferences(PREF_NAME, 0);
        
        cookies = new HashMap<String, String>();
        Map<String, ?> all = cookiePref.getAll();
        for (Entry<String, ?> item : all.entrySet()) {
            Object value = item.getValue();
            if (value instanceof String)
                cookies.put(item.getKey(), (String)value);
        }
    }
    
    @Override
    public synchronized void add(URI uri, HttpCookie cookie) {
        uri.getHost();
    }

    @Override
    public synchronized List<HttpCookie> get(URI uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized List<HttpCookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized List<URI> getURIs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized boolean remove(URI uri, HttpCookie cookie) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public synchronized boolean removeAll() {
        // TODO Auto-generated method stub
        return false;
    }
}
