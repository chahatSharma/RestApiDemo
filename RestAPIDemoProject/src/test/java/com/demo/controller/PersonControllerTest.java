package com.demo.controller;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class PersonControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void testGetPerson() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/person/32").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().string(containsString("32")));
    }

    @Test
    public void testNewUpdatePerson() throws Exception {
        String roleParameter = "roleName";
        String roleValue = "help_desk";
        String personUid = "person__21";
        String parameterName = "username";
        String parameterValue = "aapeshave";
        String parameterKey = "user__43";
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI0MCIsImlhdCI6MTQ3OTkzNzE5Nywic3ViIjoiQUNDRVNTX1RPS0VOIiwiaXNzIjoiREVNTy5JTkMiLCJ1c2VyIjoidXNlcl9fNDMiLCJ1cmwiOiJodHRwOi8vd3d3LmV4YW1wbGUuY29tIiwiZXhwIjoxNTExNDczMTk3fQ.KVvIWjTqg4OqvcjnjFJeMeOgdEc-_IU7hl7UzZLwVMk";
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.patch("/v1/person/person__21");
        mockHttpServletRequestBuilder.accept(MediaType.APPLICATION_JSON);
        mockHttpServletRequestBuilder.header("token", token);
        mockHttpServletRequestBuilder.content(roleValue);
        mockHttpServletRequestBuilder.param("parameterName", roleParameter);

        MockHttpServletResponse response = mvc.perform(mockHttpServletRequestBuilder).andReturn().getResponse();
        Assert.assertTrue(Boolean.parseBoolean(response.getContentAsString()));
    }


    @Test
    public void testNewUpdatePersonWithObjectData() throws Exception {
        String personUid = "person__21";
        String parameterName = "role";
        String parameterValue = "{\n" +
                "      \"_createdOn\": \"1479937197\",\n" +
                "      \"roleName\": \"help_desk\",\n" +
                "      \"objectName\": \"role\",\n" +
                "      \"_id\": \"role__43\",\n" +
                "      \"_modifiedOn\": \"1479949057\"\n" +
                "    }";
        String parameterKey = "user__43";
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI0MCIsImlhdCI6MTQ3OTkzNzE5Nywic3ViIjoiQUNDRVNTX1RPS0VOIiwiaXNzIjoiREVNTy5JTkMiLCJ1c2VyIjoidXNlcl9fNDMiLCJ1cmwiOiJodHRwOi8vd3d3LmV4YW1wbGUuY29tIiwiZXhwIjoxNTExNDczMTk3fQ.KVvIWjTqg4OqvcjnjFJeMeOgdEc-_IU7hl7UzZLwVMk";
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.patch("/v1/person/person__21");
        mockHttpServletRequestBuilder.accept(MediaType.APPLICATION_JSON);
        mockHttpServletRequestBuilder.header("token", token);
        mockHttpServletRequestBuilder.content(parameterValue);
        mockHttpServletRequestBuilder.param("parameterName", parameterName);

        MockHttpServletResponse response = mvc.perform(mockHttpServletRequestBuilder).andReturn().getResponse();
        Assert.assertTrue(Boolean.parseBoolean(response.getContentAsString()));
    }
}
