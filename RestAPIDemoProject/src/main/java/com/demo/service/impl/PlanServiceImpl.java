package com.demo.service.impl;

import com.demo.service.PlanService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


@Service
public class PlanServiceImpl implements PlanService {


    
    private Jedis jedisConnection = new Jedis();

    @Override
    public String addPlan(JsonNode planNode) {System.out.println("planNode"+planNode);
        if (planNode.has("userUid")) {
            JSONObject responseMap = new JSONObject();
            Iterator<String> objectIterator = planNode.fieldNames();
            Jedis jedisConnection = new Jedis();
            while (objectIterator.hasNext()) {
                String key = objectIterator.next();
                JsonNode entry = planNode.get(key);
                if (entry instanceof TextNode) {
                    responseMap.put(key, entry);
                } else if (entry instanceof ArrayNode) {
                    JSONArray arrayMap = new JSONArray();
                    Integer numberOfObjects = 0;
                    String objectType = key;
                    for (JsonNode entryInArray : entry) {System.out.println("JsonNode" + entryInArray);
                        numberOfObjects++;
                        try {
                            String uid = processJsonObject(entryInArray);
                            JSONObject toPutInLink = new JSONObject();
                            toPutInLink.put(uid, jedisConnection.get(uid));
                            //objectType = uid.split("__")[0];
                            arrayMap.add(toPutInLink);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Validate.notNull(objectType);
                        responseMap.put(objectType, arrayMap);
                    }
                } else if (entry != null) {
                    try {
                        String uid = processJsonObject(entry);
                        String objectType = uid.split("__")[0];
                        Validate.notEmpty(objectType);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("value", uid);
                        responseMap.put(objectType, jsonObject);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            responseMap.put("_createdOn", getUnixTimestamp());
            TextNode objectType = (TextNode) responseMap.get("objectName");
            String objectTypeToUse = String.valueOf(objectType.asText());
            jedisConnection.incr(objectTypeToUse);
            String uid = objectTypeToUse + "__" + jedisConnection.get(objectTypeToUse);
            responseMap.put("_id", uid);
            try {
                responseMap.put("ETag", calculateETag(responseMap));
            } catch (NoSuchAlgorithmException | ParseException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            jedisConnection.set(uid, responseMap.toJSONString());
            //queueService.sendMessage(responseMap);
            return responseMap.toJSONString();
        }
        return null;
    }

    @Override
    public JSONObject getPlan(String planUid) throws ResourceNotFoundException, ParseException {
        String stringFromDb = jedisConnection.get(planUid);
        JSONObject response = new JSONObject();
        JSONParser parser = new JSONParser();
        if (StringUtils.isNotEmpty(stringFromDb)) {
            try {
                JSONObject planFromDb = (JSONObject) new JSONParser().parse(stringFromDb);
                for (Object entryKey : planFromDb.keySet()) {
                    Object entry = planFromDb.get(entryKey);
                    if (entry instanceof JSONObject) {
                        String objectInfo = (String) ((JSONObject) entry).get("value");
                        String objectType = objectInfo.split("__", 2)[0];
                        JSONObject object = getJSONObjectFromObject(jedisConnection, (JSONObject) entry, parser);
                        response.put(objectType, object);
                    } else if (entry instanceof JSONArray) {
                        String objectType = null;
                        JSONArray arrayEntries = new JSONArray();
                        JSONArray entryArray = (JSONArray) entry;
                        int count = 0;
                        for (Object object : entryArray) {
                            count++;
                            String key = (String) ((JSONObject) object).get(Integer.toString(count));
                            JSONObject arrayEntry = (JSONObject) parser.parse(jedisConnection.get(key));
                            objectType = (String) arrayEntry.get("objectName");
                            arrayEntries.add(arrayEntry);
                        }
                        response.put(objectType, arrayEntries);
                    } else {
                        response.put(entryKey, entry);
                    }
                }
                return response;
            } catch (ParseException e) {
                throw new InternalError("Parsing failed.");
            }
        } else {
            throw new ResourceNotFoundException("Can not locate plan with uid: " + planUid);
        }
    }

    @Override
    public JSONObject addBenefitToPlan(String benefitObject, String planUid) throws ResourceNotFoundException, ParseException {
        String stringFromDb = jedisConnection.get(planUid);
        if (StringUtils.isNotBlank(stringFromDb)) {
            JSONParser parser = new JSONParser();
            JSONObject plan = (JSONObject) parser.parse(stringFromDb);
            System.out.println(plan + "vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
            JSONArray benefits = (JSONArray) plan.get("benefit");

            plan.remove("benefit");
            plan.remove("ETag");

            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode jsonNode = mapper.readTree(benefitObject);
                String benefitUid = processJsonObject(jsonNode);
                JSONObject toPutInArray = new JSONObject();
                toPutInArray.put((benefits.size() + 1), benefitUid);

                benefits.add(toPutInArray);

            } catch (IOException e) {
                e.printStackTrace();
            }

            plan.put("benefit", benefits);
            plan.put("_modifiedOn", getUnixTimestamp());
            try {
                plan.put("ETag", calculateETag(plan));
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            jedisConnection.set((String) plan.get("_id"), plan.toJSONString());
           String s = jedisConnection.get(planUid);
           plan = (JSONObject) parser.parse(s);
           // _queueService.sendMessage(plan);
            return plan;
        } else {
            throw new ResourceNotFoundException("Can not locate plan with uid: " + planUid);
        }
    }

    @Override
    public JSONObject patchBenefitOfThePlan(String planKey, JSONObject benefitObject, JSONObject dataToBePatched) throws ParseException {
        System.out.println(benefitObject.toJSONString());
        for (Object key : dataToBePatched.keySet()) {
            Object entry = dataToBePatched.get(key);
            if (benefitObject.containsKey(key)) {
                if (entry instanceof String) {
                    benefitObject.replace(key, entry);
                }
            } else {
                benefitObject.put(key, entry);
            }
        }
        benefitObject.put("_modifiedOn", getUnixTimestamp());
        String benefitUid = (String) benefitObject.get("_id");
        jedisConnection.set(benefitUid, benefitObject.toJSONString());
        //_queueService.sendMessage(benefitObject);

        String planString = jedisConnection.get(planKey);
        JSONObject planObject = (JSONObject) new JSONParser().parse(planString);

        planObject.put("_modifiedOn", getUnixTimestamp());
        try {
            planObject.replace("ETag", calculateETag(planObject));
            String planUid = (String) planObject.get("_id");
            jedisConnection.set(planUid, planObject.toJSONString());
            //_queueService.sendMessage(planObject);
        } catch (NoSuchAlgorithmException | ParseException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return planObject;
    }

    @Override
    public Boolean deletePlan(String planKey) throws ResourceNotFoundException {
        String planString = jedisConnection.get(planKey);
        if (StringUtils.isNotBlank(planString)) {
            JSONParser parser = new JSONParser();
            try {
                JSONObject plan = (JSONObject) parser.parse(planString);
                List<JSONObject> benefits = (ArrayList<JSONObject>) plan.get("benefit");
                int count = 0;
                for (JSONObject benefit : benefits) {
                    count++;
                    String benefitkey = (String) benefit.get(Integer.toString(count));
                    jedisConnection.del(benefitkey);
                }

                System.out.println("Deleting Plan: " + planKey);
                Long del = jedisConnection.del(planKey);
                if (del > 0) {
                    return Boolean.TRUE;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            throw new ResourceNotFoundException("Can not locate plan");
        }
        return Boolean.FALSE;
    }

    private String processJsonObject(JsonNode incomingNode) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject toPersist = (JSONObject) parser.parse(incomingNode.toString());
        String uid = getUuidForObject(toPersist);
        String createdOn = getUnixTimestamp();
        toPersist.put("_id", uid);
        toPersist.put("_createdOn", createdOn);
        jedisConnection.set(uid, toPersist.toJSONString());
       // _queueService.sendMessage(toPersist);
        return uid;
    }

    private String getUuidForObject(JSONObject toPersist) {
        String objectType = (String) toPersist.get("objectName");
        jedisConnection.incr(objectType);
        return objectType + "__" + jedisConnection.get(objectType);
    }

    private String getUnixTimestamp() {
        Long unixDate = new Date().getTime() / 1000;
        return unixDate.toString();
    }

    private String calculateETag(JSONObject object) throws NoSuchAlgorithmException,
            UnsupportedEncodingException,
            ParseException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] bytesOfMessage = object.toJSONString().getBytes("UTF-8");
        byte[] theDigest = messageDigest.digest(bytesOfMessage);
        return theDigest.toString();
    }

    private JSONObject getJSONObjectFromObject(Jedis jedis, JSONObject entry, JSONParser parser) throws ParseException {
        String objectInfo = (String) entry.get("value");
        String objectString = jedis.get(objectInfo);
        JSONObject objectMap = (JSONObject) parser.parse(objectString);
        return objectMap;
    }
}
