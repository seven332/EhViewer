package com.hippo.ehviewer.ehclient;

public abstract class EhParser {
    public Object obj;
    abstract boolean parser(String pageContext);
}
