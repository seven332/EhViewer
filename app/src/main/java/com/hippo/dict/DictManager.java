package com.hippo.dict;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DictManager {

    // pattern for language
    private static final String enRegEx = "[0-9a-zA-Z\\s]";
    // private static final String zhRegEx = "";

    private DictDatabase mDictDatabase;

    public DictManager(Context context) {
        mDictDatabase = DictDatabase.getInstance(context);
    }

    public void importDict(final Uri dictUri, final DictImportService.ProcessListener listener)
            throws IOException, URISyntaxException {
        mDictDatabase.importDict(dictUri, listener);
    }

    public void deletDict(String dict) {
        mDictDatabase.deletDict(dict);
    }

    public String[] getSuggestions(String prefix) {
        List<String> result = new ArrayList<>();
        String databaseResult[] = mDictDatabase.getSuggestions(prefix);
        for (String s : databaseResult) {
            if (filter(s)) {
                result.add(s);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public void importAbort() {
        mDictDatabase.importAbort();
    }

    public boolean filter(String item) {
        return true;
//        Pattern en = Pattern.compile(enRegEx);
//        Matcher m = en.matcher(item);
//        return m.matches();
    }
}
