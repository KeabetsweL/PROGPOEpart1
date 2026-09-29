package com.mycompany.progpoepart1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PROGPOEpart1Test {

    private Login valid() {
        return new Login("Kyle", "Smith", "kyl_1", "Ch&&sec@ke99!", "+27838968976");
    }

    @Test void usernameValid() { assertTrue(valid().checkUserName()); }

    @Test void usernameInvalid() {
        Login login = valid();
        login.setUsername("kyle!!!!!!!");
        assertFalse(login.checkUserName());
        assertEquals("Username is not correctly formatted; please ensure that your username contains an underscore and is no more than five characters in length.",
                login.registerUser());
    }

    @Test void passwordValid() { assertTrue(valid().checkPasswordComplexity()); }

    @Test void passwordInvalid() {
        Login login = valid();
        login.setPassword("password");
        assertFalse(login.checkPasswordComplexity());
        assertEquals("Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.",
                login.registerUser());
    }

    @Test void cellphoneValid() { assertTrue(valid().checkCellPhoneNumber()); }

    @Test void cellphoneInvalid() {
        Login login = valid();
        login.setCellPhone("08966553");
        assertFalse(login.checkCellPhoneNumber());
        assertEquals("Cell phone number incorrectly formatted or does not contain international code.",
                login.registerUser());
    }

    @Test void registrationAndSuccessfulLogin() {
        Login login = valid();
        assertEquals("User registered successfully.", login.registerUser());
        login.setLoginCredentials("kyl_1", "Ch&&sec@ke99!");
        assertTrue(login.loginUser());
        assertEquals("Welcome Kyle, Smith it is great to see you again.", login.returnLoginStatus());
    }

    @Test void failedLogin() {
        Login login = valid();
        login.registerUser();
        assertFalse(login.loginUser("kyl_1", "wrong"));
        assertEquals("Username or password incorrect, please try again.", login.returnLoginStatus());
    }

    @Test void cannotLoginBeforeRegistration() {
        assertFalse(valid().loginUser("kyl_1", "Ch&&sec@ke99!"));
    }
}