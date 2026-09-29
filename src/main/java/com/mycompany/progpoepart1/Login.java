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
    
    public Login() { }
     
    public void setFirstName(String value) { firstName = value; }
    public void setLastName(String value) { lastName = value; }
    public void setUsername(String value) { username = value; }
    public void setPassword(String value) { password = value; }
    public void setCellPhone(String value) { cellPhone = value; }
    private static final Pattern PHONE = Pattern.compile("^\\+27[6-8][0-9]{8}$");
    
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
    * https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html
    */
    public boolean checkCellPhoneNumber() {
        return cellPhone != null && PHONE.matcher(cellPhone).matches();
    }
}
