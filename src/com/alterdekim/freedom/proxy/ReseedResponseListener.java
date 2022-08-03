package com.alterdekim.freedom.proxy;

import org.json.JSONObject;

import java.util.UUID;

public class ReseedResponseListener {

    private String guid;
    private IReseedResponse listener;

    private JSONObject jsonObject;

    public ReseedResponseListener(JSONObject jsonObject, IReseedResponse listener ) {
        this.jsonObject = jsonObject;
        guid = UUID.randomUUID().toString();
        this.listener = listener;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public String getGuid() {
        return guid;
    }

    public IReseedResponse getListener() {
        return listener;
    }
}
