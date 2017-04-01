package com.demo.pojo;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User
{
    private String userName;
    private String password;
    private Date createdOn;
    private String role;
    private String userUid;
    private List<AccessToken> tokens;

    private Person person;

    public User()
    {
        this.person = new Person();
        this.createdOn = new Date();
        this.tokens = new ArrayList<>();
    }

    public User(String userName, String password) {
        this();
        this.userName = userName;
        this.password = password;
    }

    public User(String userName, String password, String firstName, String lastName, String email)
    {
        this(userName, password);
        this.person = new Person(firstName, lastName, email);
    }

    public Person getPerson() {
        return person;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public List<AccessToken> getTokens() {
        return tokens;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    @Override
    public String toString() {
        return "User{" +
                "userUid='" + userUid + '\'' +
                ", role='" + role + '\'' +
                ", createdOn=" + createdOn +
                ", password='" + password + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}
