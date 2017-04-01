package com.demo.service;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.elasticsearch.ResourceNotFoundException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public interface SchemaService {
    /**
     * Adds schema to Redis
     *
     * @param jsonSchema This is schema
     * @return returns key of added schema
     */
    @Deprecated
    String addSchemaToRedis(String jsonSchema, String objectName) throws ParseException;

    /**
     * Returns schema as string from REDIS
     *
     * @param pathToSchema This is patch to schema
     * @return returns schema
     */
    String getSchemaFromRedis(String pathToSchema);

    /**
     * Deletes schema stored in the database and return success message
     *
     * @param pathToSchema This is patch to schema
     * @return BOOLEAN if schema is deleted
     */
    Boolean deleteSchemaFromRedis(String pathToSchema);

    /**
     * @param pathToSchema This is patch to schema
     * @param data         Input body accepted form client
     * @return returns Boolean if schema is validated
     */
    Boolean validateSchema(String pathToSchema, String data) throws IOException, ProcessingException;

    /**
     * @param pathToSchema This is patch to schema
     * @param data         Input body accepted form client
     * @return returns Boolean if field is present in SCHEMA
     */
    Boolean validateFieldInSchema(String pathToSchema, String data);

    /**
     * @param schemaBody This is patch to schema
     * @return returns key of added schema
     */
    String addNewSchema(String schemaBody);

    /**
     *
     * @param pathToSchema path of the schema to be changed
     * @param toChange body of the object to be changed
     * @param paramterName
     * @return return the result
     */
    String patchSchema(String pathToSchema, JSONObject toChange, String paramterName) throws ResourceNotFoundException;
}
