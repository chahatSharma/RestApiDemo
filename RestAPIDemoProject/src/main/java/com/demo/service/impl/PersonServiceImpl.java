package com.demo.service.impl;

import com.demo.pojo.AccessToken;
import com.demo.pojo.User;
import com.demo.service.PersonService;

import com.demo.service.TokenService;
import com.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PersonServiceImpl implements PersonService {

	private String PERSON_COUNT = "PERSON_COUNT";

	private String USER_COUNT = "USER_COUNT";

	@Autowired
	TokenService tokenService;

	@Autowired
	UserService userService;

	Jedis jedisConnectiion = new Jedis("localhost");

	@Override
	public String processAndAddPerson(String personData) {

		Jedis jedis = new Jedis("localhost");

		User user = processUser(personData);

		user = processKeys(jedis, user);

		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> responseMap;
		String response = null;
		try {
			responseMap = new HashMap<>();
			responseMap.put("userUUID", user.getUserUid());
			responseMap.put("personUUID", user.getPerson().getPersonUid());

			// Create a token for user
			AccessToken token = tokenService.createAccessTokenAPI(user.getUserUid(), user.getRole(), "ACCESS_TOKEN");
			if (token != null) {
				user.getTokens().add(token);
				responseMap.put("tokenAUTH", token.getTokenUid());
			}
			jedis.set(user.getPerson().getPersonUid(), mapper.writeValueAsString(user.getPerson()));
			jedis.set(user.getUserUid(), mapper.writeValueAsString(user));
			response = mapper.writeValueAsString(responseMap);

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		jedis.close();
		return response;
	}

	@Override
	public String getPerson(String personUID) {
		Jedis jedis = new Jedis("localhost");
		if (personUID != null && !(personUID.isEmpty())) {
			return jedis.get(personUID);
		} else
			return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String v1AddPerson(String personBody) {
		Jedis jedis = new Jedis("localhost");
		JSONParser parser = new JSONParser();
		JSONObject responseObject = new JSONObject();
		try {
			Map<String, Object> bodyObj = (HashMap<String, Object>) parser.parse(personBody);
			JSONObject personObject = new JSONObject();

			String objectType = null;
			String uid = null;
			// Create initial data for personObject
			processInitialData(jedis, bodyObj, personObject, responseObject);

			for (String propertyKey : bodyObj.keySet()) {
				Object property = bodyObj.get(propertyKey);
				if (property instanceof JSONArray) {
					System.out.println("inside if");
					JSONArray propertyArray = (JSONArray) property;
					objectType = null;
					JSONArray objectKeys = new JSONArray();
					int count = 0;
					for (Object object : propertyArray) {
						objectType = (String) ((JSONObject) object).get("_type");
						jedis.incr(objectType);
						uid = objectType + "__" + jedis.get(objectType);
						((JSONObject) object).put("_createdOn", getUnixTimestamp());
						((JSONObject) object).put("_id", uid);

						// Add to Jedis
						jedis.set(uid, ((JSONObject) object).toJSONString());
						// Send object to elasticsearch
						// queueService.sendMessage((JSONObject) object);
						// This is done to create link
						count++;
						JSONObject toPutInLink = new JSONObject();
						toPutInLink.put(propertyKey+"__"+count, uid);
						objectKeys.add(toPutInLink);
					}
					personObject.put(objectType, objectKeys);
					responseObject.put(objectType, objectKeys);
				} else if (property instanceof JSONObject) {
					System.out.println("Inside else");
					JSONObject jsonObject = new JSONObject();
					objectType = (String) ((JSONObject) property).get("_type");
					// jsonObject.put("objectType", objectType);
					if (objectType.equals("user")) {
						String userString = userService.newAddUser((JSONObject) property);
						System.out.println(userString);
						JSONObject userObject = (JSONObject) parser.parse(userString);
						jsonObject.put("objectValue", userObject.get("user"));
						jsonObject.put("value", userObject.get("user"));
						responseObject.put("Authorization", userObject.get("Authorization"));
						responseObject.put(objectType, userObject.get("user"));
						personObject.put(objectType, jsonObject);
					} else {
						jedis.incr(objectType);
						uid = objectType + "__" + jedis.get(objectType);
						((JSONObject) property).put("_createdOn", getUnixTimestamp());
						((JSONObject) property).put("_id", uid);
						jedis.set(uid, ((JSONObject) property).toJSONString());
						// Send object to elasticsearch
						// queueService.sendMessage((JSONObject) property);
						// Creating link over here
						jsonObject.put("objectValue", uid);
						personObject.put(objectType, jsonObject);
						responseObject.put(objectType, jsonObject);
					}
				} else {
					personObject.put(propertyKey, property);
				}
				responseObject.put("ETag", calculateETag(personObject));
			}
			// Metadata is stored jedis.
			jedis.set((String) responseObject.get(bodyObj.get("_type")), personObject.toString());
			// Send Metadata to elasticsearch
			// queueService.sendMessage(personObject);
			System.out.println("response" + responseObject.toJSONString());
			return responseObject.toJSONString();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			jedis.close();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String v1GetPerson(String personUid) throws ResourceNotFoundException {
		Jedis jedis = new Jedis("localhost");
		JSONObject response = new JSONObject();
		JSONParser parser = new JSONParser();
		try {
			JSONObject resultObject = (JSONObject) new JSONParser().parse(jedis.get(personUid));
			if (resultObject != null) {
				for (Object entryKey : resultObject.keySet()) {
					Object entry = resultObject.get(entryKey);
					if (entry instanceof JSONObject) {
						String objectInfo = (String) ((JSONObject) entry).get("value");
						String objectType = objectInfo.split("__", 2)[0];
						if (objectType.equals("user")) {
							JSONObject jsonObject = userService.newGetUser(objectInfo);
							response.put(objectType, jsonObject);
						} else {
							JSONObject object = getJSONObjectFromObject(jedis, (JSONObject) entry, parser);
							response.put(objectType, object);
						}
					} else if (entry instanceof JSONArray) {
						JSONArray arrayEntries = new JSONArray();
						JSONArray entryArray = (JSONArray) entry;
						String objectType = null;
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
				}System.out.println(response);
				return response.toJSONString();
			}
		} catch (NullPointerException e) {
			throw new ResourceNotFoundException("Can not find person with UID");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public JSONObject newUpdatePerson(String personId, String parameterName, String parameterKey, String parameterValue)
			throws IOException {
		String personString = v1GetPerson(personId);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode personNode = mapper.readTree(personString);
		Assert.assertNotNull(personNode);

		JsonNode parentNode = personNode.findParent(parameterName);
		System.out.println("parentNode"+parentNode);
		Validate.notNull(parentNode);

		String parameterValueTemp = mapper.writeValueAsString(parameterValue);
		try {
			Object parameterValueObject = new JSONParser().parse(parameterValueTemp);
			if (parameterValueObject instanceof String) {
				System.out.println("String found");
				JsonNode id = parentNode.get("_id");
				Assert.assertNotNull(id);
				String objectToBeUpdated = jedisConnectiion.get(id.asText());
				System.out.println("objectToBeUpdated" + objectToBeUpdated);

				if (!StringUtils.isBlank(objectToBeUpdated)) {
					JSONObject toChange = (JSONObject) new JSONParser().parse(objectToBeUpdated);
					System.out.println("toChange" + toChange.toJSONString());
					System.out.println("parameterName" + parameterValue.trim());
					toChange.remove(parameterName);
					toChange.put(parameterName, parameterValue.trim());
					toChange.put("_modifiedOn", getUnixTimestamp());
					
					System.out.println(toChange.toJSONString());
					jedisConnectiion.set(id.asText(), toChange.toJSONString());

					JSONObject personObject = (JSONObject) new JSONParser().parse(jedisConnectiion.get(personId));
					Assert.assertNotNull(personObject);
					personObject.put("_modifiedOn", getUnixTimestamp());
					personObject.put("ETag", calculateETag(personObject));
					jedisConnectiion.set((String) personObject.get("_id"), personObject.toJSONString());
					//responseObject.put("ETag", calculateETag(personObject));
					return personObject;
				}
			} else if (parameterValueObject instanceof JSONObject) {
				System.out.println("Object Found");
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private JSONObject getJSONObjectFromObject(Jedis jedis, JSONObject entry, JSONParser parser) throws ParseException {
		String key = (String) entry.get("value");
		String objectString = jedis.get(key);
		JSONObject objectMap = (JSONObject) parser.parse(objectString);
		return objectMap;
	}

	private void processInitialData(Jedis jedis, Map<String, Object> bodyObj, JSONObject personObject,
			JSONObject responseObject) {
		String objectType = (String) bodyObj.get("_type");
		jedis.incr(objectType);
		String uid = objectType + "__" + jedis.get(objectType);
		personObject.put("_createdOn", getUnixTimestamp());
		personObject.put("_id", uid);
		responseObject.put(objectType, uid);
	}

	private User processKeys(Jedis jedis, User user) {
		jedis.incr(PERSON_COUNT);
		jedis.incr(USER_COUNT);

		String personUid = "person" + "__" + user.getPerson().getFirstName() + "__" + jedis.get(PERSON_COUNT);
		String userUid = "user" + "__" + user.getUserName() + "__" + jedis.get(USER_COUNT);

		user.setUserUid(userUid);
		user.getPerson().setPersonUid(personUid);
		return user;
	}

	private User processUser(String personData) {
		JSONParser parser = new JSONParser();
		try {
			JSONObject personEntity = (JSONObject) parser.parse(personData);

			User user = new User((String) personEntity.get("username"), (String) personEntity.get("password"),
					(String) personEntity.get("firstName"), (String) personEntity.get("lastName"),
					(String) personEntity.get("email"));
			return user;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getUnixTimestamp() {
		Long unixDate = new Date().getTime() / 1000;
		String unixDateString = unixDate.toString();
		return unixDateString;
	}

	private String calculateETag(JSONObject object)
			throws NoSuchAlgorithmException, UnsupportedEncodingException, ParseException {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		byte[] bytesOfMessage = object.toJSONString().getBytes("UTF-8");
		byte[] theDigest = messageDigest.digest(bytesOfMessage);
		return theDigest.toString();
	}
}
