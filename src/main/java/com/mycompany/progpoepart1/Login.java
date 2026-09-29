/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.progpoepart1;

import java.util.regex.Pattern;

/**
 *
 * @author Keabetswe Maisela
 */
public class Login {
    /**
    * properties to store user details
    */
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String cellPhone;
    private static final Pattern PHONE = Pattern.compile("^\\+27[6-8][0-9]{8}$");
    private boolean registered;
    private boolean loggedIn;
    private String loginUsername;
    private String loginPassword;
    
    public Login() { }
    
    public Login(String firstName, String lastName, String username, String password, String cellPhone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.cellPhone = cellPhone;
    }
     
    public void setFirstName(String value) { 
        firstName = value; 
    }
    
    public void setLastName(String value) { 
        lastName = value; 
    }
    
    public void setUsername(String value) { 
        username = value; 
    }
    
    public void setPassword(String value) { 
        password = value;
    }
    
    public void setCellPhone(String value) { 
        cellPhone = value;
    }
    
    public boolean checkUserName() {
        return username != null && username.length() <= 5 && !username.contains("_");
    }
    
    public boolean checkPasswordComplexity() {
        if (password == null || password.length() < 8) return false;
        boolean upper = false, digit = false, special = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) upper = true;
            if (Character.isDigit(c)) digit = true;
            if (!Character.isLetterOrDigit(c)) special = true;
        }
        return upper && digit && special;
    }
    
    /**
    * South African mobile format: +27 followed by a 9-digit mobile number without its leading zero.
    * Regex syntax reference: Oracle Java Pattern documentation:
    * docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html
    */
    public boolean checkCellPhoneNumber() {
        return cellPhone != null && PHONE.matcher(cellPhone).matches();
    }
    
    public String registerUser() {
        registered = false;
        loggedIn = false;
        
        if (!checkUserName()) 
            return "Username is not correctly formatted; please ensure that your username contains an underscore and is no more than five characters in length.";
        
        if (!checkPasswordComplexity()) 
            return "Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.";
        
        if (!checkCellPhoneNumber()) 
            return "Cell phone number incorrectly formatted or does not contain international code.";
        
        registered = true;
        
        return "User registered successfully.";
    }
    
    public boolean loginUser(String enteredUsername, String enteredPassword) {
        loggedIn = registered && username.equals(enteredUsername) && password.equals(enteredPassword);
        return loggedIn;
    }

    /** Prescribed no-argument method, using credentials previously supplied via setters. */
    public boolean loginUser() { 
        return loginUser(loginUsername, loginPassword);
    }
    
    public void setLoginCredentials(String username, String password) {
        this.loginUsername = username;
        this.loginPassword = password;
    }

    public String returnLoginStatus() {
        return loggedIn ? "Welcome " + firstName + ", " + lastName + " it is great to see you again."
                : "Username or password incorrect, please try again.";
    }
}
