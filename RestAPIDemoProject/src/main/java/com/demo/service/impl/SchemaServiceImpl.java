package com.demo.service.impl;

import com.demo.service.SchemaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class SchemaServiceImpl implements SchemaService {
    public static final String SCHEMA_PREFIX = "SCHEMA__";

    private Log log = LogFactory.getLog(SchemaServiceImpl.class);

    @Override
    public String addSchemaToRedis(String jsonSchema, String objectName) {
        Jedis jedis = new Jedis("localhost");
        try {
            jedis.set(SCHEMA_PREFIX + objectName, jsonSchema);
            return SCHEMA_PREFIX + objectName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
    }

    @Override
    public String getSchemaFromRedis(String pathToSchema) {
        Jedis jedis = new Jedis("localhost");
        try {
        	
            return jedis.get(pathToSchema);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
    }

    @Override
    public Boolean deleteSchemaFromRedis(String pathToSchema) {
        Jedis jedis = new Jedis("localhost");
        try {
            long result = jedis.del(pathToSchema);
            if (result == 1) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
    }

    @Override
    public Boolean validateSchema(String pathToSchema, String data) throws IOException, ProcessingException {
        String jsonSchema = getSchemaFromRedis(pathToSchema);
        System.out.println("ipathToSchema" + pathToSchema);
        if (jsonSchema != null && !(jsonSchema.isEmpty())) {
            final JsonNode d = JsonLoader.fromString(data);
            
            final JsonNode s = JsonLoader.fromString(jsonSchema);

            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonValidator validator = factory.getValidator();

            ProcessingReport report = null;
            report = validator.validateUnchecked(s, d, Boolean.TRUE);
//            if (isSchemaFreeFromErrors(report))
//            {
//                log.info("Schema is free form warnings");
//            }
            if (report != null) {
                if (!report.toString().contains("success")) {
                    throw new ProcessingException(report.toString());
                } else{System.out.println("Returning true");
                    return Boolean.TRUE;}
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean validateFieldInSchema(String pathToSchema, String data) {
        String schema = getSchemaFromRedis(pathToSchema);
        if (!StringUtils.isBlank(schema)) {
            if (StringUtils.contains(schema, data)) {
                return Boolean.TRUE;
            }
        } else {
            log.error("SCHEMA Not Found for " + pathToSchema);
        }
        return Boolean.FALSE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String addNewSchema(String schemaBody) {
        JSONParser parser = new JSONParser();
        Jedis jedis = new Jedis("localhost");
        try {
            Map<String, String> response = new HashMap<>();
            Map<String, Object> schemaObject = (HashMap<String, Object>) parser.parse(schemaBody);
            Map<String, JSONObject> properties = (HashMap<String, JSONObject>) schemaObject.get("properties");
            String schemaKey = SCHEMA_PREFIX + schemaObject.get("objectName");
            jedis.set(schemaKey, schemaObject.toString());
            response.put((String) schemaObject.get("objectName"), schemaKey);
            for (String propertyKey : properties.keySet()) {
                JSONObject property = properties.get(propertyKey);
                String objectType = (String) property.get("type");
                if (objectType.equals("array") || objectType.equals("object")) {
                    JSONObject newObjectSchema = new JSONObject();
                    String objectName = "";
                    if (objectType.equals("array")) {
                        newObjectSchema = (JSONObject) property.get("items");
                        objectName = (String) newObjectSchema.get("objectName");
                    } else if (objectType.equals("object")) {
                        newObjectSchema = property;
                        objectName = (String) property.get("objectName");
                    }

                    schemaKey = SCHEMA_PREFIX + objectName;
                    System.out.println("schemaKey" + schemaKey);
                    System.out.println("schema" + newObjectSchema.toJSONString());
                    jedis.set(schemaKey, newObjectSchema.toJSONString());
                    response.put(objectName, schemaKey);
                }
            }
            return response.toString();
        } catch (ParseException e) {
            throw new BadRequestException("Schema Generation Failed. Can not parse data");
        } finally {
            jedis.close();
        }
    }

    @Override
    public String patchSchema(String pathToSchema, JSONObject toChange, String parameterName) throws ResourceNotFoundException {
        String schemaToBeUpdated = getSchemaFromRedis(pathToSchema);
        if (!StringUtils.isBlank(schemaToBeUpdated)) {System.out.println(schemaToBeUpdated);
            try {
                JSONObject parentSchemaObject = (JSONObject) new JSONParser().parse(schemaToBeUpdated);
                JSONObject propertiesObject = (JSONObject) parentSchemaObject.get("properties");
                propertiesObject.put(parameterName, toChange);
                String newSchema = addNewSchema(parentSchemaObject.toJSONString());
                return newSchema;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            throw new ResourceNotFoundException("Requested Resource Not Found");
        }
        return null;
    }

    private Boolean isSchemaFreeFromErrors(ProcessingReport report) {
        Validate.notNull(report);
        Iterator<ProcessingMessage> processingMessageIterator = report.iterator();
        int flag = 0;
        while (processingMessageIterator.hasNext()) {
            JsonNode messageNode = processingMessageIterator.next().asJson();
            JsonNode message = messageNode.get("message");
            JsonNode level = messageNode.get("level");
            if (level.textValue().equals("warning")) {
                log.warn("Warning: " + message);
                flag = 1;
            } else if (level.textValue().equals("error")) {
                log.error("Error: " + message);
                flag = 1;
            }
        }
        if (flag == 1) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }
}
