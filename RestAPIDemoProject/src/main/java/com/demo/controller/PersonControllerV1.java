package com.demo.controller;

import com.demo.service.PersonService;

import com.demo.service.SchemaService;
import com.demo.service.TokenService;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;



@RestController
@Api(description = "New Api to do CRUD operations on a person with or without user account.")
@RequestMapping("/v1/person")
public class PersonControllerV1 {

    @Autowired
    SchemaService _schemaService;

    @Autowired
    PersonService _personService;

    @Autowired
    TokenService _tokenService;

    

    private static final String SCHEMA_LOCATION = "SCHEMA__person";

    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ApiOperation(value = "Create a person", response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 403,
                    message = "json schema not validated",
                    responseHeaders = @ResponseHeader(
                            name = "BadRequest",
                            description = "valid json schema should be provided")),
            @ApiResponse(code = 500,
                    message = "Internal Server Error",
                    responseHeaders = @ResponseHeader(
                            name = "GENERAL_ERROR",
                            description = "unhandled exception occured"))
    })
    public String addPerson(@RequestBody String body, HttpServletResponse response) throws IOException {
        if (!(body.isEmpty())) {
            try {
                if (_schemaService.validateSchema(SCHEMA_LOCATION, body)) {System.out.println("Inside schema validation");
                    return _personService.v1AddPerson(body);
                } else {
                    response.sendError(403, "Schema not validated");
                }
            } catch (IOException | ProcessingException e) {
                response.sendError(403, "Schema not validated");
                System.out.println(e);
            }
        } else {
            System.out.println("body is blank");
        }
        return null;
    }

    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a person from database", response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 500,
                    message = "Internal Server Error",
                    responseHeaders = @ResponseHeader(
                            name = "GENERAL_ERROR",
                            description = "unhandled exception occured"))
    })
    public String getPerson(@PathVariable("uuid") String uid, HttpServletResponse response) throws IOException {
        try {
            if (!StringUtils.isBlank(uid)) {
                return _personService.v1GetPerson(uid);
            }
        } catch (ResourceNotFoundException e) {
            response.sendError(404, "Requested Person Entry not Found");
        }
        return null;
    }

    @RequestMapping(value = "/{uuid}", method = RequestMethod.PATCH)
    public String patchPerson(@PathVariable("uuid") String personId,
                              @RequestHeader String token,
                              @RequestParam String parameterName,
                              @RequestBody String parameterValue,
                              HttpServletResponse response) throws IOException {
        try {System.out.println("Inside token validated");
            if (_tokenService.isTokenValidated(token, personId)) {System.out.println("tokenValidated");
                String userUid = _tokenService.getUserIdFromToken(token);
                Validate.notNull(userUid, "UserUid can not be null to do further actions");
                JSONObject result = _personService.newUpdatePerson(personId, parameterName, userUid, parameterValue);
                return result.toString();
            }
        } catch (ExpiredJwtException e) {
            response.sendError(401, "Token is expired. Exception: " + e.toString());
        } catch (SignatureException | MalformedJwtException e) {
            response.sendError(401, "Token is malformed. Exception: " + e.toString());
        }
        return null;
    }

    /*@RequestMapping(value = "/message", method = RequestMethod.POST)
    public String postMessage(@RequestBody String message) {
        _queueService.sendMessage(message);
        return null;
    }*/
}
