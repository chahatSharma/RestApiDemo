package com.demo.controller;


import com.demo.service.PersonService;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.FileReader;
import java.io.IOException;

@RestController
@Deprecated
@Api(description = "API to do operations on person")
public class PersonController {


    private static final String PERSON_SCHEMA = "person-schema.json";

    @Autowired
    PersonService personService;


    @GET
    @RequestMapping("/person/{uuid}")
    @ApiOperation(value = "Find person using UUID",
            response = String.class)
    public String returnPerson(@PathVariable("uuid") String id) {
        return personService.getPerson(id);
    }


    @POST
    @RequestMapping("/person")
    @ApiOperation(value = "Create a person",
            response = String.class)
    public String addPerson(@RequestBody String body, HttpServletResponse response) {

        try {
            isPersonSchemaValidated(body, "/Users/ajinkya/IdeaProjects/spring_boot_check/person_schema.json");
            return personService.processAndAddPerson(body);
        } catch (BadRequestException e) {
            try {
                response.sendError(400, e.getMessage());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    private Boolean isPersonSchemaValidated(String jsonData, String jsonSchemapath) throws BadRequestException {
        JSONParser parser = new JSONParser();
        try {
            String schema = parser.parse(new FileReader(jsonSchemapath)).toString();

            final JsonNode d = JsonLoader.fromString(jsonData);
            final JsonNode s = JsonLoader.fromString(schema);

            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonValidator v = factory.getValidator();

            ProcessingReport report = null;
            try {
                report = v.validate(s, d);
            } catch (ProcessingException e) {
                e.printStackTrace();
            }

            if (!report.toString().contains("success")) {
                throw new BadRequestException(
                        report.toString());
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return Boolean.FALSE;
    }
}
