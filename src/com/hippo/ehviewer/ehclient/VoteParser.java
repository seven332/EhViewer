package com.hippo.ehviewer.ehclient;

import org.json.JSONException;
import org.json.JSONObject;

public class VoteParser {
    
    public String mTagPane;
    
    boolean parser(String pageContext) {
        try {
            JSONObject jsonObject = new JSONObject(pageContext);
            mTagPane = jsonObject.getString("tagpane");
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
