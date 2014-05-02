package com.hippo.ehviewer.ehclient;

import org.json.JSONException;
import org.json.JSONObject;

public class ParserRate extends EhParser {
    
    public float mRatingAvg;
    public int mRatingCnt;
    
    @Override
    boolean parser(String pageContext) {
        try {
            JSONObject jsonObject = new JSONObject(pageContext);
            mRatingAvg = (float)jsonObject.getDouble("rating_avg");
            mRatingCnt = jsonObject.getInt("rating_cnt");
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
