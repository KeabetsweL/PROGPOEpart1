package com.mycompany.progpoepart1;

import java.util.regex.Pattern;

/**
 * Author : Keabetswe Mayisela
 * Handles registration and authentication.
 *
 * Part 1: validates the user's registration details.
 * Part 2: tracks successful login so messaging can be restricted
 * to authenticated users.
 */
public class Login {

    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String cellPhone;

    private boolean registered;
    private boolean loggedIn;

    private String loginUsername;
    private String loginPassword;

    /*
     * South African mobile number:
     * +27 followed by nine digits, starting with 6, 7 or 8.
     *
     * Preserves the validation used in the existing project.
     */
    private static final Pattern PHONE =
            Pattern.compile("^\\+27[6-8][0-9]{8}$");

    public Login() {
        // Allows the application to capture details using setters.
    }

    public Login(String firstName, String lastName,
                 String username, String password, String cellPhone) {
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

    /**
     * The username must contain an underscore and must
     * have no more than five characters.
     */
    public boolean checkUserName() {
        return username != null
                && username.length() <= 5
                && username.contains("_");
    }

    /**
     * Password requirements:
     * at least eight characters, one uppercase letter,
     * one number and one special character.
     */
    public boolean checkPasswordComplexity() {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasNumber = false;
        boolean hasSpecialCharacter = false;

        for (char character : password.toCharArray()) {
            if (Character.isUpperCase(character)) {
                hasUppercase = true;
            }

            if (Character.isDigit(character)) {
                hasNumber = true;
            }

            if (!Character.isLetterOrDigit(character)
                    && !Character.isWhitespace(character)) {
                hasSpecialCharacter = true;
            }
        }

        return hasUppercase && hasNumber && hasSpecialCharacter;
    }

    /**
     * Checks that the cellphone number includes the
     * international country code and a valid mobile format.
     */
    public boolean checkCellPhoneNumber() {
        return cellPhone != null
                && PHONE.matcher(cellPhone).matches();
    }

    /**
     * Registration succeeds only when all three validations pass.
     * Existing return messages are preserved for compatibility
     * with the project's current tests.
     */
    public String registerUser() {
        registered = false;
        loggedIn = false;

        if (!checkUserName()) {
            return "Username is not correctly formatted; please ensure "
                    + "that your username contains an underscore and "
                    + "is no more than five characters in length.";
        }

        if (!checkPasswordComplexity()) {
            return "Password is not correctly formatted; please ensure "
                    + "that the password contains at least eight characters, "
                    + "a capital letter, a number, and a special character.";
        }

        if (!checkCellPhoneNumber()) {
            return "Cell phone number incorrectly formatted or does "
                    + "not contain international code.";
        }

        registered = true;
        return "User registered successfully.";
    }

    /**
     * A user must register successfully before logging in.
     * Both supplied credentials must match the registered values.
     */
    public boolean loginUser(String enteredUsername,
                             String enteredPassword) {
        loggedIn = registered
                && username.equals(enteredUsername)
                && password.equals(enteredPassword);

        return loggedIn;
    }

    /**
     * Supports the prescribed no-argument login method.
     */
    public boolean loginUser() {
        return loginUser(loginUsername, loginPassword);
    }

    public void setLoginCredentials(String username, String password) {
        loginUsername = username;
        loginPassword = password;
    }

    public String returnLoginStatus() {
        if (loggedIn) {
            return "Welcome " + firstName + ", " + lastName
                    + " it is great to see you again.";
        }

        return "Username or password incorrect, please try again.";
    }

    /**
     * Part 2 requirement:
     * the application uses this result to control access
     * to the QuickChat messaging menu.
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }
}