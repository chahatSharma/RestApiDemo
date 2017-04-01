package com.demo.service;

import com.demo.service.impl.PersonServiceImpl;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class PersonServiceTest {
    PersonServiceImpl personService;

    @Before
    public void setUp() throws Exception {
        personService = new PersonServiceImpl();
    }

    @Test
    public void v1AddPerson() throws Exception {

    }

    @Test
    public void v1GetPerson() throws Exception {

    }

    @Test
    public void testUpdatePerson() throws Exception{

    }

    @Test
    public void testNewUpdatePerson() throws Exception {
        String personUid = "person__21";
        String parameterName = "username";
        String parameterValue = "aapeshave";
        String parameterKey = "user__43";
        JSONObject result = personService.newUpdatePerson(personUid, parameterName, parameterKey, parameterValue);
        int i =0;
    }
}