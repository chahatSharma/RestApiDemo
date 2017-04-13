package com.demo.service.impl;


import com.demo.service.TokenService;
import com.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Configurable("jedisConfiguration")
public class UserServiceImpl
        implements UserService {

    @Autowired
    TokenService tokenService;

    

    private String PERSON_COUNT = "PERSON_COUNT";
    private String USER_COUNT = "USER_COUNT";

    private Log log = LogFactory.getLog(UserServiceImpl.class);

    @Override
    public String addUser(JSONObject userObject) throws JsonProcessingException, ParseException {
        Jedis jedis = new Jedis("localhost");

        jedis.incr(PERSON_COUNT);
        jedis.incr(USER_COUNT);

        JSONObject personObject = (JSONObject) userObject.get("person");
        jedis.del("person");
        processKeys(userObject, jedis, personObject);
        userObject.put("person", personObject);

        JSONObject token = processAndAddToken(userObject, "tokens", (String) userObject.get("role"),
                (String) userObject.get("userUid"), null);

        jedis.set((String) userObject.get("userUid"), userObject.toJSONString());
        jedis.close();

        JSONObject response = new JSONObject();
        response.put("userUid", userObject.get("userUid"));
        response.put("personUid", personObject.get("personUid"));
        response.put("Authorization Header", token.get("tokenUid"));

        return response.toJSONString();
    }

    private JSONObject processAndAddToken(JSONObject userObject,
                                          String tokenName,
                                          String role,
                                          String uid,
                                          JSONObject responseObject)
            throws JsonProcessingException, ParseException {System.out.println("inside processtoken");
        JSONObject token = tokenService.createAccessToken(uid, role, "ACCESS_TOKEN");

        JSONArray tokens = (JSONArray) userObject.get(tokenName);
        if (tokens == null) {
            tokens = new JSONArray();
        }
        JSONObject tokenObject = new JSONObject();
        tokenObject.put(tokens.size() + 1, token.get("tokenId"));
        tokens.add(tokenObject);
        if (responseObject != null) {
            responseObject.put("Authorization", token.get("tokenUid"));
        }
        userObject.put("token", tokens);
        return token;
    }

    @Override
    public String getUser(String userPath) throws ParseException {
        Jedis jedis = new Jedis("localhost");
        String result = jedis.get(userPath);
        JSONObject object = new JSONObject((JSONObject) new JSONParser().parse(result));
        jedis.close();
        return object.toJSONString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String updateUser(String userPath, String parameterName, String parameterValue) {
        try (Jedis jedis = new Jedis("localhost")) {
            JSONObject user = null;
            user = new JSONObject((JSONObject) new JSONParser().parse(jedis.get(userPath)));
            if (user != null) {
                Object object = user.get(parameterName);
                if (object instanceof List) {
                    JSONObject parameterObject = (JSONObject) new JSONParser().parse(parameterValue);
                    JSONArray objectArray = (JSONArray) object;
                    parameterObject.put("createdOn", getUnixTimestamp());
                    objectArray.add(parameterObject);
                } else if (object instanceof JSONObject) {
                    JSONObject parameterObject = (JSONObject) new JSONParser().parse(parameterValue);
                    parameterObject.put("modifiedOn", getUnixTimestamp());
                    user.put(parameterName, parameterObject);
                } else if (object instanceof String) {
                    user.put(parameterName, parameterValue);
                }
                Long del = jedis.del(userPath);
                if (del == 1) {
                    jedis.set(userPath, user.toString());
                    JSONObject response = new JSONObject();
                    response.put("userUid", user.get("userUid"));
                    JSONObject personObj = (JSONObject) user.get("person");
                    response.put("personUid", personObj.get("personUid"));

                    return response.toJSONString();
                }
            }
        } catch (ParseException e) {
            throw new InternalServerErrorException("Failed while patching user");
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String newAddUser(JSONObject body) {
        Jedis jedis = new Jedis("localhost");
        JSONObject responseObject = new JSONObject();
        try {
            Map<String, Object> bodyObj = (HashMap<String, Object>) body;
            JSONObject userObject = new JSONObject();

            String objectType;
            String uid;
            String role = null;
            // Create initial data for personObject
            processInitialData(jedis, bodyObj, userObject, responseObject);

            for (String propertyKey : bodyObj.keySet()) {
                Object property = bodyObj.get(propertyKey);
                System.out.println("propertyKey" + propertyKey);
                
                if (property instanceof JSONArray) { 
                    JSONArray propertyArray = (JSONArray) property;
                    objectType = null;
                    JSONArray objectKeys = new JSONArray();
                    int count = 0;
                    for (Object object : propertyArray) {
                        objectType = (String) ((JSONObject) object).get("_type");
                       
                        uid = processAndGetUid(jedis, objectType, (JSONObject) object);
                        //Add to Jedis
                        jedis.set(uid, ((JSONObject) object).toJSONString());
                        //_queueService.sendMessage((JSONObject) object);
                        // This is done to create link
                        JSONObject toPutInLink = new JSONObject();
                        toPutInLink.put(count++, uid);
                        objectKeys.add(toPutInLink);
                    }
                    userObject.put(objectType, objectKeys);
                    responseObject.put(objectType, objectKeys);
                } else if (property instanceof JSONObject) {System.out.println("property" + property);
                    objectType = (String) ((JSONObject) property).get("_type");
System.out.println("objectType" + objectType);
                    if (objectType.equals("userRole")) {
                        role = (String) ((JSONObject) property).get("roleName");
                    }

                    uid = processAndGetUid(jedis, objectType, (JSONObject) property);
                    jedis.set(uid, ((JSONObject) property).toJSONString());
                    //_queueService.sendMessage((JSONObject) property);
                    // Creating link over here
                    JSONObject jsonObject = new JSONObject();
                    // jsonObject.put("objectType", objectType);
                    // jsonObject.put("objectValue", uid);
                    jsonObject.put("value", uid);
                    userObject.put(objectType, jsonObject);
                    responseObject.put(objectType, jsonObject);
                } else {
                    userObject.put(propertyKey, property);
                }
            }
System.out.println("role" + role);
            if (role != null) {
                processAndAddToken(userObject, "token", role, (String) userObject.get("_id"), responseObject);
            }
            userObject.put("eTag", calculateETag(userObject));
            responseObject.put("eTag", userObject.get("eTag"));
            jedis.set((String) responseObject.get(bodyObj.get("_type")), userObject.toJSONString());
            //_queueService.sendMessage(userObject);
            return responseObject.toJSONString();

        } catch (ParseException | UnsupportedEncodingException | NoSuchAlgorithmException | JsonProcessingException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return null;
    }

    private String calculateETag(JSONObject object) throws NoSuchAlgorithmException,
            UnsupportedEncodingException,
            ParseException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] bytesOfMessage = object.toJSONString().getBytes("UTF-8");
        byte[] thedigest = messageDigest.digest(bytesOfMessage);
        return thedigest.toString();
    }

    @SuppressWarnings("unchecked")
    private String processAndGetUid(Jedis jedis, String objectType, JSONObject object) {
        String uid;
        jedis.incr(objectType);
        uid = objectType + "__" + jedis.get(objectType);
        object.put("_id", uid);
        object.put("_createdOn", getUnixTimestamp());
        return uid;
    }

    @SuppressWarnings("unchecked")
    private void processInitialData(Jedis jedis,
                                    Map<String, Object> bodyObj,
                                    JSONObject userObject,
                                    JSONObject responseObject) {
        String objectType = (String) bodyObj.get("_type");
        jedis.incr(objectType);
        String uid = objectType + "__" + jedis.get(objectType);
        userObject.put("_id", uid);
        userObject.put("_createdOn", getUnixTimestamp());
        responseObject.put(objectType, uid);
    }


    @SuppressWarnings("unchecked")
    private void processKeys(JSONObject userObject, Jedis jedis, JSONObject personObject) {
        String personUid = "person" + "__" + personObject.get("firstName") + "__" + jedis.get(PERSON_COUNT);
        String userUid = "user" + "__" + userObject.get("userName") + "__" + jedis.get(USER_COUNT);

        userObject.put("userUid", userUid);
        personObject.put("personUid", personUid);

        userObject.put("createdOn", getUnixTimestamp());
        personObject.put("createdOn", getUnixTimestamp());
    }


    @SuppressWarnings("unchecked")
    @Override
    public JSONObject newGetUser(String pathToObject) throws ResourceNotFoundException {
        Jedis jedis = new Jedis("localhost");
        JSONObject response = new JSONObject();
        JSONParser parser = new JSONParser();

        Assert.assertNotNull(pathToObject);
        try {
            String res = jedis.get(pathToObject);
            if (!StringUtils.isBlank(res)) {
                JSONObject resultObject = (JSONObject) new JSONParser().parse(res);
                if (resultObject != null) {
                    for (Object entryKey : resultObject.keySet()) {
                        Object entry = resultObject.get(entryKey);
                        if (entry instanceof JSONObject) {
                            String objectInfo = (String) ((JSONObject) entry).get("value");
                            String objectType = objectInfo.split("__", 2)[0];
                            JSONObject object = getJSONObjectFromObject(jedis, (JSONObject) entry, parser);
                            response.put(objectType, object);
                        } else if (entry instanceof JSONArray) {
                            String objectType = null;
                            JSONArray arrayEntries = new JSONArray();
                            JSONArray entryArray = (JSONArray) entry;
                            int count = 0;
                            for (Object object : entryArray) {
                                count++;
                                String key = (String) ((JSONObject) object).get(Integer.toString(count));
                                JSONObject arrayEntry = (JSONObject) parser.parse(jedis.get(key));
                                objectType = (String) arrayEntry.get("_type");
                                arrayEntries.add(arrayEntry);
                            }
                            response.put(objectType, arrayEntries);
                        } else {
                            response.put(entryKey, entry);
                        }
                    }
                    return response;
                }
            } else {
                throw new ResourceNotFoundException("Invalid object key");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Boolean newUpdateUser(String userUid,
                                 String parameterName,
                                 String parameterKey,
                                 String parameterValue) throws ResourceNotFoundException, ParseException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Validate.notEmpty(userUid, "User id can not blank");
        JSONObject userMetaData = getObjectMetaData(userUid);
        if (userMetaData != null) {
            log.info("Requested Found metadata: " + userMetaData.toJSONString());
            if (isKeyMatched(userUid.split("__")[1], parameterKey.split("__")[1])) {
                JSONObject parentObjectToBeChanged = newGetUser(parameterKey);
                Validate.notNull(parentObjectToBeChanged);
                if (parentObjectToBeChanged.containsKey(parameterName)) {
                    Object objectToBeChanged = parentObjectToBeChanged.get(parameterName);
                    if (objectToBeChanged instanceof String) {
                        Jedis jedis = new Jedis("localhost");
                        if (!parentObjectToBeChanged.get("_id").equals(userMetaData.get("_id"))) {
                            parentObjectToBeChanged.remove(parameterName);
                            parentObjectToBeChanged.put(parameterName, parameterValue);
                            parentObjectToBeChanged.put("_modifiedOn", getUnixTimestamp());
                            parentObjectToBeChanged.put("eTag", calculateETag(parentObjectToBeChanged));
                            jedis.set((String) parentObjectToBeChanged.get("_id"), parentObjectToBeChanged.toJSONString());
                            userMetaData.put("eTag", calculateETag(userMetaData));
                            jedis.set(userUid, userMetaData.toJSONString());
                        } else {
                            userMetaData.remove(parameterName);
                            userMetaData.put(parameterName, parameterValue);
                            userMetaData.put("_modifiedOn", getUnixTimestamp());
                            userMetaData.put("eTag", calculateETag(userMetaData));
                            jedis.set(userUid, userMetaData.toJSONString());
                        }

                        jedis.close();
                        return Boolean.TRUE;
                    }
                    if (objectToBeChanged instanceof JSONObject) {
                        log.info("Need to replace object");
                    }
                } else {
                    JSONObject requestObject = (JSONObject) new JSONParser().parse(parameterValue);
                    JSONObject objectToBeChanged = newGetUser(parameterKey);
                    Assert.assertNotNull(requestObject);
                    Assert.assertNotNull(objectToBeChanged);
                    if (requestObject.get("_type").equals(objectToBeChanged.get("_type"))) {
                        Jedis jedis = new Jedis("localhost");
                        String createdOn = (String) objectToBeChanged.get("_createdOn");
                        String uid = (String) objectToBeChanged.get("_id");
                        requestObject.put("_id", uid);
                        requestObject.put("_createdOn", createdOn);
                        requestObject.put("_modifiedOn", getUnixTimestamp());
                        requestObject.put("eTag", calculateETag(requestObject));
                        userMetaData.put("eTag", calculateETag(userMetaData));
                        jedis.set(userUid, userMetaData.toJSONString());
                        jedis.set(uid, requestObject.toJSONString());
                        return Boolean.TRUE;
                    } else {
                        log.error("Can not change object. Internal Server Error");
                    }
                }
            } else {
                throw new BadRequestException("Can not modify objects from other entity");
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean deleteUser(String userUid) {
        Jedis jedis = new Jedis("localhost");
        try {
            JSONObject userMetaData = (JSONObject) new JSONParser().parse(jedis.get(userUid));
            long result = deleteUser(userMetaData, jedis);
            if (result > 0) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } catch (ParseException e) {
            log.error("Failed while parsing. Exception: " + e);
        } finally {
            jedis.close();
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean addTokenToUser(String userUid, JSONObject tokenObject) {
        Jedis jedis = new Jedis("localhost");
        try {
            String tokenUid = (String) tokenObject.get("tokenId");
            Validate.notEmpty(tokenUid);
            JSONObject userMetaData = (JSONObject) new JSONParser().parse(jedis.get(userUid));
            JSONArray tokens = (JSONArray) userMetaData.get("token");

            JSONObject toPutInArray = new JSONObject();
            toPutInArray.put((tokens.size() + 1), tokenObject.get("tokenId"));
            tokens.add(toPutInArray);

            userMetaData.replace("eTag", calculateETag(userMetaData));
            userMetaData.put("_modifiedOn", getUnixTimestamp());

            jedis.set(userUid, userMetaData.toJSONString());
            //_queueService.sendMessage(userMetaData);
            return Boolean.TRUE;

        } catch (ParseException e) {
            log.error("Failed while parsing. Exception: " + e);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return Boolean.FALSE;
    }

    private long deleteUser(JSONObject object, Jedis jedis) {
        long result = 0;
        if (object != null) {
            for (Object entryKey : object.keySet()) {
                Object entry = object.get(entryKey);
                {
                    if (entry instanceof JSONObject) {
                        long res = jedis.del((String) ((JSONObject) entry).get("objectValue"));
                        result = result + res;
                    } else if (entry instanceof JSONArray) {
                        JSONArray entryArray = (JSONArray) entry;
                        for (Object place : entryArray) {
                            long res = jedis.del((String) place);
                            result = result + res;
                        }
                    }
                }
            }
        }
        long res = jedis.del((String) object.get("_id"));
        result = result + res;
        log.info("Deleted " + String.valueOf(result) + " objects");
        return result;
    }

    private Boolean isKeyMatched(String s, String s1) {
        String userKey = s;
        String patchkey = s1;
        if (StringUtils.equals(userKey, patchkey)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    private JSONObject getObjectMetaData(String userUid) {
        Jedis jedis = new Jedis("localhost");
        try {
            String userMetaDataString = jedis.get(userUid);
            if (!StringUtils.isBlank(userMetaDataString)) {
                JSONObject userMetaData = (JSONObject) new JSONParser().parse(userMetaDataString);
                return userMetaData;
            } else {
                throw new ResourceNotFoundException("User Not Found in Database. Requested Resource: " + userUid);
            }
        } catch (ParseException e) {
            log.error("Failed while Parsing. Exception: " + e);
        } finally {
            jedis.close();
        }
        return null;
    }

    private JSONObject getJSONObjectFromObject(Jedis jedis, JSONObject entry, JSONParser parser) throws ParseException {
        String objectInfo = (String) entry.get("value");
        String objectString = jedis.get(objectInfo);
        JSONObject objectMap = (JSONObject) parser.parse(objectString);
        return objectMap;
    }

    private String getUnixTimestamp() {
        Long unixDate = new Date().getTime() / 1000;
        String unixDateString = unixDate.toString();
        return unixDateString;
    }
}
