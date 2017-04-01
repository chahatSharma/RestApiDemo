package com.demo.service;

import com.demo.service.impl.PlanServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by ajinkyapeshave on 11/30/16.
 */
public class PlanServiceTest {


    public static final String VALID_PLAN_BODY = "{\n" +
            "  \"benefits\": [\n" +
            "    {\n" +
            "      \"objectName\": \"benefit\",\n" +
            "      \"price\": \"786\",\n" +
            "      \"description\": \"sample benefit\",\n" +
            "      \"name\": \"benefit a\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"objectName\": \"plan\",\n" +
            "  \"startDate\": \"1124151\",\n" +
            "  \"endDate\": \"112541\",\n" +
            "  \"totalPrice\": \"786\"\n" +
            "}";
    public static final String VALID_BENEFIT_BODY = "{\n" +
            "      \"_createdOn\": \"1480687384\",\n" +
            "      \"price\": \"786\",\n" +
            "      \"name\": \"benefit a\",\n" +
            "      \"objectName\": \"benefit\",\n" +
            "      \"description\":\"Silver plan\"\n" +
            "    }";

    private PlanServiceImpl _planService;

    @Before
    public void setUp() throws Exception {
        _planService = new PlanServiceImpl();
    }

    @Test
    public void addPlan() throws Exception {
        String validPlan = VALID_BENEFIT_BODY;
        JsonNode planNode = new ObjectMapper().readTree(validPlan);
        ((ObjectNode) planNode).put("userUid", "user__44");
        String result = _planService.addPlan(planNode);
        Assert.assertNotNull(result);
    }

    @Test
    public void testAddBenefitToPlan() {
        String benefitBody = VALID_BENEFIT_BODY;
        String planUid = "plan__20";
        try {
            JSONObject jsonObject = _planService.addBenefitToPlan(benefitBody, planUid);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}