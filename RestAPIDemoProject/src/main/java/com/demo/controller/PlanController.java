package com.demo.controller;

import com.demo.service.PlanService;
import com.demo.service.QueueService;
import com.demo.service.SchemaService;
import com.demo.service.TokenService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import ch.qos.logback.core.net.SyslogOutputStream;
import ch.qos.logback.core.subst.Token.Type;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.swagger.annotations.*;
import redis.clients.jedis.Jedis;
import scala.annotation.meta.field;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.hadoop.mapred.gethistory_jsp;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonObjectDeserializer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@Api(description = "API to do CRUD Operations on Plan")
@RequestMapping("/plan")
public class PlanController {

	public static final String SAMPLE_PLAN_BODY = "{\n" + "  \"totalPrice\": \"786\",\n"
			+ "  \"objectName\": \"plan\",\n" + "  \"benefits\": [\n" + "    {\n" + "      \"price\": \"786\",\n"
			+ "      \"name\": \"Gold Plan\",\n" + "      \"objectName\": \"benefit\",\n"
			+ "      \"description\": \"It is very good plan\"\n" + "    }\n" + "  ],\n"
			+ "  \"startDate\": \"12/01/2016\"\n" + "}";
	@Autowired
	TokenService _tokenService;

	@Autowired
	SchemaService _schemaService;

	@Autowired
	PlanService _planService;
	
