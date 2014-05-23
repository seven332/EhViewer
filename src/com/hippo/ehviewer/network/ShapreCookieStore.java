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

package com.hippo.ehviewer.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hippo.ehviewer.util.Util;

import android.content.Context;
import android.content.SharedPreferences;

public class ShapreCookieStore implements CookieStore {
    //private static final String TAG = "ShapreCookieStore";
    private final CookieDataSource datasource;
    private final Map<URI, List<HttpCookie>> map;
    
    public ShapreCookieStore(Context context) {
        datasource = new CookieDataSource(context);
        map = datasource.getAll();
    }
    
    public synchronized void add(URI uri, HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie == null");
        }

        uri = cookiesUri(uri);
        List<HttpCookie> cookies = map.get(uri);
        if (cookies == null) {
            cookies = new ArrayList<HttpCookie>();
            map.put(uri, cookies);
        } else {
            cookies.remove(cookie);
        }
        cookies.add(cookie);
        
        // For sql
        if (cookies != null) {
            datasource.add(uri.getHost(), cookie);
        }
    }

    private URI cookiesUri(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            return new URI("http", uri.getHost(), null, null);
        } catch (URISyntaxException e) {
            return uri; // probably a URI with no host
        }
    }

    public synchronized List<HttpCookie> get(URI uri) {/*
        if (uri == null) {
            throw new NullPointerException("uri == null");
        }

        List<HttpCookie> result = new ArrayList<HttpCookie>();

        // get cookies associated with given URI. If none, returns an empty list
        List<HttpCookie> cookiesForUri = map.get(uri);
        if (cookiesForUri != null) {
            for (Iterator<HttpCookie> i = cookiesForUri.iterator(); i.hasNext(); ) {
                HttpCookie cookie = i.next();
                if (cookie.hasExpired()) {
                    i.remove(); // remove expired cookies
                    // For sql
                    datasource.remove(uri.getHost(), cookie);
                } else {
                    result.add(cookie);
                }
            }
        }

        // get all cookies that domain matches the URI
        for (Map.Entry<URI, List<HttpCookie>> entry : map.entrySet()) {
            if (uri.equals(entry.getKey())) {
                continue; // skip the given URI; we've already handled it
            }

            List<HttpCookie> entryCookies = entry.getValue();
            for (Iterator<HttpCookie> i = entryCookies.iterator(); i.hasNext(); ) {
                HttpCookie cookie = i.next();
                if (!HttpCookie.domainMatches(cookie.getDomain(), uri.getHost())) {
                    continue;
                }
                if (cookie.hasExpired()) {
                    i.remove(); // remove expired cookies
                    // For sql
                    datasource.remove(uri.getHost(), cookie);
                } else if (!result.contains(cookie)) {
                    result.add(cookie);
                }
            }
        }
        return Collections.unmodifiableList(result);*/
        
        
        List<HttpCookie> result = new ArrayList<HttpCookie>();
        for (URI uri1 : map.keySet()) {
            List<HttpCookie> list = map.get(uri1);
            for (Iterator<HttpCookie> i = list.iterator(); i.hasNext(); ) {
                HttpCookie cookie = i.next();
                if (cookie.hasExpired()) {
                    i.remove(); // remove expired cookies
                    // For sql
                    datasource.remove(uri1.getHost(), cookie);
                } else if (!result.contains(cookie)) {
                    result.add(cookie);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized List<HttpCookie> getCookies() {
        List<HttpCookie> result = new ArrayList<HttpCookie>();
        for (URI uri : map.keySet()) {
            List<HttpCookie> list = map.get(uri);
            for (Iterator<HttpCookie> i = list.iterator(); i.hasNext(); ) {
                HttpCookie cookie = i.next();
                if (cookie.hasExpired()) {
                    i.remove(); // remove expired cookies
                    // For sql
                    datasource.remove(uri.getHost(), cookie);
                } else if (!result.contains(cookie)) {
                    result.add(cookie);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized List<URI> getURIs() {
        List<URI> result = new ArrayList<URI>(map.keySet());
        result.remove(null); // sigh
        return Collections.unmodifiableList(result);
    }

    public synchronized boolean remove(URI uri, HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie == null");
        }
        
        // For sql
        datasource.remove(uri.getHost(), cookie);
        
        List<HttpCookie> cookies = map.get(cookiesUri(uri));
        if (cookies != null) {
            return cookies.remove(cookie);
        } else {
            return false;
        }
    }

    public synchronized boolean removeAll() {
        boolean result = !map.isEmpty();
        map.clear();
        
        // For sql
        datasource.removeAll();
        
        return result;
    }
    
    private class CookieDataSource {
        
        private static final String COOKIE_PREFS = "cookie";
        private static final String COOKIE_URI_LIST = "cookie_uri_list";
        private static final String COOKIE_URI_PREFIX = "uri_";
        private static final String COOKIE_NAME_PREFIX = "name_";
        private final SharedPreferences cookiePrefs;
        private final Map<String, Set<String>> data;
        
        public CookieDataSource(Context context) {
            cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, 0);
            data = new HashMap<String, Set<String>>();
        }
        
        public Map<URI, List<HttpCookie>> getAll() {
            Map<URI, List<HttpCookie>> map = new HashMap<URI, List<HttpCookie>>();
            String rawUris = cookiePrefs.getString(COOKIE_URI_LIST, null);
            if (rawUris == null)
                return map;
            String[] strUris = decodeStrings(rawUris);
            
            for (String strUri : strUris) {
                URI uri = null;
                try {
                    uri = new URI("http", strUri, null, null);
                } catch (URISyntaxException e) {continue;}
                
                // Add to map
                data.put(strUri, new HashSet<String>());
                String rawNames = cookiePrefs.getString(COOKIE_URI_PREFIX + strUri, null);
                if (rawNames == null)
                    continue;
                String[] names = decodeStrings(rawNames);
                List<HttpCookie> cookies = new ArrayList<HttpCookie>();
                for (String name : names) {
                    String strCookie = cookiePrefs.getString(COOKIE_NAME_PREFIX + strUri + name, null);
                    if (strCookie == null)
                        continue;
                    
                    // Add to map
                    data.get(strUri).add(name);
                    cookies.add(decodeCookie(strCookie));
                }
                map.put(uri, cookies);
            }
            return map;
        }
        
        
        public void add(String uri, HttpCookie cookie) {
            Set<String> names = data.get(uri);
            if (names == null) {
                names = new HashSet<String>();
                data.put(uri, names);
                putStrings(COOKIE_URI_LIST, data.keySet());
            }
            
            if (!names.contains(cookie.getName())) {
                names.add(cookie.getName());
                putStrings(COOKIE_URI_PREFIX + uri, names);
            }
            
            cookiePrefs.edit().putString(COOKIE_NAME_PREFIX
                    + uri + cookie.getName(),
                    encodeCookie(cookie))
                    .apply();
        }
        
        public void remove(String uri, HttpCookie cookie) {
            cookiePrefs.edit().remove(COOKIE_NAME_PREFIX
                    + uri + cookie.getName())
                    .apply();
            
            Set<String> names = data.get(uri);
            names.remove(cookie.getName());
            if (names.size() != 0)
                putStrings(COOKIE_URI_PREFIX + uri, names);
            else {
                cookiePrefs.edit().remove(COOKIE_URI_PREFIX + uri)
                        .apply();
                data.remove(uri);
                putStrings(COOKIE_URI_LIST, data.keySet());
            }
        }
        
        public void removeAll() {
            cookiePrefs.edit().clear().apply();
        }
        
        public void putStrings(String key, Set<String> values) {
            StringBuffer sb = new StringBuffer();
            for (String value : values) {
                sb.append(value).append('\n');
            }
            int length = sb.length();
            sb.delete(length - 1, length);
            cookiePrefs.edit()
                    .putString(key, Util.byteArrayToHexString(
                    sb.toString().getBytes())).apply();
        }
        
        public String[] decodeStrings(String rawStrings) {
            return new String(Util.hexStringToByteArray(rawStrings)).split("\n");
        }
        
        protected String encodeCookie(HttpCookie cookie) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(os);
                writeObject(cookie, outputStream);
            } catch (Exception e) {
                return null;
            } finally {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return Util.byteArrayToHexString(os.toByteArray());
        }

        protected HttpCookie decodeCookie(String cookieStr) {
            byte[] bytes = Util.hexStringToByteArray(cookieStr);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            HttpCookie cookie = null;
            try {
                ObjectInputStream ois = new ObjectInputStream(is);
                cookie = readObject(ois);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return cookie;
        }
    }
    
    
    public static void writeObject(HttpCookie cookie, ObjectOutputStream out) throws IOException {
        out.writeObject(cookie.getName());
        out.writeObject(cookie.getValue());
        out.writeObject(cookie.getComment());
        out.writeObject(cookie.getCommentURL());
        out.writeObject((Boolean)cookie.getDiscard());
        out.writeObject(cookie.getDomain());
        out.writeObject((Long)cookie.getMaxAge());
        out.writeObject(cookie.getPath());
        out.writeObject(cookie.getPortlist());
        out.writeObject((Boolean)cookie.getSecure());
        out.writeObject((Integer)cookie.getVersion());
    }

    public static HttpCookie readObject(ObjectInputStream in) {
        HttpCookie clientCookie = null;
        try {
            String name = (String)in.readObject();
            String value = (String)in.readObject();
            clientCookie = new HttpCookie(name, value);
            clientCookie.setComment((String)in.readObject());
            clientCookie.setCommentURL((String)in.readObject());
            clientCookie.setDiscard((Boolean)in.readObject());
            clientCookie.setDomain((String)in.readObject());
            clientCookie.setMaxAge((Long)in.readObject());
            clientCookie.setPath((String)in.readObject());
            clientCookie.setPortlist((String)in.readObject());
            clientCookie.setSecure((Boolean)in.readObject());
            clientCookie.setVersion((Integer)in.readObject());
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return clientCookie;
    }
}
