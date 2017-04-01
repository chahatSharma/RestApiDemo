package com.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;


public interface PlanService {
    /**
     * @param planNode get a plan node from controller
     * @return details about the added plan
     */
    String addPlan(JsonNode planNode);

    /**
     * @param planUid to get the plan
     * @return the plan associated with the uid
     */
    JSONObject getPlan(String planUid) throws ResourceNotFoundException, ParseException;

    /**
     * @param benefitObject to be added
     * @param planUid
     * @return plan object
     */
    JSONObject addBenefitToPlan(String benefitObject, String planUid) throws ResourceNotFoundException, ParseException;

    /**
     * @param planKey         Found plan object in the database
     * @param benefitObject   Object to be replaced in the database
     * @param dataToBePatched
     * @return plan object
     */
    JSONObject patchBenefitOfThePlan(String planKey, JSONObject benefitObject, JSONObject dataToBePatched) throws ParseException;

    /**
     * @param planKey key of the plan to be deleted
     * @return true if deleted
     */
    Boolean deletePlan(String planKey) throws ResourceNotFoundException;
}
