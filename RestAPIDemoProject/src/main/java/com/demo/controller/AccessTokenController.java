package com.demo.controller;


import com.demo.service.TokenService;
import com.demo.service.UserService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.Key;
import java.util.Date;

@RestController
@RequestMapping("/access_token")
public class AccessTokenController {

    @Autowired
    TokenService _tokenService;

    @Autowired
    UserService _userService;

    private static final String API_SECRET = "aap1212";

    @POST
    @RequestMapping("/")
    private String createToken(@RequestBody(required = true) TokenEntity tokenEntity) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(API_SECRET);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder().setId(tokenEntity.id)
                .setIssuedAt(now)
                .setSubject(tokenEntity.subject)
                .setIssuer(tokenEntity.issuer)
                .claim("person", "Chahat")
                .signWith(signatureAlgorithm, signingKey);

        //TODO: You can add more claims
        return builder.compact();
    }

    @POST
    @RequestMapping("/new")
    public TokenEntity createTokenForUser(@RequestHeader String token,
                                          @RequestParam String roleName,
                                          @RequestParam String subject,
                                          HttpServletResponse response) throws IOException {
        try {
            if (_tokenService.isTokenValidated(token, "Sample")) {
                String userUid = _tokenService.getUserIdFromToken(token);
                JSONObject accessToken = _tokenService.createAccessToken(userUid, roleName, subject);
                System.out.println("Created Token with uid: " + accessToken.get("tokenId"));
                if (_userService.addTokenToUser(userUid, accessToken)) {
                    TokenEntity responseEntity = new TokenEntity();
                    responseEntity.id = (String) accessToken.get("tokenUid");
                    responseEntity.subject = subject;
                    responseEntity.issuer = (String) accessToken.get("issuer");
                    responseEntity.userUid = userUid;
                    responseEntity.ttlMillis = (Long) accessToken.get("validTill");
                    return responseEntity;
                } else {
                    response.sendError(500, "Token creation failed");
                }
            }
        } catch (ExpiredJwtException e) {
            response.sendError(429, "Token is expired");
        } catch (SignatureException e) {
            response.sendError(403, "Can not validate token");
        } catch (MalformedJwtException e) {
            response.sendError(401, "Bad Request. Malformed JWT");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @GET
    @RequestMapping("/validate")
    public String isTokenValidated(@RequestHeader String token, HttpServletResponse response) throws BadRequestException, IOException {

        Claims claims = null;
        try {
            claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(API_SECRET))
                    .parseClaimsJws(token).getBody();
            System.out.println("ID: " + claims.getId());
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Issuer: " + claims.getIssuer());
            System.out.println("Person: " + claims.get("person"));
            System.out.println("Expiration: " + claims.getExpiration());
        } catch (ExpiredJwtException e) {
            response.sendError(429, "Token is expired");
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            response.sendError(403, "Can not validate token");
        }
        return token;
    }


    public static class TokenEntity {
        @JsonProperty(required = true)
        public String id;
        @JsonProperty(required = true)
        public String issuer;
        @JsonProperty(required = true)
        public String subject;
        @JsonProperty(required = false)
        public String userUid;
        @JsonProperty
        public Long ttlMillis;
    }
}
