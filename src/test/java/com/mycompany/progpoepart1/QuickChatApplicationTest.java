package com.mycompany.progpoepart1;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Author: keabetswe Mayisela
 * Tests the integrated Part 2 application.
 *
 * Simulated console input replaces manual typing.
 * Captured console output allows assertions against displayed messages.
 * Temporary storage prevents tests from changing real stored messages.
 */
public class QuickChatApplicationTest {

    @TempDir
    Path temporaryDirectory;

    /**
     * Valid registration input, followed by successful login.
     */
    private static final String VALID_LOGIN = String.join(
            "\n",
            "Kyle",
            "Smith",
            "kyl_1",
            "Ch&&sec@ke99!",
            "+27838968976",
            "kyl_1",
            "Ch&&sec@ke99!"
    ) + "\n";

    @Test
    void successfulLoginDisplaysWelcomeAndMenu() {
        String output = runApplication(
                VALID_LOGIN
                        + "2\n"   // Requested message count.
                        + "3\n"   // Quit.
        );

        assertTrue(output.contains("Welcome to QuickChat."));
        assertTrue(output.contains("1) Send Messages"));
        assertTrue(output.contains("2) Show recently sent messages"));
        assertTrue(output.contains("3) Quit"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void failedLoginPreventsAccessToMessaging() {
        String input = String.join(
                "\n",
                "Kyle",
                "Smith",
                "kyl_1",
                "Ch&&sec@ke99!",
                "+27838968976",
                "kyl_1",
                "incorrect-password"
        ) + "\n";

        String output = runApplication(input);

        assertTrue(output.contains(
                "Username or password incorrect, please try again."
        ));

        assertFalse(output.contains("Welcome to QuickChat."));
        assertFalse(output.contains("1) Send Messages"));
        assertFalse(output.contains(
                "How many messages would you like to enter?"
        ));
    }

    @Test
    void optionTwoDisplaysComingSoonAndReturnsToMenu() {
        String output = runApplication(
                VALID_LOGIN
                        + "1\n"
                        + "2\n"   // Coming Soon.
                        + "3\n"   // Quit after returning to menu.
        );

        assertTrue(output.contains("Coming Soon."));
        assertEquals(2, occurrences(output, "1) Send Messages"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void quitExitsWithoutRequestingMessageDetails() {
        String output = runApplication(
                VALID_LOGIN
                        + "2\n"
                        + "3\n"
        );

        assertTrue(output.contains("Total messages sent: 0"));
        assertTrue(output.contains("Goodbye."));
        assertFalse(output.contains("Recipient (+27...):"));
    }

    @Test
    void prescribedTwoMessageScenarioSendsOnlyFirstMessage() {
        String output = runApplication(
                VALID_LOGIN
                        + "2\n"   // Enter two messages.
                        + "1\n"   // Main menu: Send Messages.
                        + "+27718693002\n"
                        + "Hi Mike, can you join us for dinner tonight?\n"
                        + "1\n"   // Send first message.
                        + "08575975889\n"
                        + "Hi Keegan, did you receive the payment?\n"
                        + "2\n"   // Disregard second message.
                        + "0\n"   // Confirm.
                        + "3\n"   // Quit.
        );

        assertTrue(output.contains("Message 1 of 2"));
        assertTrue(output.contains("Message 2 of 2"));

        assertEquals(
                1,
                occurrences(output, "Message successfully sent.")
        );

        assertTrue(output.contains("Press 0 to delete the message."));
        assertTrue(output.contains("Message disregarded."));
        assertTrue(output.contains("Total messages sent: 1"));

        assertTrue(output.contains(
                "Message: Hi Mike, can you join us for dinner tonight?"
        ));

        assertFalse(output.contains(
                "Message: Hi Keegan, did you receive the payment?"
        ));

        // Two random ID digits, zero-based number, uppercase words.
        assertTrue(output.matches(
                "(?s).*Message Hash: [0-9]{2}:0:HITONIGHT.*"
        ));
    }

    @Test
    void sentDetailsAppearInRequiredOrder() {
        String output = runApplication(
                VALID_LOGIN
                        + "1\n"
                        + "1\n"
                        + "+27718693002\n"
                        + "Hi Mike, can you join us for dinner tonight?\n"
                        + "1\n"
                        + "3\n"
        );

        String newline = System.lineSeparator();

        String expectedPattern =
                "(?s).*Message ID: [0-9]{10}"
                        + newline
                        + "Message Hash: [0-9]{2}:0:HITONIGHT"
                        + newline
                        + "Recipient: \\+27718693002"
                        + newline
                        + "Message: Hi Mike, can you join us for dinner tonight\\?"
                        + ".*";

        assertTrue(
                output.matches(expectedPattern),
                "Details must appear as ID, hash, recipient, then message."
        );
    }

    @Test
    void selectingSendAgainCannotExceedRequestedCount() {
        String output = runApplication(
                VALID_LOGIN
                        + "1\n"
                        + "1\n"
                        + "+27718693002\n"
                        + "Hello Mike\n"
                        + "1\n"
                        + "1\n"   // Try entering another batch.
                        + "3\n"
        );

        assertTrue(output.contains(
                "You have already entered the requested number of messages."
        ));

        assertEquals(1, occurrences(output, "Recipient (+27...):"));
        assertEquals(1, occurrences(output, "Message successfully sent."));
        assertTrue(output.contains("Total messages sent: 1"));
    }

    @Test
    void messageNumbersIncreaseWithForLoop() {
        String output = runApplication(
                VALID_LOGIN
                        + "2\n"
                        + "1\n"
                        + "+27718693002\n"
                        + "Hello Mike\n"
                        + "1\n"
                        + "+27718693002\n"
                        + "Hello Keegan\n"
                        + "1\n"
                        + "3\n"
        );

        assertTrue(output.matches(
                "(?s).*Message Hash: [0-9]{2}:0:HELLOMIKE.*"
        ));

        assertTrue(output.matches(
                "(?s).*Message Hash: [0-9]{2}:1:HELLOKEEGAN.*"
        ));

        assertTrue(output.contains("Total messages sent: 2"));
    }

    @Test
    void invalidNumericInputIsRejectedAndRetried() {
        String output = runApplication(
                VALID_LOGIN
                        + "abc\n" // Invalid requested count.
                        + "0\n"   // Count must be positive.
                        + "1\n"   // Valid count.
                        + "hello\n" // Invalid menu input.
                        + "9\n"   // Out-of-range menu number.
                        + "3\n"
        );

        assertEquals(
                2,
                occurrences(
                        output,
                        "Please enter a whole number from 1 to 2147483647."
                )
        );

        assertEquals(
                2,
                occurrences(
                        output,
                        "Please enter a whole number from 1 to 4."
                )
        );

        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void invalidRecipientCanBeCorrectedWithinSameMessageSlot() {
        String output = runApplication(
                VALID_LOGIN
                        + "1\n"
                        + "1\n"
                        + "08575975889\n"
                        + "Hello Mike\n"
                        + "1\n"   // Attempt to send invalid recipient.
                        + "1\n"   // Correct details.
                        + "+27718693002\n"
                        + "Hello Mike\n"
                        + "1\n"   // Send corrected message.
                        + "3\n"
        );

        assertTrue(output.contains(
                "Cell phone number is incorrectly formatted or does "
                        + "not contain an international code. Please correct "
                        + "the number and try again."
        ));

        assertEquals(1, occurrences(output, "Message 1 of 1"));
        assertEquals(1, occurrences(output, "Message successfully sent."));
        assertTrue(output.contains("Total messages sent: 1"));
    }

    @Test
    void overlengthMessageCanBeCorrectedBeforeSending() {
        String output = runApplication(
                VALID_LOGIN
                        + "1\n"
                        + "1\n"
                        + "+27718693002\n"
                        + "A".repeat(251) + "\n"
                        + "1\n"   // Attempt to send.
                        + "1\n"   // Correct details.
                        + "+27718693002\n"
                        + "Hello Mike\n"
                        + "1\n"
                        + "3\n"
        );

        assertTrue(output.contains(
                "Message exceeds 250 characters by 1; please reduce the size."
        ));

        assertEquals(1, occurrences(output, "Message successfully sent."));
        assertTrue(output.contains("Total messages sent: 1"));
    }

    @Test
    void storeActionCreatesJsonWithoutIncreasingSentTotal()
            throws Exception {

        String output = runApplication(
                VALID_LOGIN
                        + "1\n"
                        + "1\n"
                        + "+27718693002\n"
                        + "Hi Mike, can you join us for dinner tonight?\n"
                        + "3\n"   // Store message.
                        + "3\n"   // Quit main menu.
        );

        assertTrue(output.contains("Message successfully stored."));
        assertTrue(output.contains("Total messages sent: 0"));

        try (var files = Files.list(temporaryDirectory)) {
            Path[] storedFiles = files
                    .filter(path -> path.toString().endsWith(".json"))
                    .toArray(Path[]::new);

            assertEquals(1, storedFiles.length);

            String json = Files.readString(
                    storedFiles[0],
                    StandardCharsets.UTF_8
            );

            assertTrue(json.contains("\"recipient\": \"+27718693002\""));
            assertTrue(json.contains(
                    "\"message\": \"Hi Mike, can you join us "
                            + "for dinner tonight?\""
            ));
        }
    }

    /**
     * Runs the actual application with supplied console input.
     * Exhausting input unexpectedly causes the test to fail.
     */
    private String runApplication(String input) {
        ByteArrayOutputStream capturedOutput =
                new ByteArrayOutputStream();

        try (Scanner scanner = new Scanner(input);
             PrintStream output = new PrintStream(
                     capturedOutput,
                     true,
                     StandardCharsets.UTF_8)) {

            PROGPOEpart1.run(
                    scanner,
                    output,
                    new Message(temporaryDirectory)
            );
        }

        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    /**
     * Counts exact output occurrences without interpreting regex syntax.
     */
    private int occurrences(String text, String target) {
        int count = 0;
        int position = 0;

        while ((position = text.indexOf(target, position)) >= 0) {
            count++;
            position += target.length();
        }

        return count;
    }
}