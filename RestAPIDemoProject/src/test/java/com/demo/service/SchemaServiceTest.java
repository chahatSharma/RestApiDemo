package com.demo.service;

import com.demo.service.impl.SchemaServiceImpl;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by ajinkya on 11/5/16.
 */
public class SchemaServiceTest {
    SchemaServiceImpl schemaService;
    public static final String TEST_SCHEMA_DATA = "{\n" +
            "  \"objectName\": \"dolore exercitation enim\",\n" +
            "  \"sampleObject\": {\n" +
            "    \"objectName\": \"in commodo\",\n" +
            "    \"name\": \"exercitation consectetur veniam est\",\n" +
            "    \"adddress\": \"deserunt adipisicing\"\n" +
            "  }\n" +
            "}";

    public static final String TEST_INVALID_DATA = "{\n" +
            "  \"objectName\": \"test\",\n" +
            "  \"sampleObject\": {\n" +
            "    \"objectName\": \"sampleObject\",\n" +
            "    \"name\": \"exercitation consectetur veniam est\",\n" +
            "    \"adddress\": \"deserunt adipisicing\",\n" +
            "    \"attribute\": \"ONe\"\n" +
            "  }\n" +
            "}";

    public static final String SCHEMA__USER = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"objectName\": \"user\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"password\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"_createdOn\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"role\": {\n" +
            "      \"objectName\": \"role\",\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"_createdOn\": {\n" +
            "          \"type\": \"string\"\n" +
            "        },\n" +
            "        \"roleId\": {\n" +
            "          \"type\": \"string\"\n" +
            "        },\n" +
            "        \"roleName\": {\n" +
            "          \"type\": \"string\"\n" +
            "        },\n" +
            "        \"objectName\": {\n" +
            "          \"type\": \"string\"\n" +
            "        },\n" +
            "        \"_id\": {\n" +
            "          \"type\": \"string\"\n" +
            "        }\n" +
            "      },\n" +
            "      \"required\": [\n" +
            "        \"roleId\",\n" +
            "        \"roleName\",\n" +
            "        \"objectName\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"objectName\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"eTag\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"_id\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"token\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": {\n" +
            "        \"objectName\": \"token\",\n" +
            "        \"type\": \"object\",\n" +
            "        \"properties\": {\n" +
            "          \"validTill\": {\n" +
            "            \"type\": \"integer\"\n" +
            "          },\n" +
            "          \"tokenUid\": {\n" +
            "            \"type\": \"string\"\n" +
            "          },\n" +
            "          \"role\": {\n" +
            "            \"type\": \"string\"\n" +
            "          },\n" +
            "          \"tokenId\": {\n" +
            "            \"type\": \"string\"\n" +
            "          },\n" +
            "          \"accessUrl\": {\n" +
            "            \"type\": \"string\"\n" +
            "          },\n" +
            "          \"createdOn\": {\n" +
            "            \"type\": \"integer\"\n" +
            "          },\n" +
            "          \"issuer\": {\n" +
            "            \"type\": \"string\"\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"username\": {\n" +
            "      \"type\": \"string\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"password\",\n" +
            "    \"role\",\n" +
            "    \"objectName\",\n" +
            "    \"username\"\n" +
            "  ]\n" +
            "}";
    public static final String SAMPLE_USER_BODY = "{\n" +
            "\t\"objectName\": \"user\",\n" +
            "\t\"username\": \"sampada\",\n" +
            "\t\"password\": \"admin\",\n" +
            "\t\"role\":{\n" +
            "\t\t\"objectName\": \"role\",\n" +
            "\t\t\"roleId\": \"1\",\n" +
            "\t\t\"roleName\": \"read__only\",\n" +
            "\t\t\"attribute\": \"sample\"\n" +
            "\t}\n" +
            "}";

    public static final String SAMPLE_INVALID_USER_BODY = "{\n" +
            "\t\"objectName\": \"user\",\n" +
            "\t\"username\": \"sampada\",\n" +
            "\t\"attribute\": \"hello\",\n" +
            "\t\"password\": \"admin\",\n" +
            "\t\"role\":{\n" +
            "\t\t\"objectName\": \"role\",\n" +
            "\t\t\"roleId\": \"1\",\n" +
            "\t\t\"roleName\": \"read__only\",\n" +
            "\t\t\"attribute\": \"sample\"\n" +
            "\t}\n" +
            "}";

    public static final String SCHEMA__address = "{\n" +
            "  \"objectName\": \"address\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"country\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"zipCode\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"city\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"objectName\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"street1\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"street2\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"state\": {\n" +
            "      \"type\": \"string\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"objectName\",\n" +
            "    \"street1\",\n" +
            "    \"street2\",\n" +
            "    \"city\",\n" +
            "    \"state\",\n" +
            "    \"country\",\n" +
            "    \"zipCode\"\n" +
            "  ]\n" +
            "}";

    @Before
    public void setUp() {
        schemaService = new SchemaServiceImpl();
    }

    @Test
    public void testDeleteSchemaFromRedis() throws Exception {

    }

    @Test
    public void testValidateSchemaWithValidUser() throws Exception {
        String pathToSchema = "SCHEMA__user";
        String sampleUser = SAMPLE_USER_BODY;
        Boolean isSchemaValidated = schemaService.validateSchema(pathToSchema, sampleUser);
        Assert.assertTrue(isSchemaValidated);
    }

    @Test
    public void testValidateSchemaWithInvalidUser() throws Exception {
        String pathToSchema = "SCHEMA__user";
        String sampleInvalidUserBody = SAMPLE_INVALID_USER_BODY;
        Boolean isSchemaValidated = schemaService.validateSchema(pathToSchema, sampleInvalidUserBody);
        Assert.assertTrue(isSchemaValidated);
    }

    @Test
    public void testSampleSchema() throws IOException, ProcessingException {
        String pathToSchema = "SCHEMA__test";
        String schema = schemaService.getSchemaFromRedis(pathToSchema);
        // String schemaData = TEST_SCHEMA_DATA;
        String schemaData = TEST_INVALID_DATA;
        Boolean schema__test = schemaService.validateSchema("SCHEMA__test", schemaData);
        Assert.assertTrue(schema__test);
    }

    @Test
    public void testValidateFieldInSchema() throws Exception {
    }

    @Test
    public void testAddNewSchema() throws Exception {

    }

    @Test
    public void testPatchSchema() throws Exception {
        String pathToSchema = "SCHEMA__plan";
        JSONObject type = new JSONObject();
        type.put("type", "String");
        String result = schemaService.patchSchema(pathToSchema, type, "sample");
        Assert.assertNotNull(result);
    }
}