	@Autowired
	QueueService _queueService;

	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation(value = "Create a plan with benefits", notes = "Note: You need to provide authentication token", response = PlanAggregate.class)
	@ApiResponses(value = {
			@ApiResponse(code = 401, message = "customer not authorized to make api call", responseHeaders = @ResponseHeader(name = "UNAUTHORIZED", description = "customer not authorized to make api call")),
			@ApiResponse(code = 400, message = "Bad Request", responseHeaders = @ResponseHeader(name = "BAD_REQUEST", description = "bad request")),
			@ApiResponse(code = 500, message = "Internal Server Error", responseHeaders = @ResponseHeader(name = "GENERAL_ERROR", description = "unhandled exception occurred")) })
	public PlanAggregate createPlan(
			@ApiParam(value = "Authentication Token. It is usually created when you create User Account.") @RequestHeader(required = true) String token,
			@ApiParam(value = "JSON Body for plan. Refer to Schemas for more info", defaultValue = SAMPLE_PLAN_BODY) @RequestBody String planBody,
			HttpServletResponse response) throws IOException {
		try {
			if (_tokenService.isTokenValidated(token, "SampleString")) {
				String userUid = _tokenService.getUserIdFromToken(token);

				Validate.notNull(userUid, "UserUid can not be null to do further actions");
				String pathToSchema = "SCHEMA__" + "plan";// getPathToSchema(planBody);
				if (_schemaService.validateSchema(pathToSchema, planBody)) {
					// System.out.println("userUid" + userUid);
					ObjectMapper mapper = new ObjectMapper();
					JsonNode planNode = mapper.readTree(planBody);
					((ObjectNode) planNode).put("userUid", userUid);
					String result = _planService.addPlan(planNode);
					if (StringUtils.isNotBlank(result)) {
						// System.out.println("result" + result);
						PlanAggregate responseEntity = new PlanAggregate();
						JSONObject responseObject = (JSONObject) new JSONParser().parse(result);
						responseEntity._id = (String) responseObject.get("_id");
						// System.out.println("Before responseEntity" +
						// responseObject);
						// System.out.println("Befor response" +
						// response.getHeader("eTag"));
						processETag(response, responseObject);
						// System.out.println("After responseEntity" +
						// responseObject);
						// System.out.println("After response" +
						// response.getHeader("eTag"));
						responseEntity._objectInfo = responseObject;
						return responseEntity;
					} else {
						response.sendError(500, "Our Servers are Having Problems");
					}
				} else {
					response.sendError(401, "Schema not found.");
				}
			}
		} catch (ExpiredJwtException e) {
			response.sendError(401, "Token is expired. Exception: " + e.toString());
		} catch (SignatureException | MalformedJwtException e) {
			response.sendError(401, "Token is malformed. Exception: " + e.toString());
		} catch (ProcessingException e) {
			response.sendError(500, "Failed While Processing Schema. Report: " + e.toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/{uid}", method = RequestMethod.GET)
	@ApiOperation(value = "Get a  plan related to user")
	public PlanAggregate getPlan(
			@ApiParam(value = "Authentication Token. It is usually created when you create User Account.") @RequestHeader(required = true) String token,
			@PathVariable("uid") String planUid, HttpServletResponse response, HttpServletRequest request)
			throws IOException {
		try {
			if (_tokenService.isTokenValidated(token, "SampleString")) {
				JSONObject plan = null;
				try {
					plan = _planService.getPlan(planUid);
					Jedis jedis = new Jedis();
					Validate.notNull(plan);
					String eTag = request.getHeader("If-None-Match");
					if (StringUtils.isNotBlank(eTag)) {
						String eTagFromObject = (String) plan.get("ETag");

						if (StringUtils.isNotEmpty(eTagFromObject)) {
							if (StringUtils.equals(eTagFromObject, eTag)) {
								response.sendError(304, "Object is not modified");
							}
						}

						// response.addHeader("ETag", eTagFromObject);
						// response
					}

					// responseEntity.
					JSONObject responseObject = (JSONObject) new JSONParser().parse(plan.toJSONString());
					JSONObject newPlan = plan;

					Set keys = responseObject.keySet();
					Iterator it = keys.iterator();
					while (it.hasNext()) {
						String key = (String) it.next();
						if (!key.equalsIgnoreCase("_id") && !key.equalsIgnoreCase("_type") && !key.equalsIgnoreCase("userUid")) {
							if (null != plan.get(key) && plan.get(key) instanceof JSONArray) {
								JSONArray array = (JSONArray) plan.get(key);
								JSONArray newArray = new JSONArray();
								Iterator arrayIt = array.iterator();
								JSONObject arrayob = new JSONObject();
								while (arrayIt.hasNext()) {
									JSONObject arrayObj = (JSONObject) arrayIt.next();
									Set objKey = arrayObj.keySet();
									Iterator itt = objKey.iterator();
									while (itt.hasNext()) {
										String k = (String) itt.next();
										if (!k.equalsIgnoreCase("_id") && !k.equalsIgnoreCase("_type")) {
											String s = jedis.get((String) arrayObj.get(k));
											if (null != s) {
												arrayob = (JSONObject) new JSONParser().parse(s);
												if (null != arrayob)
													arrayObj.replace(k, arrayob);
												// System.out.println(arrayObj +
												// "----- " + k);
											}
										}

									}

									newArray.add(arrayObj);
									// System.out.println(newArray +
									// "-----pppppp");
									// String objjs = jedis.get(arrayKey);
									// array.remov
								}

								newPlan.put(key, newArray);
							} else {// System.out.println("keykeykkk"+key);
								String obj = jedis.get((String) plan.get(key));
								JSONParser parser = new JSONParser();
								// System.out.println("objbjbjbj"+obj);
								if (null != obj && !obj.isEmpty()) {

									newPlan.put(key, parser.parse(obj));
									// System.out.println("replacing newplan
									// with obj" + newPlan);
								} else {
									newPlan.put(key, plan.get(key));
									// System.out.println("didnot find
									// obj"+newPlan);
								}
							}

						}
					}
					// System.out.println("planplanplanplansss"+newPlan);
					JSONObject newResponseObject = (JSONObject) new JSONParser().parse(newPlan.toJSONString());
					PlanAggregate responseEntity = new PlanAggregate();
					responseEntity._id = (String) newResponseObject.get("_id");
					processETag(response, newResponseObject);
					responseEntity._objectInfo = newResponseObject;
					// System.out.println("responseObject" +
					// responseEntity._objectInfo);
					return responseEntity;
					// return plan.toJSONString();
				} catch (ResourceNotFoundException e) {
					response.sendError(404, e.toString());
				}
			}
		} catch (ExpiredJwtException e) {
			response.sendError(401, "Token is expired. Exception: " + e.toString());
		} catch (SignatureException | MalformedJwtException e) {
			response.sendError(401, "Token is malformed. Exception: " + e.toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/{uid}/planService", method = RequestMethod.PUT)
	@ApiOperation(value = "Add benefit to a plan", response = PlanAggregate.class, notes = "Authorization token is need. Role Supported Admin")
	public PlanAggregate addBenefitToPlan(
			@ApiParam(value = "Authentication Token. It is usually created when you create User Account.") @RequestHeader(required = true) String token,
			@ApiParam(value = "Uid of the plan for which benefit to be added") @PathVariable("uid") String planUid,
			@ApiParam(value = "JSON Body for benefit. Refer to Schemas for more info") @RequestBody String benefitBody,
			HttpServletResponse response) throws IOException {
		try {
			if (_tokenService.isTokenValidated(token, "Sample String")) {
				TokenService.TokenInfo tokenInfo = _tokenService.getTokenInfo(token);
				Validate.notNull(tokenInfo);
				if (StringUtils.isNotBlank(tokenInfo.role) && StringUtils.equals("admin", tokenInfo.role)) {System.out.println("getPathToSchema(benefitBody)"+getPathToSchema(benefitBody));
					String pathToSchema = "SCHEMA__" + getPathToSchema(benefitBody);
					// System.out.println("pathToSchema" + pathToSchema);
					Validate.notNull(pathToSchema);
					if (_schemaService.validateSchema(pathToSchema, benefitBody)) {
						JSONObject result = _planService.addBenefitToPlan(benefitBody, planUid);
						Validate.notNull(result);
						PlanAggregate responseEntity = new PlanAggregate();
						responseEntity._id = (String) result.get("_id");
						response = processETag(response, result);

						responseEntity._objectInfo = result;
						return responseEntity;
					} else {
						response.sendError(401,
								"Bad Schema. Please check schema or add _type field in the payload");
					}
				} else {
					response.sendError(403,
							"Admin role is needed to perform this action. Consider getting an admin token");
				}
			}
		} catch (ExpiredJwtException e) {
			response.sendError(401, "Token is expired. Exception: " + e.toString());
		} catch (SignatureException | MalformedJwtException e) {
			response.sendError(401, "Token is malformed. Exception: " + e.toString());
		} catch (ProcessingException e) {
			response.sendError(401,
					"Bad Schema. Please check schema or add _type field in the payload. More Info: "
							+ e.toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public String getPlanUsingParameters(HttpServletResponse response) throws IOException {
		response.sendError(405, "Method not supported");
		return null;
	}

	@ResponseBody
	@ApiOperation(value = "Patch benefit object related to a plan", response = PlanAggregate.class, nickname = "Patch Benefit")
	@RequestMapping(value = "/{uid}", method = RequestMethod.PATCH)
	public PlanAggregate patchBenefit(
			@ApiParam(value = "Authentication Token. It is usually created when you create User Account.") @RequestHeader(required = true) String token,
			@ApiParam(value = "Uid of the plan for which benefit to be added") @PathVariable("uid") String planUid,
			HttpServletResponse response,

			@ApiParam(value = "Body to be patched") @RequestBody String benefitData) throws IOException {
		try {
			// System.out.println("token" + token);
			if (_tokenService.isTokenValidated(token, "Sample String")) {
				TokenService.TokenInfo tokenInfo = _tokenService.getTokenInfo(token);
				Validate.notNull(tokenInfo);
				Integer flag = 0;
				int countId =0;
				if (StringUtils.isNotBlank(tokenInfo.role) && StringUtils.equals("admin", tokenInfo.role)) {
					JSONObject plan = null;
					try {
						plan = _planService.getPlan(planUid);
						Validate.notNull(plan);

						///////////////////////////////////////////////////////////////
						ObjectMapper mapper = new ObjectMapper();
						if (benefitData.contains("_id")) {

						} else {
							response.sendError(403, "Please provide _id for the Json object to be updated");
							return null;
						}

						JsonNode patchNode = mapper.readTree(benefitData);
						Iterator<String> patchIt = patchNode.fieldNames();

						///////////////////////////////////////////////////////////////

						Jedis jedisConnection = new Jedis();
						JSONParser parser = new JSONParser();
						ArrayList<String> patchKeys = new ArrayList<>();
						JSONObject objectToBePatched = (JSONObject) new JSONParser().parse(benefitData);
						ArrayList<JSONObject> listOfPlanObj = new ArrayList<>();
						JSONObject result = new JSONObject();
						boolean isArrayObj = false;
						while (patchIt.hasNext()) {
							String key = patchIt.next();
							// System.out.println("keykeykeykey" + key);
							// System.out.println("planplanplan" + plan);
							patchKeys.add(key);
							JSONObject object = new JSONObject();
							if (plan.containsKey(key)) {
								if (plan.get(key) instanceof JSONArray) {
									// System.out.println("chahat" +
									// plan.get(key));
									JSONArray planArray = (JSONArray) plan.get(key);

									Iterator planIt = planArray.iterator();
									while (planIt.hasNext()) {
										// System.out.println("ppppp" +
										// ((JSONObject) planIt.next()));
										object = ((JSONObject) planIt.next());
										listOfPlanObj.add(object);
										// System.out.println("kkksllls" +
										// object.get("_id"));
										isArrayObj = true;
									}
								} else {
									String id = (String) plan.get(key);

									object = (JSONObject) parser.parse(jedisConnection.get(id));
									listOfPlanObj.add(object);
									// System.out.println("inininin" + object);
								}

							} else {
								Set keySet = plan.keySet();
								Iterator keyIt = keySet.iterator();
								while (keyIt.hasNext()) {
									Object keyObj = keyIt.next();
									// System.out.println("planplanplan" +
									// plan);
									if (plan.get(keyObj) instanceof JSONObject) {
										// System.out.println("not found in
										// patch" + plan.get(keyObj));
									} else if (plan.get(keyObj) instanceof JSONArray) {
										JSONArray planArray = (JSONArray) plan.get(keyObj);
										// System.out.println("mmmmm" +
										// planArray.get(0));
										Iterator planIt = planArray.iterator();
										while (planIt.hasNext()) {
											JSONObject arrayObj = ((JSONObject) planIt.next());
											if (arrayObj.containsKey(key)) {
												// System.out.println("arrayObjarrayObjarrayObj"
												// + ((JSONObject)
												// objectToBePatched.get(key)).get("_id"));
												// JSONObject newObj =
												// (JSONObject)
												// parser.parse((String)
												// objectToBePatched.get(key));

												if (objectToBePatched.get(key) instanceof JSONObject) {
													// System.out.println("vvvv"
													// + arrayObj.get(key));
													if (null != arrayObj.get(key) && (arrayObj.get(key).toString()
															.equalsIgnoreCase(((JSONObject) objectToBePatched.get(key))
																	.get("_id").toString()))) {countId = countId + 1;
														String id = (String) arrayObj.get(key);
														object = (JSONObject) parser.parse(jedisConnection.get(id));
														listOfPlanObj.add(object);
														// System.out.println("lislss"
														// + object);
													}
												}

											}
											// listOfPlanObj.add(((JSONObject)
											// planIt.next()));
										}
									}

								}

							}

							for (JSONObject o : listOfPlanObj) {
								JSONObject newObj = new JSONObject();

								// System.out.println(
								// "true for patchKeys" + ((JSONObject)
								// objectToBePatched.get(key)).get("_id"));
								// System.out.println("llllll " + o.get("_id"));
								// System.out.println("full object " + o);
								// System.out.println("isArrayObj" +
								// isArrayObj);
								if (isArrayObj) {
									Set keySet = o.keySet();
									Iterator keyIt = keySet.iterator();
									while (keyIt.hasNext()) {
										String k = (String) keyIt.next();
										// System.out.println("angad" +
										// o.get(k));
										// System.out.println("chinu" +
										// ((JSONObject)
										// objectToBePatched.get(key)).get(k));

										if (null != ((JSONObject) objectToBePatched.get(key)).get(k)
												&& (((JSONObject) objectToBePatched.get(key))
														.get(k) instanceof JSONObject)) {
											// System.out.println(
											// "((JSONObject) ((JSONObject)
											// objectToBePatched.get(key)).get(k)).get(_id)"
											// + ((JSONObject) ((JSONObject)
											// objectToBePatched.get(key))
											// .get(k)).get("_id"));
											if (((JSONObject) ((JSONObject) objectToBePatched.get(key)).get(k))
													.get("_id").equals((o.get(k)))) {countId = countId+1;
												// System.out.println("o.get(k).toString()"
												// + o.get(k).toString());
												String uid = o.get(k).toString();
												o = (JSONObject) parser.parse(jedisConnection.get(o.get(k).toString()));
												newObj = o;
												// System.out.println("ooooo11"
												// + newObj);
												JSONObject toUpdate = (JSONObject) ((JSONObject) objectToBePatched
														.get(key)).get(k);
												Set keyset = toUpdate.keySet();
												Iterator keyin = keyset.iterator();
												while (keyin.hasNext()) {
													String propKey = (String) keyin.next();
													// System.out.println("lalala"
													// + propKey);
													newObj.replace(propKey, toUpdate.get(propKey));
												}
												// System.out.println("vnbvnvn"
												// + newObj);
												// System.out.println("uid" +
												// uid);
												jedisConnection.set(uid, newObj.toJSONString());
												_queueService.sendMessage(newObj.toJSONString());
											}
										}
									}

								} else if (o.get("_id").toString().equalsIgnoreCase(
										((JSONObject) objectToBePatched.get(key)).get("_id").toString())) {countId = countId+1;

									// result =
									// _planService.patchBenefitOfThePlan(planUid,
									// newObj,
									// objectToBePatched);

									newObj.put(key, o); System.out.println("verifying ID" + newObj );
									Set keyset = objectToBePatched.keySet();
									Iterator keyIt = keyset.iterator();
									while (keyIt.hasNext()) {
										String propKey = (String) keyIt.next();
										JSONObject objToBeChanged = (JSONObject) objectToBePatched.get(propKey);
										System.out.println("objectToBePatched.get(propKey)" +objToBeChanged);
										
										JSONObject newInnerObj = (JSONObject) newObj.get(propKey);
										Set innerKeys =objToBeChanged.keySet();
										Iterator i=innerKeys.iterator();
										while(i.hasNext())
										{
											String innerKey = (String) i.next();
											System.out.println("objToBeChanged.get(innerKey)"+objToBeChanged.get(innerKey));
											newInnerObj.replace(innerKey, objToBeChanged.get(innerKey));
											
										}
										System.out.println("newInnerObj"+newInnerObj);
										newObj.replace(propKey, newInnerObj);
										jedisConnection.set(o.get("_id").toString(),
												((JSONObject) newObj.get(propKey)).toJSONString());
										_queueService.sendMessage(((JSONObject) newObj.get(propKey)).toJSONString());
									}
									// System.out.println("newObj.toJSONString()"
									// + newObj.toJSONString());
									// System.out.println("dinalresultkey" +
									// key);

									// System.out.println("result got" +
									// result);
								}
							}

						}

						
						if(countId == 0 )
						{
							response.sendError(404, "_id not found");
							return null;
						}
						JSONObject mergedPlan = (JSONObject) parser.parse(jedisConnection.get(planUid)); 
						System.out.println("mergedplan" + mergedPlan);
						JSONObject newMergedPlan = mergedPlan;
						Set mergedPlanKeys = mergedPlan.keySet();
						Iterator mPIt = mergedPlanKeys.iterator();
						JSONArray newArray = new JSONArray();
						while (mPIt.hasNext()) {
							String mergedPlanMainKey = (String) mPIt.next();
							if (!mergedPlanMainKey.equalsIgnoreCase("_id")&&!mergedPlanMainKey.equalsIgnoreCase("_type") &&
									!mergedPlanMainKey.equalsIgnoreCase("userUid")) {
								// System.out.println("merged plan" +
								// mergedPlan);
								// System.out.println("ffff" +
								// mergedPlanMainKey+ "---" +
								// mergedPlan.get("linkedPlanServices") + "----"
								// + (mergedPlan.get(mergedPlanMainKey)
								// instanceof JSONArray));
								if (mergedPlan.get(mergedPlanMainKey) instanceof JSONArray) {
									JSONArray array = (JSONArray) mergedPlan.get(mergedPlanMainKey);
									// System.out.println("arrayarrayarray"+array);
									Iterator arrayIt = array.iterator();
									while (arrayIt.hasNext()) {
										JSONObject childObject = (JSONObject) arrayIt.next();
										JSONObject newChildObject = childObject;
										// System.out.println("kanu"+childObject);
										for (Object id : childObject.keySet()) {
											if (!id.toString().equalsIgnoreCase("_id")
													&& !id.toString().equalsIgnoreCase("_type")) {
												String uid = (String) childObject.get(id);
												// System.out.println("llloolll"+uid
												// + "----" + id);
												// System.out.println("plplp"
												// +jedisConnection.get(uid));
												if (null != jedisConnection.get(uid)) {
													JSONObject j = (JSONObject) parser.parse(jedisConnection.get(uid));
													// System.out.println("kjkj"+j);
													newChildObject.replace(id, j);
													// System.out.println("newChildObject"+newArray);
													if (!newArray.contains(newChildObject))
														newArray.add(newChildObject);
												}
											}
											/*
											 * else { ListOrderedMap lom = new
											 * ListOrderedMap(); lom.put("_id",
											 * childObject.get("_id"));
											 * lom.put("_type",
											 * childObject.get("_type"));
											 * newArray.add(lom);
											 * 
											 * }
											 */
											// System.out.println("newArray"+newArray);
										}
									}

								} else {
									String mergedPlanMainKeyValuee = (String) mergedPlan.get(mergedPlanMainKey);
									String objValue = jedisConnection.get(mergedPlanMainKeyValuee);
									 System.out
									 .println("popopoppp" +
									 mergedPlanMainKeyValuee + "[[[[]]]]"
									 + objValue);
									if (null != objValue && !objValue.isEmpty() ) {
										JSONObject obb = (JSONObject) parser.parse(objValue);
										
										newMergedPlan.replace(mergedPlanMainKey, obb);
										// System.out.println("newMergedPlan" +
										// newMergedPlan);
									} else {
										// System.out.println("zozozo" +
										// mergedPlanMainKeyValuee + "[[[[]]]]"
										// + mergedPlanMainKeyValuee);
									}
								}
							}

						}
						Set kkk = newMergedPlan.keySet();
						Iterator iterator = kkk.iterator();
						while (iterator.hasNext()) {
							Object oo = iterator.next();
							// System.out.println("newArraynewArraynewArray"+oo);
							// System.out.println("newArraynewArray"+newArray);
							if (newMergedPlan.get(oo) instanceof JSONArray) {
								newMergedPlan.replace(oo, newArray);
								break;
							}
						}
						// System.out.println("gggg" + newMergedPlan);
						result = newMergedPlan;
						Validate.notNull(result);
						PlanAggregate responseObject = new PlanAggregate();
						responseObject._id = (String) result.get("_id");
						responseObject._objectInfo = result;
						response = processETag(response, result);
						flag = 1;
						if (flag == 0) {
							response.sendError(404, "Benefit not found");
						}
						// System.out.println("responseObject" +
						// responseObject);
						return responseObject;

						/*
						 * List<JSONObject> benefits = (ArrayList<JSONObject>)
						 * plan.get("linkedPlanServices");
						 * Validate.notNull(benefits);
						 * //System.out.println("benefits" + benefits); for
						 * (JSONObject object : benefits) { if
						 * (object.containsValue("")) { JSONObject
						 * objectToBePatched = (JSONObject) new
						 * JSONParser().parse(benefitData);
						 * System.out.println("objectToBePatched" + object);
						 * JSONObject result =
						 * _planService.patchBenefitOfThePlan(planUid, object,
						 * objectToBePatched); Validate.notNull(result);
						 * PlanAggregate responseObject = new PlanAggregate();
						 * responseObject._id = (String) result.get("_id");
						 * responseObject._objectInfo = result; response =
						 * processETag(response, result); flag = 1; return
						 * responseObject; } }
						 */

					} catch (ResourceNotFoundException e) {
						response.sendError(404, "Plan not found");
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else {
					response.sendError(403,
							"Admin role is needed to perform this action. Consider getting an admin token");
				}
			}
		} catch (ExpiredJwtException e) {
			response.sendError(401, "Token is expired. Exception: " + e.toString());
		} catch (SignatureException | MalformedJwtException e) {
			response.sendError(401, "Token is malformed. Exception: " + e.toString());
		}
		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/{uid}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Delete plan related to user", response = Boolean.class, notes = "Authorization token is need. Role Supported Admin")
	public Boolean deletePlan(
			@ApiParam(value = "Authentication Token. It is usually created when you create User Account.") @RequestHeader(required = true) String token,
			@PathVariable("uid") String planUid, HttpServletResponse response, HttpServletRequest request)
			throws IOException {
		try {
			if (_tokenService.isTokenValidated(token, "SampleString")) {
				try {
					Boolean aBoolean = _planService.deletePlan(planUid);
					return aBoolean;
				} catch (ResourceNotFoundException e) {
					response.sendError(404, "Plan not found");
				}
			}
		} catch (ExpiredJwtException e) {
			response.sendError(401, "Token is expired. Exception: " + e.toString());
		} catch (SignatureException | MalformedJwtException e) {
			response.sendError(401, "Token is malformed. Exception: " + e.toString());
		}
		return Boolean.FALSE;
	}

	private HttpServletResponse processETag(HttpServletResponse response, JSONObject responseObject) {
		String eTag = (String) responseObject.get("ETag");
		if (StringUtils.isNotBlank(eTag)) {
			response.addHeader("ETag", eTag);
			responseObject.remove("ETag");
		}
		return response;
	}

	private String getPathToSchema(String payload) {
		JSONObject bodyObject = null;
		try {
			bodyObject = (JSONObject) new JSONParser().parse(payload);
			System.out.println("payload" + bodyObject);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Validate.notNull(bodyObject);
		return (String) bodyObject.get("_type");
	}

	@ApiModel(description = "Plan Aggregate Model")
	public static class PlanAggregate {
		@ApiModelProperty(value = "Identifier. Use this to Request Plan from Server")
		@JsonProperty(required = true)
		String _id;
		@ApiModelProperty(value = "Additional Information about the plan")
		@JsonProperty(required = true)
		JSONObject _objectInfo;
	}
}
