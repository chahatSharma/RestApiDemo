package com.demo.controller;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.math.RandomUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SchemaControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void testAddJsonSchema() throws Exception {
        Map<String, String> params = new HashMap<>();
        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(new FileReader("/Users/ajinkya/IdeaProjects/spring_boot_check/person_schema.json"));
        String schema = object.toString();
        params.put("schema", schema);
        params.put("objectName", "person");
    }

    @Test
    public void getJSonSchema() throws Exception {
        String result = mvc.perform(MockMvcRequestBuilders
                .get("/schema/SCHEMA__User")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status()
                        .isOk()).andReturn().getResponse().getContentAsString();


        Jedis jedis = new Jedis("localhost");
        ScanParams params = new ScanParams();
        params.match("password:ad*");
        ScanResult<String> scanResult = jedis.scan("0", params);
        List<String> keys = scanResult.getResult();
        int i=0;
    }

    @Test
    public void testCheckJSONTreeNode() throws IOException {
        String json = "{\n" +
                "  \"objectName\": \"person\",\n" +
                "  \"firstName\": \"Apoorva\",\n" +
                "  \"lastName\": \"Yeragi\",\n" +
                "  \"userAccount\": {\n" +
                "  \t\"objectName\": \"userAccount\",\n" +
                "  \t\"username\" : \"aapeshave\",\n" +
                "  \t\"password\" : \"admin\",\n" +
                "  \t\"token\": [\n" +
                "  \t\t{\n" +
                "  \t\t\t\"objectName\": \"token\",\n" +
                "  \t\t\t\"tokenName\" : \"sample token\"\n" +
                "  \t\t}\n" +
                "  \t\t]\n" +
                "  },\n" +
                "  \"email\": [\n" +
                "    {\n" +
                "      \"objectName\": \"email\",\n" +
                "      \"emailAddress\": \"a@b.com\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"objectName\": \"email\",\n" +
                "      \"emailAddress\": \"example@b.com\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(json);
        Map<String, Object> rootMap = mapper.convertValue(rootNode, Map.class);
        JSONParser parser = new JSONParser();
        try {
            traverseTree(rootMap, parser, new HashMap<>());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void traverseTree(Map<String, Object> rootNode,
                              JSONParser parser,
                              HashMap<Object, Object> hashMap
    ) throws ParseException {
        int id = RandomUtils.nextInt();
        String objectType = (String) rootNode.get("_type");
        for (String objectKey : rootNode.keySet())
        {
            Object objectNode = rootNode.get(objectKey);
            if (objectNode instanceof ArrayList)
            {
                System.out.println("Array found: "+ objectNode.toString());
                List<Map<String, Object>> objectArray = (ArrayList<Map<String, Object>>) objectNode;
                for (Map<String, Object> property : objectArray)
                {
                    traverseTree(property, parser, hashMap);
                }
            }
            else if (objectNode instanceof Map)
            {
                System.out.println("Object found: "+ objectNode.toString());
                traverseTree((Map<String, Object>) objectNode, parser, hashMap);
            }
            else
            {
                hashMap.put(objectKey, objectNode.toString());
                System.out.println("Normal String Found: "+ objectNode.toString());
            }
        }
        //tempObject.put("_createdOn", getUnixTimestamp());
        //tempObject.put("_objectType", objectType);
        System.out.println("Loop finished. Printing Contents of Temp: " + hashMap.toString());
    }

    private String getUnixTimestamp() {
        Long unixDate = new Date().getTime() / 1000;
        String unixDateString = unixDate.toString();
        return unixDateString;
    }
}
