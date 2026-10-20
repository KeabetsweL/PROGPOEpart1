package com.mycompany.progpoepart1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests registration and authentication.
 *
 * Part 1: validates the prescribed registration test data.
 * Part 2, requirement 1: verifies the login state that the
 * messaging menu will use to control access.
 *
 * Each test receives a fresh Login object so tests do not
 * depend on execution order or another test's state.
 */
public class LoginTest {

    private Login login;

    @BeforeEach
    void setUp() {
        login = new Login(
                "Kyle",
                "Smith",
                "kyl_1",
                "Ch&&sec@ke99!",
                "+27838968976"
        );
    }

    @Test
    @DisplayName("Username containing an underscore and at most five characters is valid")
    void acceptsPrescribedValidUsername() {
        assertTrue(login.checkUserName());
    }

    @Test
    @DisplayName("The prescribed invalid username is rejected")
    void rejectsPrescribedInvalidUsername() {
        login.setUsername("kyle!!!!!!!");

        assertFalse(login.checkUserName());

        assertEquals(
                "Username is not correctly formatted; please ensure "
                        + "that your username contains an underscore and "
                        + "is no more than five characters in length.",
                login.registerUser()
        );
    }

    @Test
    @DisplayName("A short username still requires an underscore")
    void rejectsUsernameWithoutUnderscore() {
        login.setUsername("kyle");

        // Checks the underscore rule independently of the length rule.
        assertFalse(login.checkUserName());
    }

    @Test
    @DisplayName("An underscore does not make an overlength username valid")
    void rejectsUsernameLongerThanFiveCharacters() {
        login.setUsername("kyle_1");

        // Six characters: the underscore is present, but the length fails.
        assertFalse(login.checkUserName());
    }

    @Test
    @DisplayName("The prescribed complex password is accepted")
    void acceptsPrescribedValidPassword() {
        assertTrue(login.checkPasswordComplexity());
    }

    @Test
    @DisplayName("The prescribed weak password is rejected")
    void rejectsPrescribedInvalidPassword() {
        login.setPassword("password");

        assertFalse(login.checkPasswordComplexity());

        assertEquals(
                "Password is not correctly formatted; please ensure "
                        + "that the password contains at least eight characters, "
                        + "a capital letter, a number, and a special character.",
                login.registerUser()
        );
    }

    @Test
    @DisplayName("A password must contain at least eight characters")
    void rejectsPasswordShorterThanEightCharacters() {
        // Contains uppercase, number and special character, but is too short.
        login.setPassword("Ab1!xyz");

        assertFalse(login.checkPasswordComplexity());
    }

    @Test
    @DisplayName("A password must contain an uppercase letter")
    void rejectsPasswordWithoutUppercase() {
        login.setPassword("abcdef1!");

        assertFalse(login.checkPasswordComplexity());
    }

    @Test
    @DisplayName("A password must contain a number")
    void rejectsPasswordWithoutNumber() {
        login.setPassword("Abcdefg!");

        assertFalse(login.checkPasswordComplexity());
    }

    @Test
    @DisplayName("A password must contain a special character")
    void rejectsPasswordWithoutSpecialCharacter() {
        login.setPassword("Abcdefg1");

        assertFalse(login.checkPasswordComplexity());
    }

    @Test
    @DisplayName("The prescribed international cellphone number is valid")
    void acceptsPrescribedValidCellphone() {
        assertTrue(login.checkCellPhoneNumber());
    }

    @Test
    @DisplayName("The prescribed incorrectly formatted cellphone is rejected")
    void rejectsPrescribedInvalidCellphone() {
        login.setCellPhone("08966553");

        assertFalse(login.checkCellPhoneNumber());

        assertEquals(
                "Cell phone number incorrectly formatted or does "
                        + "not contain international code.",
                login.registerUser()
        );
    }

    @Test
    @DisplayName("Valid registration returns the expected confirmation")
    void registersValidUser() {
        assertEquals(
                "User registered successfully.",
                login.registerUser()
        );

        // Registration alone must not grant access to messaging.
        assertFalse(login.isLoggedIn());
    }

    @Test
    @DisplayName("Part 2: a new user has no authenticated messaging access")
    void startsLoggedOut() {
        assertFalse(login.isLoggedIn());
    }

    @Test
    @DisplayName("Part 2: correct credentials cannot bypass registration")
    void cannotLoginBeforeRegistration() {
        assertFalse(login.loginUser("kyl_1", "Ch&&sec@ke99!"));
        assertFalse(login.isLoggedIn());
    }

    @Test
    @DisplayName("Part 2: successful login enables authenticated access")
    void successfulLoginSetsLoggedInState() {
        login.registerUser();

        assertTrue(login.loginUser("kyl_1", "Ch&&sec@ke99!"));
        assertTrue(login.isLoggedIn());

        assertEquals(
                "Welcome Kyle, Smith it is great to see you again.",
                login.returnLoginStatus()
        );
    }

    @Test
    @DisplayName("An incorrect password prevents authenticated access")
    void incorrectPasswordKeepsUserLoggedOut() {
        login.registerUser();

        assertFalse(login.loginUser("kyl_1", "WrongPassword1!"));
        assertFalse(login.isLoggedIn());

        assertEquals(
                "Username or password incorrect, please try again.",
                login.returnLoginStatus()
        );
    }

    @Test
    @DisplayName("An incorrect username prevents authenticated access")
    void incorrectUsernameKeepsUserLoggedOut() {
        login.registerUser();

        assertFalse(login.loginUser("other", "Ch&&sec@ke99!"));
        assertFalse(login.isLoggedIn());
    }

    @Test
    @DisplayName("The prescribed no-argument login method authenticates correctly")
    void authenticatesUsingPreviouslySuppliedCredentials() {
        login.registerUser();
        login.setLoginCredentials("kyl_1", "Ch&&sec@ke99!");

        assertTrue(login.loginUser());
        assertTrue(login.isLoggedIn());
    }

    @Test
    @DisplayName("An unsuccessful login attempt clears a previous successful login")
    void failedLoginClearsPreviousAuthenticatedState() {
        login.registerUser();
        assertTrue(login.loginUser("kyl_1", "Ch&&sec@ke99!"));

        assertFalse(login.loginUser("kyl_1", "wrong"));
        assertFalse(login.isLoggedIn());
    }

    @Test
    @DisplayName("Failed registration cannot be followed by successful login")
    void invalidRegistrationPreventsLogin() {
        login.setUsername("kyle");
        login.registerUser();

        assertFalse(login.loginUser("kyle", "Ch&&sec@ke99!"));
        assertFalse(login.isLoggedIn());
    }
}