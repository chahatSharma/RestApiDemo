package com.demo.service.impl;

import com.demo.service.UserService;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by ajinkya on 10/24/16.
 */
public class UserServiceImplTest {

    @Autowired
    UserService _userService;

    @Test
    public void testNewAddUser() throws Exception {
        JSONObject userObject = new JSONObject();
        String s = _userService.newAddUser(userObject);
    }
}