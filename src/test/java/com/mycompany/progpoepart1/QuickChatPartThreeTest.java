package com.mycompany.progpoepart1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the Part 3 application flow.
 *
 * Tests invoke the actual application's run() method.
 * Console input is simulated and output is captured.
 * Each test uses an isolated temporary storage directory.
 */
public class QuickChatPartThreeTest {

    private static final String LOGIN = String.join(
            "\n",
            "Kyle",
            "Smith",
            "kyl_1",
            "Ch&&sec@ke99!",
            "+27838968976",
            "kyl_1",
            "Ch&&sec@ke99!"
    ) + "\n";

    @TempDir
    Path temporaryDirectory;

    private MessageRecord sampleStoredMessage() {
        return new MessageRecord(
                "1123456789",
                1,
                "+27838968976",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                MessageRecord.Status.STORED
        );
    }

    @Test
    void loadsSavedMessagesAndDisplaysStoredReport() throws IOException {
        MessageRecord stored = sampleStoredMessage();
        new MessageJsonStore(temporaryDirectory).save(stored);

        String output = runApplication(
                LOGIN
                        + "1\n"   // Requested message count.
                        + "4\n"   // Stored Messages.
                        + "6\n"   // Full stored report.
                        + "0\n"   // Return.
                        + "3\n"   // Quit.
        );

        assertTrue(output.contains("Stored messages loaded: 1"));
        assertTrue(output.contains("4) Stored Messages"));
        assertTrue(output.contains(stored.fullDetails()));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void emptyStorageStillAllowsSubmenuAccess() {
        String output = runApplication(
                LOGIN
                        + "1\n"
                        + "4\n"
                        + "6\n"
                        + "0\n"
                        + "3\n"
        );

        assertTrue(output.contains("Stored messages loaded: 0"));
        assertTrue(output.contains("No stored messages."));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void newlySentMessageAppearsInSentReport() {
        String output = runApplication(
                LOGIN
                        + "1\n"
                        + "1\n"
                        + "+27834557896\n"
                        + "Did you get the cake?\n"
                        + "1\n"   // Send.
                        + "4\n"
                        + "7\n"   // Sent report.
                        + "0\n"
                        + "3\n"
        );

        /*
         * Restrict assertions to the submenu output.
         * The original send confirmation also prints message details.
         */
        String submenu = output.substring(
                output.indexOf("0) Return to main menu")
        );

        assertTrue(submenu.contains("Message: Did you get the cake?"));
        assertTrue(submenu.contains("Sender: +27838968976"));
        assertTrue(submenu.contains("Recipient: +27834557896"));
        assertTrue(submenu.contains("Status: SENT"));
        assertTrue(output.contains("Total messages sent: 1"));
    }

    @Test
    void newlyStoredMessageAppearsImmediatelyAndAfterRestart()
            throws IOException {

        String firstRun = runApplication(
                LOGIN
                        + "1\n"
                        + "1\n"
                        + "+27838884567\n"
                        + "Ok, I am leaving without you.\n"
                        + "3\n"   // Store.
                        + "4\n"
                        + "6\n"   // Stored report.
                        + "0\n"
                        + "3\n"
        );

        assertTrue(firstRun.contains("Message successfully stored."));
        assertTrue(firstRun.contains(
                "Message: Ok, I am leaving without you."
        ));
        assertTrue(firstRun.contains("Status: STORED"));
        assertTrue(firstRun.contains("Total messages sent: 0"));

        MessageRecord[] saved =
                new MessageJsonStore(temporaryDirectory).loadAll();

        assertEquals(1, saved.length);
        assertEquals("+27838968976", saved[0].sender());

        String secondRun = runApplication(
                LOGIN
                        + "1\n"
                        + "4\n"
                        + "6\n"
                        + "0\n"
                        + "3\n"
        );

        assertTrue(secondRun.contains("Stored messages loaded: 1"));
        assertTrue(secondRun.contains(saved[0].fullDetails()));
    }

    @Test
    void deletedMessageDoesNotReturnAfterRestart() throws IOException {
        MessageRecord stored = sampleStoredMessage();
        new MessageJsonStore(temporaryDirectory).save(stored);

        String firstRun = runApplication(
                LOGIN
                        + "1\n"
                        + "4\n"
                        + "5\n"
                        + stored.messageHash() + "\n"
                        + "0\n"
                        + "3\n"
        );

        assertTrue(firstRun.contains(
                "Message: \"" + stored.text()
                        + "\" successfully deleted."
        ));

        assertFalse(Files.exists(
                temporaryDirectory.resolve(stored.messageID() + ".json")
        ));

        String secondRun = runApplication(
                LOGIN
                        + "1\n"
                        + "4\n"
                        + "6\n"
                        + "0\n"
                        + "3\n"
        );

        assertTrue(secondRun.contains("Stored messages loaded: 0"));
        assertTrue(secondRun.contains("No stored messages."));
        assertFalse(secondRun.contains(stored.text()));
    }

    @Test
    void loadedMessagesSupportIdRecipientAndLongestSearches()
            throws IOException {

        MessageRecord stored = sampleStoredMessage();
        new MessageJsonStore(temporaryDirectory).save(stored);

        String output = runApplication(
                LOGIN
                        + "1\n"
                        + "4\n"
                        + "3\n"   // ID search.
                        + stored.messageID() + "\n"
                        + "4\n"   // Stored recipient search.
                        + stored.recipient() + "\n"
                        + "2\n"   // Longest stored message.
                        + "0\n"
                        + "3\n"
        );

        assertTrue(output.contains(
                "Recipient: " + stored.recipient()
                        + System.lineSeparator()
                        + "Message: " + stored.text()
        ));

        assertTrue(output.contains(stored.fullDetails()));

        // ID search, recipient search and longest-message display.
        assertEquals(3, occurrences(output, stored.text()));
    }

    @Test
    void disregardedMessageIsExcludedFromReportsAndDisk()
            throws IOException {

        String output = runApplication(
                LOGIN
                        + "1\n"
                        + "1\n"
                        + "invalid-number\n"
                        + "Yohoooo, I am at your gate.\n"
                        + "2\n"   // Disregard.
                        + "0\n"   // Confirm.
                        + "4\n"
                        + "6\n"   // Stored report.
                        + "7\n"   // Sent report.
                        + "0\n"
                        + "3\n"
        );

        assertTrue(output.contains("Message disregarded."));
        assertTrue(output.contains("No stored messages."));
        assertTrue(output.contains("No sent messages."));

        assertFalse(output.contains(
                "Message: Yohoooo, I am at your gate."
        ));

        assertEquals(
                0,
                new MessageJsonStore(temporaryDirectory).loadAll().length
        );

        assertTrue(output.contains("Total messages sent: 0"));
    }

    @Test
    void invalidStoredFileStopsApplicationBeforeMessagingMenu()
            throws IOException {

        Path file = temporaryDirectory.resolve("1123456789.json");
        Files.writeString(file, "{ invalid JSON");

        String output = runApplication(LOGIN);

        assertTrue(output.contains(
                "Stored messages could not be loaded:"
        ));

        assertFalse(output.contains("1) Send Messages"));
        assertFalse(output.contains(
                "How many messages would you like to enter?"
        ));

        assertTrue(Files.exists(file));
    }

    @Test
    void unsuccessfulLoginDoesNotReadStoredFiles() throws IOException {
        /*
         * If loading occurs before authentication, this malformed
         * file would produce a storage error.
         */
        Files.writeString(
                temporaryDirectory.resolve("1123456789.json"),
                "{ invalid JSON"
        );

        String input = String.join(
                "\n",
                "Kyle",
                "Smith",
                "kyl_1",
                "Ch&&sec@ke99!",
                "+27838968976",
                "kyl_1",
                "wrong-password"
        ) + "\n";

        String output = runApplication(input);

        assertTrue(output.contains(
                "Username or password incorrect, please try again."
        ));

        assertFalse(output.contains("Stored messages loaded:"));
        assertFalse(output.contains(
                "Stored messages could not be loaded:"
        ));

        assertFalse(output.contains("4) Stored Messages"));
    }

    @Test
    void returningFromSubmenuAllowsMessageEntry() {
        String output = runApplication(
                LOGIN
                        + "1\n"
                        + "4\n"
                        + "0\n"   // Return without consuming message quota.
                        + "1\n"
                        + "+27834557896\n"
                        + "Did you get the cake?\n"
                        + "1\n"
                        + "3\n"
        );

        assertTrue(output.contains("Message successfully sent."));
        assertTrue(output.contains("Total messages sent: 1"));
        assertTrue(output.contains("Goodbye."));
    }

    private String runApplication(String input) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        try (Scanner scanner = new Scanner(input);
             PrintStream output = new PrintStream(
                     captured,
                     true,
                     StandardCharsets.UTF_8)) {

            PROGPOEpart1.run(
                    scanner,
                    output,
                    new Message(temporaryDirectory)
            );
        }

        return captured.toString(StandardCharsets.UTF_8);
    }

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