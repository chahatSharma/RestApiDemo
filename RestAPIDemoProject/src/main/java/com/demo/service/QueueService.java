package com.demo.service;

import org.json.simple.JSONObject;

/**
 * Created by ajinkya on 11/15/16.
 */
public interface QueueService {

    void sendMessage(String message);

    void sendMessage(JSONObject jsonObject);

    String receiveMessage(String data);
}
