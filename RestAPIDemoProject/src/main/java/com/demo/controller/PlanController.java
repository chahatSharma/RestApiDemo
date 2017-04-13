package com.demo.controller;

import com.demo.service.PlanService;
import com.demo.service.SchemaService;
import com.demo.service.TokenService;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import scala.annotation.meta.field;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
					System.out.println("userUid" + userUid);
					ObjectMapper mapper = new ObjectMapper();
					JsonNode planNode = mapper.readTree(planBody);
					((ObjectNode) planNode).put("userUid", userUid);
					String result = _planService.addPlan(planNode);
					if (StringUtils.isNotBlank(result)) {
						System.out.println("result" + result);
						PlanAggregate responseEntity = new PlanAggregate();
						JSONObject responseObject = (JSONObject) new JSONParser().parse(result);
						responseEntity._id = (String) responseObject.get("_id");
						System.out.println("Before responseEntity" + responseObject);
						System.out.println("Befor response" + response.getHeader("eTag"));
						processETag(response, responseObject);
						System.out.println("After responseEntity" + responseObject);
						System.out.println("After response" + response.getHeader("eTag"));
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
					PlanAggregate responseEntity = new PlanAggregate();
					// responseEntity.
					JSONObject responseObject = (JSONObject) new JSONParser().parse(plan.toJSONString());
					responseEntity._id = (String) responseObject.get("_id");
					processETag(response, responseObject);
					responseEntity._objectInfo = responseObject;
					System.out.println("responseObject" + responseObject);
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
	@RequestMapping(value = "/{uid}/benefit", method = RequestMethod.PUT)
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
				if (StringUtils.isNotBlank(tokenInfo.role) && StringUtils.equals("admin", tokenInfo.role)) {
					String pathToSchema = "SCHEMA__" + getPathToSchema(benefitBody);
					System.out.println("pathToSchema" + pathToSchema);
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
								"Bad Schema. Please check schema or add ObjectName field in the payload");
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
					"Bad Schema. Please check schema or add ObjectName field in the payload. More Info: "
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
			System.out.println("token" + token);
			if (_tokenService.isTokenValidated(token, "Sample String")) {
				TokenService.TokenInfo tokenInfo = _tokenService.getTokenInfo(token);
				Validate.notNull(tokenInfo);
				Integer flag = 0;
				if (StringUtils.isNotBlank(tokenInfo.role) && StringUtils.equals("admin", tokenInfo.role)) {
					JSONObject plan = null;
					try {
						plan = _planService.getPlan(planUid);
						Validate.notNull(plan);

						ObjectMapper mapper = new ObjectMapper();
						JsonNode patchNode = mapper.readTree(benefitData);
						Iterator<String> patchIt = patchNode.fieldNames();
						ArrayList<String> patchKeys = new ArrayList<>();
						JSONObject objectToBePatched = (JSONObject) new JSONParser().parse(benefitData);
						ArrayList<JSONObject> listOfPlanObj = new ArrayList<>();
						JSONObject result = new JSONObject();
						while (patchIt.hasNext()) {
							String key = patchIt.next();
							System.out.println("keykeykeykey" + key);
							patchKeys.add(key);
							JSONObject object = new JSONObject();
							if (plan.containsKey(key)) {
								if (plan.get(key) instanceof JSONObject) {
									object = (JSONObject) plan.get(key);
									listOfPlanObj.add(object);
									System.out.println("inininin" + object);
								} else if (plan.get(key) instanceof JSONArray) {
									System.out.println("chahat" + plan.get(key));
									JSONArray planArray = (JSONArray) plan.get(key);
									System.out.println("kkksllls" + planArray.get(0));
									Iterator planIt = planArray.iterator();
									while (planIt.hasNext()) {
										// System.out.println("ppppp" +
										// ((JSONObject) planIt.next()));
										listOfPlanObj.add(((JSONObject) planIt.next()));
									}
								}

								

							} else {
								Set keySet = plan.keySet();
								Iterator keyIt = keySet.iterator();
								while (keyIt.hasNext()) {
									Object keyObj = keyIt.next();
									System.out.println("planplanplan" + plan);
									if (plan.get(keyObj) instanceof JSONObject)
										System.out.println("not found in patch" + plan.get(keyObj));
									else if (plan.get(keyObj) instanceof JSONArray) {
										JSONArray planArray = (JSONArray) plan.get(keyObj);
										System.out.println("mmmmm" + planArray.get(0));
										Iterator planIt = planArray.iterator();
										while (planIt.hasNext()) {
											JSONObject arrayObj = ((JSONObject) planIt.next());
											if (arrayObj.containsKey(key)) {System.out.println("arrayObjarrayObjarrayObj"+arrayObj);
												if (null != ((JSONObject) arrayObj.get(key)).get("_id")
														&& ((JSONObject) arrayObj.get(key)).get("_id").toString()
																.equalsIgnoreCase(
																		((JSONObject) objectToBePatched.get(key))
																				.get("_id").toString()))
												{
													listOfPlanObj.add(((JSONObject) arrayObj.get(key)));
													System.out.println("lislss"+listOfPlanObj);
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
								newObj.put(key, o);
								System.out.println("true for patchKeys"
										+ ((JSONObject) objectToBePatched.get(key)).get("_id"));
								System.out.println("llllll " + o.get("_id"));
								if (o.get("_id").toString().equalsIgnoreCase(
										((JSONObject) objectToBePatched.get(key)).get("_id").toString())) {
									System.out.println("verifying ID");
									result = _planService.patchBenefitOfThePlan(planUid, newObj,
											objectToBePatched);
									System.out.println("result got" + result);
								}
							}
							
						}
						Validate.notNull(result);
						PlanAggregate responseObject = new PlanAggregate();
						responseObject._id = (String) result.get("_id");
						responseObject._objectInfo = result;
						response = processETag(response, result);
						flag = 1;
						if (flag == 0) {
							response.sendError(404, "Benefit not found");
						}System.out.println("responseObject" +responseObject);
						return responseObject;
						

						/*List<JSONObject> benefits = (ArrayList<JSONObject>) plan.get("linkedPlanServices");
						Validate.notNull(benefits);
						//System.out.println("benefits" + benefits);
						for (JSONObject object : benefits) {
							if (object.containsValue("")) {
								 JSONObject objectToBePatched = (JSONObject)
								 new JSONParser().parse(benefitData);
								System.out.println("objectToBePatched" + object);
								JSONObject result = _planService.patchBenefitOfThePlan(planUid, object,
										objectToBePatched);
								Validate.notNull(result);
								PlanAggregate responseObject = new PlanAggregate();
								responseObject._id = (String) result.get("_id");
								responseObject._objectInfo = result;
								response = processETag(response, result);
								flag = 1;
								return responseObject;
							}
						}*/
						
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
