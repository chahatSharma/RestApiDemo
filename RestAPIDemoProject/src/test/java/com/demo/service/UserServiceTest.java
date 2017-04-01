package com.demo.service;

import com.demo.service.impl.UserServiceImpl;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ajinkya on 10/18/16.
 */
public class UserServiceTest {


    UserServiceImpl userService;

    public static final String SAMPLE_VALID_USER_BODY = "{\n" +
            "  \"objectName\": \"user\",\n" +
            "  \"password\": \"labore ut\",\n" +
            "  \"userName\": \"cillum sed\",\n" +
            "  \"role\": {\n" +
            "  \t\"objectName\": \"role\",\n" +
            "    \"roleId\": \"dolor mollit labore\"\n" +
            "  },\n" +
            "  \"tokens\": [\n" +
            "    {\n" +
            "    \t\"objectName\": \"token\",\n" +
            "      \"createdOn\": \"amet\",\n" +
            "      \"issuer\": \"\",\n" +
            "      \"role\": \"culpa non magna amet\",\n" +
            "      \"accessUrl\": \"do\",\n" +
            "      \"tokenId\": \"nisi quis ut\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"_id\": \"dolor\",\n" +
            "  \"createdOn\": \"ullamco in irure\"\n" +
            "}";

    public static final String SAMPLE_USER_2 = "{\n" +
            "\t\"objectName\": \"user\",\n" +
            "\t\"username\": \"aditya\",\n" +
            "\t\"password\": \"admin\",\n" +
            "\t\"role\":{\n" +
            "\t\t\"objectName\": \"role\",\n" +
            "\t\t\"roleId\": \"1\",\n" +
            "\t\t\"roleName\": \"admin\"\n" +
            "\t},\n" +
            "\t\"_id\":\"786\"\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        userService = new UserServiceImpl();
    }

    @Test
    public void testAddUser() throws Exception {

    }

    @Test
    public void testGetUser() throws Exception {
        JSONObject object = userService.newGetUser("user__3");
        Assert.assertNotNull(object);
    }

    @Test
    public void testNewAddUser() throws Exception {
        JSONObject object = getSampleUserObject(SAMPLE_USER_2);
        String s = userService.newAddUser(object);
        JSONObject responseObject = (JSONObject) new JSONParser().parse(s);
        String userKey = (String) responseObject.get("user");
        responseObject = userService.newGetUser(userKey);
        System.out.println(responseObject.toJSONString());
    }

    @Test
    public void testNewUpdateRoleName() throws ParseException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String userUid = "user__1";
        String parameterName = "roleName";
        String parameterKey = "role__1";
        String parameterValue = "READ_ONLY";
        Boolean result = userService.newUpdateUser(userUid, parameterName, parameterKey, parameterValue);
        Assert.assertTrue(result);
    }

    @Test
    public void testNewUpdateRole() throws ParseException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String parameterValue = "{\n" +
                "    \"_createdOn\": \"1478272985\",\n" +
                "    \"roleId\": \"1\",\n" +
                "    \"roleName\": \"read__only\",\n" +
                "    \"objectName\": \"role\",\n" +
                "    \"_id\": \"role__1\"\n" +
                "  }";
        String userUid = "user__1";
        String parameterName = "role";
        String parameterKey = "role__1";
        Boolean result = userService.newUpdateUser(userUid, parameterName, parameterKey, parameterValue);
        Assert.assertTrue(result);
    }

    @Test
    public void testDeleteUser() throws ParseException {
        JSONObject sampleUser = (JSONObject) new JSONParser().parse(SAMPLE_USER_2);
        String result = userService.newAddUser(sampleUser);
        Assert.assertNotNull(result);

        JSONObject resultObject = (JSONObject) new JSONParser().parse(result);

        String userUid = (String) resultObject.get("_id");
        Boolean resultOfDelete = userService.deleteUser(userUid);
        Assert.assertTrue(resultOfDelete);
    }

    private JSONObject getSampleUserObject(String userBody) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(userBody);
    }


}