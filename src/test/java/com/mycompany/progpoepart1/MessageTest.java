package com.mycompany.progpoepart1;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Author: Keabetswe Mayisela
 * Part 2 tests for the Message class.
 *
 * Covers the prescribed test data and expected responses:
 * - Message length success and failure.
 * - Recipient validation success and failure.
 * - Message hash construction, including multiple cases in a loop.
 * - Random ten-digit message IDs.
 * - Send, disregard and store actions.
 * - Sent-message totals and displayed details.
 * - Actual JSON file storage.
 *
 * No test opens a dialog or requires keyboard input.
 */
public class MessageTest {

    private static final String VALID_RECIPIENT = "+27718693002";

    private static final String INVALID_RECIPIENT = "08575975889";

    private static final String FIRST_MESSAGE =
            "Hi Mike, can you join us for dinner tonight?";

    private static final String SECOND_MESSAGE =
            "Hi Keegan, did you receive the payment?";

    /*
     * JUnit creates a separate temporary directory for each test.
     * Test files therefore do not affect real stored messages.
     */
    @TempDir
    Path temporaryDirectory;

    private Message messages;

    @BeforeEach
    void setUp() {
        messages = new Message(temporaryDirectory);
    }

    @Test
    @DisplayName("Prescribed message within 250 characters is ready to send")
    void acceptsPrescribedMessageLength() {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        assertEquals(
                "Message ready to send.",
                messages.checkMessageLength()
        );
    }

    @Test
    @DisplayName("Exactly 250 characters is accepted")
    void acceptsExactly250Characters() {
        messages.startMessage(
                0,
                VALID_RECIPIENT,
                "A".repeat(250)
        );

        assertEquals(
                "Message ready to send.",
                messages.checkMessageLength()
        );
    }

    @Test
    @DisplayName("251 characters reports an excess of one")
    void rejects251Characters() {
        messages.startMessage(
                0,
                VALID_RECIPIENT,
                "A".repeat(251)
        );

        assertEquals(
                "Message exceeds 250 characters by 1; please reduce the size.",
                messages.checkMessageLength()
        );
    }

    @Test
    @DisplayName("An overlength message reports the actual excess")
    void reportsActualExcessLength() {
        messages.startMessage(
                0,
                VALID_RECIPIENT,
                "A".repeat(275)
        );

        assertEquals(
                "Message exceeds 250 characters by 25; please reduce the size.",
                messages.checkMessageLength()
        );

        // Invalid messages must not increase the sent total.
        assertEquals(
                "Message exceeds 250 characters by 25; please reduce the size.",
                messages.SentMessage(1)
        );

        assertEquals(0, messages.returnTotalMessagess());
    }

    @Test
    @DisplayName("Blank and missing messages cannot be sent")
    void rejectsBlankAndMissingMessages() {
        String[] invalidMessages = {"", "   ", null};

        for (String text : invalidMessages) {
            messages.startMessage(0, VALID_RECIPIENT, text);

            assertEquals(
                    "Please enter a message.",
                    messages.checkMessageLength()
            );

            assertEquals(
                    "Please enter a message.",
                    messages.SentMessage(1)
            );
        }

        assertEquals(0, messages.returnTotalMessagess());
    }

    @Test
    @DisplayName("Prescribed valid recipient is successfully captured")
    void acceptsPrescribedRecipient() {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        assertEquals(
                "Cell phone number successfully captured.",
                messages.checkRecipientCell()
        );
    }

    @Test
    @DisplayName("Prescribed invalid recipient returns the exact error")
    void rejectsPrescribedRecipient() {
        messages.startMessage(1, INVALID_RECIPIENT, SECOND_MESSAGE);

        String expected =
                "Cell phone number is incorrectly formatted or does "
                        + "not contain an international code. Please correct "
                        + "the number and try again.";

        assertEquals(expected, messages.checkRecipientCell());

        // An invalid recipient must also prevent sending.
        assertEquals(expected, messages.SentMessage(1));
        assertEquals(0, messages.returnTotalMessagess());
    }

    @Test
    @DisplayName("Generated IDs contain ten digits and differ within a session")
    void generatesTenDigitMessageIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();

