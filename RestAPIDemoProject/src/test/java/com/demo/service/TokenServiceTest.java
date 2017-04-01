package com.demo.service;

import com.demo.pojo.AccessToken;
import com.demo.service.impl.TokenServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TokenServiceTest {

    TokenServiceImpl tokenService;

    @Before
    public void setUp() throws Exception {
        tokenService = new TokenServiceImpl();
    }

    @Test
    public void testCreateAccessToken() throws Exception {

    }

    @Test
    public void testIsTokenValidated() throws Exception {

    }

    @Test
    public void testCreateAccessTokenAPI() throws Exception {
        String userUid = "user__1";
        String role = "admin";
        String subject = "ACCESS_TOKEN";
        AccessToken accessToken = tokenService.createAccessTokenAPI(userUid, role, subject);
        Assert.assertNotNull(accessToken);
    }

    @Test
    public void testCreateAccessToken1() throws Exception {

    }

    @Test
    public void testAddTokenToUser() throws Exception {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI0MyIsImlhdCI6MTQ4MDc3NjIyNCwic3ViIjoiQUNDRVNTX1RPS0VOIiwiaXNzIjoiREVNTy5JTkMiLCJ1c2VyIjoidXNlcl9fNDYiLCJ1cmwiOiJodHRwOi8vd3d3LmV4YW1wbGUuY29tIiwicm9sZSI6ImFkbWluIiwiZXhwIjoxNTEyMzEyMjI0fQ.MlxU-2lcx9xz-WL68YvqbW37twPeNNIomUc9pE2XcuY";
        String roleName = "admin";
        String subject = "ACCESS_TOKEN";
    }
}