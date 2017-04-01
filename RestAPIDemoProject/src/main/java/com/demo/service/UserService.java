package com.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;


@Service
public interface UserService {
    /**
     * @param userObject
     * @return
     */
    String addUser(JSONObject userObject) throws JsonProcessingException, ParseException;

    /**
     * @param userPath
     * @return
     */
    String getUser(String userPath) throws ParseException;

    /**
     * @param userPath
     * @param paramterName
     * @param parameterValue
     * @return
     */
    String updateUser(String userPath, String paramterName, String parameterValue);

    /**
     * @param userObject
     * @return
     */
    String newAddUser(JSONObject userObject);

    /**
     * @param pathToObject
     * @return
     */
    JSONObject newGetUser(String pathToObject) throws ResourceNotFoundException;

    /**
     * @param userUid
     * @param parameterName
     * @param parameterKey
     * @param parameterValue
     * @return
     */
    Boolean newUpdateUser(String userUid, String parameterName, String parameterKey, String parameterValue) throws ParseException, UnsupportedEncodingException, NoSuchAlgorithmException;

    /**
     * @param userUid
     * @return
     */
    Boolean deleteUser(String userUid);

    /**
     *
     * @param userUid
     * @param tokenObject
     * @return
     */
    Boolean addTokenToUser(String userUid, JSONObject tokenObject);
}