        for (int number = 0; number < 100; number++) {
            messages.startMessage(
                    number,
                    VALID_RECIPIENT,
                    FIRST_MESSAGE
            );

            String id = messages.getMessageID();

            assertNotNull(id);
            assertEquals(10, id.length());
            assertTrue(id.matches("[0-9]{10}"));
            assertTrue(messages.checkMessageID());

            assertTrue(
                    ids.add(id),
                    "Each generated ID must be unique within this session."
            );

            assertEquals(number, messages.getMessageNumber());
        }
    }

    @Test
    @DisplayName("Message ID validation rejects overlength and non-digit IDs")
    void validatesMessageIdFormat() throws ReflectiveOperationException {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        String[] invalidIds = {
                "12345678901",
                "12345ABCDE",
                "",
                null
        };

        for (String id : invalidIds) {
            setMessageIdForTest(id);
            assertFalse(messages.checkMessageID());
        }

        setMessageIdForTest("0012345678");
        assertTrue(messages.checkMessageID());
    }

    @Test
    @DisplayName("Prescribed first message produces 00:0:HITONIGHT")
    void createsExactPrescribedHash() throws ReflectiveOperationException {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);
        setMessageIdForTest("0012345678");

        assertEquals(
                "00:0:HITONIGHT",
                messages.createMessageHash()
        );
    }

    @Test
    @DisplayName("Remaining message hashes are checked in a loop")
    void createsMultipleHashesInLoop() throws ReflectiveOperationException {
        String[][] cases = {
                {
                        "0012345678",
                        FIRST_MESSAGE,
                        "00:0:HITONIGHT"
                },
                {
                        "1234567890",
                        SECOND_MESSAGE,
                        "12:1:HIPAYMENT"
                },
                {
                        "9876543210",
                        "Please call me tomorrow.",
                        "98:2:PLEASETOMORROW"
                },
                {
                        "4500000000",
                        "  Hello   there!  ",
                        "45:3:HELLOTHERE"
                }
        };

        for (int number = 0; number < cases.length; number++) {
            messages.startMessage(
                    number,
                    VALID_RECIPIENT,
                    cases[number][1]
            );

            setMessageIdForTest(cases[number][0]);

            assertEquals(
                    cases[number][2],
                    messages.createMessageHash(),
                    "Incorrect hash for test case " + number
            );
        }
    }

    @Test
    @DisplayName("Send returns the exact confirmation and increments the total")
    void sendsMessageSuccessfully() {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        assertEquals(
                "Message successfully sent.",
                messages.SentMessage(1)
        );

        assertEquals(1, messages.returnTotalMessagess());
    }

    @Test
    @DisplayName("Disregard returns the prescribed response without sending")
    void disregardsPrescribedSecondMessage() {
        /*
         * The second prescribed recipient is invalid.
         * Disregarding must still be possible because the message
         * is being abandoned rather than sent.
         */
        messages.startMessage(1, INVALID_RECIPIENT, SECOND_MESSAGE);

        assertEquals(
                "Press 0 to delete the message.",
                messages.SentMessage(2)
        );

        assertEquals(0, messages.returnTotalMessagess());
        assertEquals("", messages.printMessages());
    }

    @Test
    @DisplayName("Store writes the complete expected JSON without counting as sent")
    void storesMessageAsJson() throws IOException,
            ReflectiveOperationException {

        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);
        setMessageIdForTest("0012345678");

        assertEquals(
                "Message successfully stored.",
                messages.SentMessage(3)
        );

        Path storedFile = temporaryDirectory.resolve("0012345678.json");

        assertTrue(Files.isRegularFile(storedFile));

        String expectedJson =
                "{\n"
                        + "  \"messageID\": \"0012345678\",\n"
                        + "  \"messageNumber\": 0,\n"
                        + "  \"messageHash\": \"00:0:HITONIGHT\",\n"
                        + "  \"recipient\": \"+27718693002\",\n"
                        + "  \"message\": \"" + FIRST_MESSAGE + "\"\n"
                        + "}\n";

        assertEquals(
                expectedJson,
                Files.readString(storedFile, StandardCharsets.UTF_8)
        );

        assertEquals(0, messages.returnTotalMessagess());
        assertEquals("", messages.printMessages());
    }

    @Test
    @DisplayName("Sent details appear in the exact required order")
    void printsDetailsInRequiredOrder()
            throws ReflectiveOperationException {

        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);
        setMessageIdForTest("0012345678");

        messages.SentMessage(1);

        String expected =
                "Message ID: 0012345678"
                        + System.lineSeparator()
                        + "Message Hash: 00:0:HITONIGHT"
                        + System.lineSeparator()
                        + "Recipient: +27718693002"
                        + System.lineSeparator()
                        + "Message: " + FIRST_MESSAGE;

        assertEquals(expected, messages.currentMessageDetails());
        assertEquals(expected, messages.printMessages());
    }

    @Test
    @DisplayName("Prescribed two-message scenario counts only the sent message")
    void prescribedScenarioHasOneSentMessage() {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        assertEquals(
                "Message successfully sent.",
                messages.SentMessage(1)
        );

        messages.startMessage(1, INVALID_RECIPIENT, SECOND_MESSAGE);

        assertEquals(
                "Press 0 to delete the message.",
                messages.SentMessage(2)
        );

        assertEquals(1, messages.returnTotalMessagess());

        assertTrue(messages.printMessages().contains(FIRST_MESSAGE));
        assertFalse(messages.printMessages().contains(SECOND_MESSAGE));
    }

    @Test
    @DisplayName("The session report retains multiple sent messages")
    void printsAllSentMessages() {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);
        messages.SentMessage(1);
        String firstDetails = messages.currentMessageDetails();

        messages.startMessage(1, VALID_RECIPIENT, SECOND_MESSAGE);
        messages.SentMessage(1);
        String secondDetails = messages.currentMessageDetails();

        assertEquals(2, messages.returnTotalMessagess());

        assertEquals(
                firstDetails
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + secondDetails,
                messages.printMessages()
        );
    }

    @Test
    @DisplayName("The same message cannot be counted twice")
    void preventsDuplicateSending() {
        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        assertEquals(
                "Message successfully sent.",
                messages.SentMessage(1)
        );

        assertEquals(
                "This message has already been processed.",
                messages.SentMessage(1)
        );

        assertEquals(1, messages.returnTotalMessagess());
    }

    @Test
    @DisplayName("JSON storage correctly escapes quotes, backslashes and newlines")
    void escapesJsonSpecialCharacters() throws IOException,
            ReflectiveOperationException {

        messages.startMessage(
                0,
                VALID_RECIPIENT,
                "Hi \"Mike\"\nUse C:\\Docs"
        );

        setMessageIdForTest("0012345678");

        Path storedFile = messages.storeMessage();

        String expectedJson =
                "{\n"
                        + "  \"messageID\": \"0012345678\",\n"
                        + "  \"messageNumber\": 0,\n"
                        + "  \"messageHash\": \"00:0:HICDOCS\",\n"
                        + "  \"recipient\": \"+27718693002\",\n"
                        + "  \"message\": \"Hi \\\"Mike\\\"\\nUse C:\\\\Docs\"\n"
                        + "}\n";

        assertEquals(
                expectedJson,
                Files.readString(storedFile, StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("A storage failure does not produce a success confirmation")
    void reportsStorageFailure() throws IOException {
        /*
         * A regular file cannot also be a storage directory.
         * This produces a portable failure without relying on
         * operating-system-specific file permissions.
         */
        Path regularFile = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(regularFile, "Existing file");

        Message blockedStorage = new Message(regularFile);
        blockedStorage.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        assertEquals(
                "Message could not be stored. Please try again.",
                blockedStorage.SentMessage(3)
        );

        assertEquals(0, blockedStorage.returnTotalMessagess());

        // Storage failure must leave the message available for sending.
        assertEquals(
                "Message successfully sent.",
                blockedStorage.SentMessage(1)
        );

        assertEquals(1, blockedStorage.returnTotalMessagess());
    }

    @Test
    @DisplayName("Invalid action and missing message do not change the total")
    void rejectsInvalidActionAndMissingMessage() {
        assertEquals(
                "Please create a message first.",
                messages.SentMessage(1)
        );

        messages.startMessage(0, VALID_RECIPIENT, FIRST_MESSAGE);

        assertEquals(
                "Invalid option. Please choose 1, 2 or 3.",
                messages.SentMessage(4)
        );

        assertEquals(0, messages.returnTotalMessagess());

        // Invalid menu input must not consume the current message.
        assertEquals(
                "Message successfully sent.",
                messages.SentMessage(1)
        );
    }

    /**
     * Supplies a predictable ID for the assignment's exact hash examples.
     *
     * Reflection is limited to test code. It allows deterministic
     * assertions without adding a public ID-changing method to Message.
     */
    private void setMessageIdForTest(String value)
            throws ReflectiveOperationException {

        Field field = Message.class.getDeclaredField("messageID");
        field.setAccessible(true);
        field.set(messages, value);
    }
}