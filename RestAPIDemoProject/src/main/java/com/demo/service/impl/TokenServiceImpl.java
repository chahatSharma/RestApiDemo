package com.demo.service.impl;

import com.demo.controller.AccessTokenController;
import com.demo.pojo.AccessToken;
import com.demo.service.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;

@Service
public class TokenServiceImpl
        implements TokenService {

    private static final String TOKEN_PREFIX = "token" + "__";
    private static final String API_SECRET = "aap1212";
    private static final String ISSUER = "DEMO.INC";
    private static final String URL = "http://www.example.com";
    private String TOKEN_COUNT = "TOKEN_COUNT";
    private Log log = LogFactory.getLog(TokenServiceImpl.class);

    @Override
    public AccessToken createAccessToken(AccessTokenController.TokenEntity tokenEntity, String userUid) {
        return null;
    }

    @Override
    public Boolean isTokenValidated(String tokenBody, String userUid) throws ExpiredJwtException, SignatureException, MalformedJwtException {
        log.info("Validating token");
        System.out.println("tokenBody"+ tokenBody);
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(API_SECRET))
                    .parseClaimsJws(tokenBody).getBody();
            log.info("Token validated for user: " + claims.get("user"));
            log.debug("Token Validation Finished");
            System.out.println("inside istokenvalidated");
            return Boolean.TRUE;
        } catch (UnsupportedJwtException e) {
            log.error(e);
            return Boolean.FALSE;
        } catch (IllegalArgumentException e) {
            log.error(e);
            return Boolean.FALSE;
        }
    }

    @Override
    public String getUserIdFromToken(String tokenBody) throws UnsupportedJwtException, IllegalArgumentException {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(API_SECRET))
                    .parseClaimsJws(tokenBody).getBody();
            return (String) claims.get("user");
        } catch (UnsupportedJwtException e) {
            log.error(e);
            throw new UnsupportedJwtException("Getting userId from token failed. Unsupported JWT");
        } catch (IllegalArgumentException e) {
            log.error(e);
            throw new IllegalArgumentException("Failed while getting userId from token. Illegal argument exception");
        }
    }

    @Override
    public AccessToken createAccessTokenAPI(String userUid, String role, String subject) throws JsonProcessingException {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        Jedis jedis = getJedis();

        JwtBuilder builder = getJwtBuilder(userUid, role, subject, now, jedis.get(TOKEN_COUNT));
        // String key = userUid.split("__", 2)[1];
        String tokenId = TOKEN_PREFIX + jedis.get(TOKEN_COUNT);
        AccessToken token = new AccessToken(tokenId, ISSUER, getNextYearDate(), URL, role, builder.compact());
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            jedis.set(tokenId, mapper.writeValueAsString(token));
        } finally {
            jedis.close();
        }
        return token;
    }

    @Override
    public JSONObject createAccessToken(String userUid, String role, String subject) throws JsonProcessingException, ParseException {
        AccessToken accessToken = createAccessTokenAPI(userUid, role, subject);
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(mapper.writeValueAsString(accessToken));
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(API_SECRET))
                    .parseClaimsJws(token).getBody();
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.userUid = (String) claims.get("user");
            tokenInfo.role = (String) claims.get("role");
            tokenInfo.issuer = ISSUER;
            tokenInfo.tokenId = token;
            tokenInfo.tokenUid = claims.getId();
            return tokenInfo;
        } catch (UnsupportedJwtException e) {
            log.error(e);
            throw new UnsupportedJwtException("Getting userId from token failed. Unsupported JWT");
        } catch (IllegalArgumentException e) {
            log.error(e);
            throw new IllegalArgumentException("Failed while getting userId from token. Illegal argument exception");
        }
    }

    private Date getNextYearDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);
        return cal.getTime();
    }

    private Jedis getJedis() {
        Jedis jedis = new Jedis("localhost");
        jedis.incr(TOKEN_COUNT);
        return jedis;
    }

    private JwtBuilder getJwtBuilder(String userUid, String role, String subject, Date issuingDate, String Id) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(API_SECRET);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        JwtBuilder builder = Jwts.builder().setId(Id)
                .setIssuedAt(issuingDate)
                .setSubject(subject)
                .setIssuer(ISSUER)
                .claim("user", userUid)
                .claim("url", URL)
                .claim("role", role)
                .signWith(signatureAlgorithm, signingKey);

        builder.setExpiration(getNextYearDate());
        return builder;
    }
}
