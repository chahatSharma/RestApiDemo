package com.demo.service.impl;

import com.demo.service.PlanService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.demo.service.QueueService;
import jersey.repackaged.com.google.common.collect.Iterators;

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
import java.util.Set;
import java.util.Map.Entry;

@Service
public class PlanServiceImpl implements PlanService {

	@Autowired
	QueueService _queueService;
	private Jedis jedisConnection = new Jedis();

	@Override
	public String addPlan(JsonNode planNode) {
		// //system.out.println("planNode" + planNode);
		if (planNode.has("userUid")) {
			JSONObject responseMap = new JSONObject();
			JSONObject planJson = new JSONObject();
			Iterator<String> objectIterator = planNode.fieldNames();

			Jedis jedisConnection = new Jedis();
			while (objectIterator.hasNext()) {
				String key = objectIterator.next();

				JsonNode entry = planNode.get(key);
				if (entry instanceof TextNode) {
					// planJson.put(key, )
					responseMap.put(key, entry);
					planJson.put(key, entry);
				} else if (entry instanceof ArrayNode) {
					JSONArray arrayMap = new JSONArray();
					JSONArray planResponseMap = new JSONArray();
					Integer numberOfObjects = 0;
					String objectType = key;
					try {

						int count = 0;
						for (JsonNode entryInArray : entry) {
							JSONObject toPutInLink = new JSONObject();
							JSONObject toPutInResponse = new JSONObject();
							// system.out.println("cccc" + entryInArray);

							// toPutInLink.put(uid, jedisConnection.get(uid));
							// objectType = uid;//.split("__")[0];
							// //system.out.println("obbbb" + objectType);
							/* if (count == 0) { */
							toPutInLink.put("_type", entryInArray.get("_type"));
							toPutInResponse.put("_type", entryInArray.get("_type"));

							// arrayMap.add(toPutInLink);
							/* } */
							// toPutInLink = new JSONObject();
							count = count + 1;
							Iterator<Entry<String, JsonNode>> it = entryInArray.fields();
							while (it.hasNext()) {
								Entry<String, JsonNode> p = it.next();

								Iterator<JsonNode> itt = p.getValue().iterator();
								if (Iterators.size(itt) > 0) {
									String uid = processJsonObject(p.getValue());
									// toPutInLink = new JSONObject();
									JSONParser parser = new JSONParser();
									// system.out.println("p.getValue()"+p.getValue());
									JSONObject jsonObject = (JSONObject) parser.parse(jedisConnection.get(uid));
									toPutInLink.put(p.getKey(), uid);
									toPutInLink.put("_id", uid);

									toPutInResponse.put(p.getKey(), jsonObject);
									toPutInResponse.put("_id", uid);
									// objectType = uid.split("__")[0];

								}

							}
							// arrayMap.add(toPutInLink);
							JsonNode newNode = entryInArray;
							Iterator nodeKeys = newNode.fieldNames();
							ObjectMapper mapper = new ObjectMapper();

							// system.out.println("toPutInLinktoPutInLink"+toPutInLink);
							newNode = mapper.readTree(toPutInLink.toJSONString());

							String mainUid = processJsonObject(newNode);
							toPutInLink.put("_id", mainUid);
							toPutInResponse.put("_id", mainUid);
							arrayMap.add(toPutInLink);
							planResponseMap.add(toPutInResponse);

							// arrayMap.add(entryInArray.get("_type"));
							// arrayMap.add(toPutInLink);

							numberOfObjects++;

							// try {
							// String uid = processJsonObject(entryInArray);
							// JSONObject toPutInLink = new JSONObject();
							// toPutInLink.put(uid, jedisConnection.get(uid));
							// //objectType = uid.split("__")[0];
							// arrayMap.add(toPutInLink);
							// } catch (ParseException e) {
							// e.printStackTrace();
							// }

							// //system.out.println("arrayMaparrayMaparrayMap" +
							// arrayMap);
							Validate.notNull(objectType);
							responseMap.put(objectType, planResponseMap);
							planJson.put(objectType, arrayMap);
							// //system.out.println("response map" +
							// responseMap);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (entry != null) {
					try {
						String uid = processJsonObject(entry);
						planJson.put(key, uid);
						String objectType = uid.split("__")[0];
						Validate.notEmpty(objectType);
						JSONParser parser = new JSONParser();
						// system.out.println("uiduiduiduiduid"+uid);
						JSONObject jsonObject = (JSONObject) parser.parse(jedisConnection.get(uid));

						// jsonObject.put(key, jedisConnection.get(uid));
						responseMap.put(key, jsonObject);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("responseMap in getplan method is" + responseMap);
			responseMap.put("_createdOn", getUnixTimestamp());
			planJson.put("_createdOn", getUnixTimestamp());
			TextNode objectType = (TextNode) responseMap.get("_type");
			String objectTypeToUse = String.valueOf(objectType.asText());
			jedisConnection.incr(objectTypeToUse);
			// system.out.println("jedisConnection.get(objectTypeToUse)"+jedisConnection.get(objectTypeToUse));
			String uid = objectTypeToUse + "__" + jedisConnection.get(objectTypeToUse);
			responseMap.put("_id", uid);
			planJson.put("_id", uid);
			try {
				responseMap.put("ETag", calculateETag(responseMap));
				planJson.put("ETag", calculateETag(responseMap));
			} catch (NoSuchAlgorithmException | ParseException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			// system.out.println("planJson"+planJson);
			jedisConnection.set(uid, planJson.toJSONString());
			/*
			 * JSONObject persistObj = responseMap;
			 * if(persistObj.containsKey("_id")) { persistObj.remove("_id");
			 * 
			 * Set persistKeys = persistObj.keySet(); }
			 */
			_queueService.sendMessage(responseMap);
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
				response = (JSONObject) parser.parse(stringFromDb);
				JSONObject planFromDb = (JSONObject) new JSONParser().parse(stringFromDb);
				/*
				 * for (Object entryKey : planFromDb.keySet()) {
				 * //system.out.println("entrykey" + entryKey); Object entry =
				 * planFromDb.get(entryKey); if (entry instanceof JSONObject) {
				 * String objectInfo = (String) ((JSONObject)
				 * entry).get("value"); String objectType =
				 * objectInfo.split("__", 2)[0]; JSONObject object =
				 * getJSONObjectFromObject(jedisConnection, (JSONObject) entry,
				 * parser); //response.put(objectType, object); } else if (entry
				 * instanceof JSONArray) { String objectType = null; JSONArray
				 * arrayEntries = new JSONArray(); JSONArray entryArray =
				 * (JSONArray) entry; int count = 0; //
				 * //system.out.println("entryArray" + entryArray); for (Object
				 * object : entryArray) { count++;
				 * 
				 * //system.out.println("object in array is" + object); String
				 * key = (String) ((JSONObject)
				 * object).get(Integer.toString(count)); //
				 * //system.out.println("key is iiii" + key); JSONObject
				 * arrayEntry = (JSONObject)
				 * parser.parse(jedisConnection.get(key)); objectType = (String)
				 * arrayEntry.get("_type"); arrayEntries.add(arrayEntry); }
				 * //response.put(objectType, arrayEntries); } else {
				 * //response.put(entryKey, entry); } }
				 */
				return response;
			} catch (ParseException e) {
				throw new InternalError("Parsing failed.");
			}
		} else {
			throw new ResourceNotFoundException("Can not locate plan with uid: " + planUid);
		}
	}

	@Override
	public JSONObject addBenefitToPlan(String benefitObject, String planUid)
			throws ResourceNotFoundException, ParseException {
		String stringFromDb = jedisConnection.get(planUid);
		System.out.println("benefitObject" + stringFromDb);
		if (StringUtils.isNotBlank(stringFromDb)) {
			JSONParser parser = new JSONParser();
			JSONObject plan = (JSONObject) parser.parse(stringFromDb);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode planNode;
			try {
				planNode = mapper.readTree(stringFromDb);
				Iterator<String> itt = planNode.fieldNames();
				// system.out.println("zzz"+Iterators.size(itt));
				/*
				 * while(itt.hasNext()) { //system.out.println("planNodess" +
				 * itt.next()); }
				 */

			} catch (JsonProcessingException e1) {
				// system.out.println(e1);
				e1.printStackTrace();
			} catch (IOException e1) {
				// system.out.println(e1);
				e1.printStackTrace();
			}

			JSONArray benefits = (JSONArray) plan.get("linkedPlanServices");
			// system.out.println(benefits +
			// "vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
			plan.remove("planserviceCostShares");
			plan.remove("ETag");

			try {

				JsonNode jsonNode = mapper.readTree(benefitObject);

				//////////////////////////////////////////////
				JSONObject toPutInLink = new JSONObject();
				Iterator<Entry<String, JsonNode>> it = jsonNode.fields();
				while (it.hasNext()) {
					Entry<String, JsonNode> p = it.next();

					Iterator<JsonNode> itt = p.getValue().iterator();
					if (Iterators.size(itt) > 0) {
						String uid = processJsonObject(p.getValue());
						// toPutInLink = new JSONObject();
						parser = new JSONParser();

						JSONObject jsonObject = (JSONObject) parser.parse(jedisConnection.get(uid));
						// system.out.println("jsonObject" + uid);
						toPutInLink.put(p.getKey(), uid);
						toPutInLink.put("_id", uid);
						// objectType = uid.split("__")[0];

					}

				}
				String benefitUid = processJsonObject(jsonNode);
				// JSONObject toPutInArray = new JSONObject();
				// toPutInArray.add(toPutInLink);
				toPutInLink.put("_id", benefitUid);
				toPutInLink.put("_type", jsonNode.get("_type"));
				// arrayMap.add(toPutInLink);

				//////////////////////////////////////////

				// toPutInArray.put((benefits.size() + 1), benefitUid);

				benefits.add(toPutInLink);

			} catch (IOException e) {
				e.printStackTrace();
			}

			plan.put("linkedPlanServices", benefits);
			plan.put("modifiedOn", getUnixTimestamp());
			try {
				plan.put("ETag", calculateETag(plan));
			} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			JSONObject newPlan = new JSONObject();
			Set planKeys = plan.keySet();
			Iterator it = planKeys.iterator();
			// JSONParser parser = new JSONParser();
			while (it.hasNext()) {
				String key = (String) it.next();

				if (!key.equalsIgnoreCase("_type") && !key.equalsIgnoreCase("_id")) {
					if (plan.get(key) instanceof JSONArray) {
						JSONArray array = (JSONArray) plan.get(key);
						JSONArray newArray = new JSONArray();
						Iterator i = array.iterator();
						while (i.hasNext()) {
							JSONObject arrayObj = (JSONObject) i.next();
							Set arrayObjKeys = arrayObj.keySet();
							Iterator itt = arrayObjKeys.iterator();

							JSONObject newArrayObj = new JSONObject();
							while (itt.hasNext()) {
								String arrayObjKey = (String) itt.next();
								if (!arrayObjKey.equalsIgnoreCase("_id") && !arrayObjKey.equalsIgnoreCase("_type")) {
									String innerObj = jedisConnection.get((String) arrayObj.get(arrayObjKey));
									if (null != innerObj) {
										newArrayObj.put(arrayObjKey, parser.parse(innerObj));
										System.out.println("I love you" + newArrayObj);
										//newArrayObj.put("ParentID", );
									}
								} else {
									newArrayObj.put(arrayObjKey, arrayObj.get(arrayObjKey));
								}
								
							}newArray.add(newArrayObj);
						}
						newPlan.put(key, newArray);
						//newPlan.put("parentId", plan.get);
					} else {
						if (null != jedisConnection.get((String) plan.get(key))) {
							JSONObject outObj = (JSONObject) parser.parse(jedisConnection.get((String) plan.get(key)));
							//outObj.put("ParentID", planUid);
							newPlan.put(key, outObj);
						
						} else {
							newPlan.put(key, plan.get(key));
						}

					}

				}
				else{
					newPlan.put(key, plan.get(key));
				}
			}

			System.out.println("before persisting plan"+plan);
			jedisConnection.set((String) plan.get("_id"), plan.toJSONString());
			//String s = jedisConnection.get(planUid);
			//plan = (JSONObject) parser.parse(s);
			_queueService.sendMessage(newPlan);
			return newPlan;
		} else {
			throw new ResourceNotFoundException("Can not locate plan with uid: " + planUid);
		}
	}

	@Override
	public JSONObject patchBenefitOfThePlan(String planKey, JSONObject benefitObject, JSONObject dataToBePatched)
			throws ParseException {
		// //system.out.println(benefitObject.toJSONString());
		// system.out.println("benefitObject before"+benefitObject);
		// system.out.println("datatobepathced "+dataToBePatched);
		String benefitUid = "";
		String planString = jedisConnection.get(planKey);
		JSONObject planObject = (JSONObject) new JSONParser().parse(planString);
		for (Object key : dataToBePatched.keySet()) {
			// system.out.println("keykeykey"+key);
			Object entry = dataToBePatched.get(key);
			if (benefitObject.containsKey(key)) {
				JSONObject obj = (JSONObject) dataToBePatched.get(key);
				// system.out.println("objobjobj"+obj);
				JSONObject o = (JSONObject) benefitObject.get(key);
				for (Object insideKeys : obj.keySet()) {
					// system.out.println("o is"+o);
					// system.out.println("inside key is" + insideKeys);
					if (o.containsKey(insideKeys)) {
						o.replace(insideKeys, obj.get(insideKeys));
					}
					benefitUid = (String) o.get("_id");
				}
				benefitObject.put(key, o);
				// system.out.println("replace obj "+o);
				// if (entry instanceof String) {//system.out.println("inside
				// string instance"+key);
				// benefitObject.replace(key, entry);

				// }
			} else {// system.out.println("inside else instance"+key);
				benefitObject.put(key, entry);
			}
			// JSONObject benefits = (JSONObject) planObject.get(key);
			/*
			 * if(null != planObject.get(key)) {
			 * ////system.out.println("nononono" +
			 * planObject.get(key).get("_id")); if (null != benefits.get("_id")
			 * && benefits.get("_id").toString() .equalsIgnoreCase(
			 * ((JSONObject) dataToBePatched.get(key)) .get("_id").toString()))
			 * { planObject.replace(key, benefitObject.get(key)); } } else {
			 */
			Set keySet = planObject.keySet();
			Iterator keyIt = keySet.iterator();
			while (keyIt.hasNext()) {
				Object keyObj = keyIt.next();
				// system.out.println("planplanplanss" + planObject);
				if (planObject.get(keyObj) instanceof JSONObject) {
					// system.out.println("not found in patchsss" +
					// planObject.get(keyObj));
					if (null != ((JSONObject) planObject.get(key)).get("_id")
							&& ((JSONObject) planObject.get(key)).get("_id").toString()
									.equalsIgnoreCase(((JSONObject) dataToBePatched.get(key)).get("_id").toString()))
						planObject.replace(keyObj, benefitObject.get(key));
				} else if (planObject.get(keyObj) instanceof JSONArray) {
					JSONArray planArray = (JSONArray) planObject.get(keyObj);
					// system.out.println("mmmmmsss" + planArray.get(0));
					Iterator planIt = planArray.iterator();
					while (planIt.hasNext()) {
						JSONObject arrayObj = ((JSONObject) planIt.next());
						if (arrayObj.containsKey(key)) {
							if (null != ((JSONObject) arrayObj.get(key)).get("_id")
									&& ((JSONObject) arrayObj.get(key)).get("_id").toString().equalsIgnoreCase(
											((JSONObject) dataToBePatched.get(key)).get("_id").toString())) {
								// listOfPlanObj.add(((JSONObject)
								// arrayObj.get(key)));
								// system.out.println("mamamama"+
								// benefitObject.get(key));
								arrayObj.replace(key, benefitObject.get(key));
							}

						}
						// listOfPlanObj.add(((JSONObject)
						// planIt.next()));
					}
				}

			}
		}

		// }
		benefitObject.put("modifiedOn", getUnixTimestamp());
		// String benefitUid = (String) benefitObject.get("_id");
		// system.out.println("benefitUid"+benefitUid);
		jedisConnection.set(benefitUid, benefitObject.toJSONString());
		_queueService.sendMessage(benefitObject);

		List<JSONObject> newBenefits = new ArrayList<JSONObject>();
		/*
		 * for(JSONObject obj : benefits) {
		 * if(obj.get("_id").equals(benefitUid)) {//system.out.println(
		 * "inside replacing objects"
		 * +benefitUid+"-----"+benefitObject.toJSONString());
		 * 
		 * obj = benefitObject; //obj.replace(benefitUid,
		 * benefitObject.toJSONString()); //system.out.println("obj"+obj);
		 * }newBenefits.add(obj);
		 * 
		 * }
		 */
		planObject.put("modifiedOn", getUnixTimestamp());
		// system.out.println("newBenefits"+newBenefits);
		// planObject.replace("linkedPlanServices", newBenefits);
		try {
			planObject.replace("ETag", calculateETag(planObject));
			String planUid = (String) planObject.get("_id");// system.out.println("planUid"+planUid);
			jedisConnection.set(planUid, planObject.toJSONString());
			planObject = (JSONObject) new JSONParser().parse(jedisConnection.get(planKey));
			_queueService.sendMessage(planObject);
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

				// //system.out.println("Deleting Plan: " + planKey);
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
		// //system.out.println("incomingNode" + incomingNode);
		JSONObject toPersist = (JSONObject) parser.parse(incomingNode.toString());
		String uid = getUuidForObject(toPersist);
		String createdOn = getUnixTimestamp();
		toPersist.put("_id", uid);
		toPersist.put("createdOn", createdOn);
		jedisConnection.set(uid, toPersist.toJSONString());
		// system.out.println("inside processJsonObject" + uid);
		// system.out.println("toPersist" + toPersist.toJSONString());
		_queueService.sendMessage(toPersist);
		return uid;
	}

	private String getUuidForObject(JSONObject toPersist) {
		// //system.out.println("toPersist" + toPersist);
		String objectType = (String) toPersist.get("_type");
		// system.out.println(" objectType" + objectType);
		jedisConnection.incr(objectType);

		// //system.out.println(jedisConnection.get(objectType));
		return objectType + "__" + jedisConnection.get(objectType);
	}

	private String getUnixTimestamp() {
		Long unixDate = new Date().getTime() / 1000;
		return unixDate.toString();
	}

	private String calculateETag(JSONObject object)
			throws NoSuchAlgorithmException, UnsupportedEncodingException, ParseException {
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
