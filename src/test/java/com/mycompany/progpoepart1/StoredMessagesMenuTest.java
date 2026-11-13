package com.mycompany.progpoepart1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Part 3 Stored Messages submenu.
 *
 * Tests use simulated input and captured output.
 * Persistent deletion is verified against real temporary JSON files.
 */
public class StoredMessagesMenuTest {

    @TempDir
    Path temporaryDirectory;

    private MessageRepository repository;
    private MessageJsonStore store;
    private StoredMessagesMenu menu;

    private MessageRecord stored;
    private MessageRecord sent;

    @BeforeEach
    void setUp() throws IOException {
        repository = new MessageRepository();
        store = new MessageJsonStore(temporaryDirectory);

        stored = new MessageRecord(
                "1123456789",
                1,
                "+27838968976",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                MessageRecord.Status.STORED
        );

        sent = new MessageRecord(
                "0012345678",
                0,
                "+27838968976",
                "+27834557896",
                "Did you get the cake?",
                MessageRecord.Status.SENT
        );

        store.save(stored);
        repository.add(stored);
        repository.add(sent);

        menu = new StoredMessagesMenu(repository, store);
    }

    @Test
    @DisplayName("Option zero returns without changing records")
    void returnsToMainMenu() {
        String output = runMenu("0\n");

        assertTrue(output.contains("Stored Messages"));
        assertTrue(output.contains("0) Return to main menu"));

        assertEquals(1, repository.getStoredMessages().length);
        assertEquals(1, repository.getSentMessages().length);
    }

    @Test
    @DisplayName("Option one displays stored senders and recipients")
    void displaysSendersAndRecipients() {
        String output = runMenu("1\n0\n");

        assertTrue(output.contains(
                "Sender: +27838968976, Recipient: +27838884567"
        ));

        assertFalse(output.contains(
                "Sender: +27838968976, Recipient: +27834557896"
        ));
    }

    @Test
    @DisplayName("Option two displays the longest stored message")
    void displaysLongestStoredMessage() {
        String output = runMenu("2\n0\n");

        assertTrue(output.contains(stored.text()));
        assertFalse(output.contains("Message: " + sent.text()));
    }

    @Test
    @DisplayName("Option three searches using the supplied message ID")
    void searchesByMessageID() {
        String output = runMenu(
                "3\n"
                        + stored.messageID() + "\n"
                        + "0\n"
        );

        assertTrue(output.contains(
                "Recipient: " + stored.recipient()
                        + System.lineSeparator()
                        + "Message: " + stored.text()
        ));
    }

    @Test
    @DisplayName("An unknown message ID produces a clear response")
    void handlesUnknownMessageID() {
        String output = runMenu("3\n9999999999\n0\n");

        assertTrue(output.contains("Message not found."));
    }

    @Test
    @DisplayName("Option four returns every stored message for a recipient")
    void searchesAllStoredRecipientMatches() throws IOException {
        MessageRecord fifth = new MessageRecord(
                "4456789012",
                4,
                stored.sender(),
                stored.recipient(),
                "Ok, I am leaving without you.",
                MessageRecord.Status.STORED
        );

        store.save(fifth);
        repository.add(fifth);

        String output = runMenu(
                "4\n"
                        + stored.recipient() + "\n"
                        + "0\n"
        );

        assertTrue(output.contains(stored.fullDetails()));
        assertTrue(output.contains(fifth.fullDetails()));
        assertFalse(output.contains(sent.fullDetails()));
    }

    @Test
    @DisplayName("A recipient without matches produces a clear response")
    void handlesUnknownRecipient() {
        String output = runMenu("4\n+27700000000\n0\n");

        assertTrue(output.contains("No matching messages."));
    }

    @Test
    @DisplayName("Option five deletes from arrays and persistent storage")
    void deletesThroughMenu() throws IOException {
        String output = runMenu(
                "5\n"
                        + stored.messageHash() + "\n"
                        + "0\n"
        );

        assertTrue(output.contains(
                "Message: \"" + stored.text()
                        + "\" successfully deleted."
        ));

        assertNull(repository.findByMessageID(stored.messageID()));
        assertEquals(0, repository.getStoredMessages().length);

        assertFalse(Files.exists(
                temporaryDirectory.resolve(stored.messageID() + ".json")
        ));

        // Reopening storage must not bring the deleted message back.
        assertArrayEquals(
                new MessageRecord[0],
                new MessageJsonStore(temporaryDirectory).loadAll()
        );

        assertEquals(1, repository.getSentMessages().length);
    }

    @Test
    @DisplayName("Option six displays the full stored-message report")
    void displaysStoredReport() {
        String output = runMenu("6\n0\n");

        assertTrue(output.contains(stored.fullDetails()));
        assertFalse(output.contains(sent.fullDetails()));
    }

    @Test
    @DisplayName("Option seven displays the sent-message report")
    void displaysSentReport() {
        String output = runMenu("7\n0\n");

        assertTrue(output.contains(sent.fullDetails()));
        assertFalse(output.contains(stored.fullDetails()));
    }

    @Test
    @DisplayName("Option eight searches sent and stored messages together")
    void searchesSentAndStoredMessages() {
        MessageRecord anotherSent = new MessageRecord(
                "5567890123",
                5,
                stored.sender(),
                stored.recipient(),
                "Hello again",
                MessageRecord.Status.SENT
        );

        repository.add(anotherSent);

        String output = runMenu(
                "8\n"
                        + stored.recipient() + "\n"
                        + "0\n"
        );

        assertTrue(output.contains(stored.fullDetails()));
        assertTrue(output.contains(anotherSent.fullDetails()));
        assertFalse(output.contains(sent.fullDetails()));
    }

    @Test
    @DisplayName("Invalid submenu input is rejected and the menu continues")
    void retriesInvalidMenuInput() {
        String output = runMenu("hello\n9\n0\n");

        String expected = "Please choose an option from 0 to 8.";

        int firstPosition = output.indexOf(expected);
        int secondPosition = output.indexOf(
                expected,
                firstPosition + expected.length()
        );

        assertTrue(firstPosition >= 0);
        assertTrue(secondPosition > firstPosition);
    }

    @Test
    @DisplayName("The stored-message delete option cannot remove sent messages")
    void refusesSentMessageDeletion() {
        assertEquals(
                "This option deletes stored messages only.",
                menu.deleteStoredByHash(sent.messageHash())
        );

        assertEquals(
                sent,
                repository.findByMessageID(sent.messageID())
        );
    }

    @Test
    @DisplayName("An ambiguous hash leaves records and files unchanged")
    void refusesAmbiguousDeletion() throws IOException {
        MessageRecord collision = new MessageRecord(
                "1199999999",
                stored.messageNumber(),
                stored.sender(),
                stored.recipient(),
                stored.text(),
                MessageRecord.Status.STORED
        );

        store.save(collision);
        repository.add(collision);

        assertEquals(stored.messageHash(), collision.messageHash());

        assertEquals(
                "Multiple messages have this hash; no message was deleted.",
                menu.deleteStoredByHash(stored.messageHash())
        );

        assertEquals(2, repository.getStoredMessages().length);
        assertEquals(2, store.loadAll().length);
    }

    @Test
    @DisplayName("A file validation failure leaves the repository unchanged")
    void preservesRecordWhenFileDeletionFails() throws IOException {
        Path file = temporaryDirectory.resolve(
                stored.messageID() + ".json"
        );

        Files.writeString(file, "{ invalid JSON");

        assertEquals(
                "Message could not be deleted; "
                        + "the message remains in the arrays.",
                menu.deleteStoredByHash(stored.messageHash())
        );

        assertTrue(Files.exists(file));

        assertEquals(
                stored,
                repository.findByMessageID(stored.messageID())
        );
    }

    @Test
    @DisplayName("An already absent file allows removal of its remaining array record")
    void removesRecordWhenFileIsAlreadyMissing() throws IOException {
        Files.delete(
                temporaryDirectory.resolve(stored.messageID() + ".json")
        );

        assertEquals(
                "Message: \"" + stored.text()
                        + "\" successfully deleted.",
                menu.deleteStoredByHash(stored.messageHash())
        );

        assertNull(repository.findByMessageID(stored.messageID()));
        assertEquals(0, repository.getStoredMessages().length);
    }

    @Test
    @DisplayName("Empty and unknown hashes do not delete records")
    void handlesMissingHashes() {
        for (String hash : new String[]{
                null,
                "",
                "   ",
                "99:9:UNKNOWN"
        }) {
            assertEquals(
                    "Message not found.",
                    menu.deleteStoredByHash(hash)
            );
        }

        assertEquals(1, repository.getStoredMessages().length);
        assertEquals(1, repository.getSentMessages().length);
    }

    private String runMenu(String input) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        try (Scanner scanner = new Scanner(input);
             PrintStream output = new PrintStream(
                     captured,
                     true,
                     StandardCharsets.UTF_8)) {

            menu.run(scanner, output);
        }

        return captured.toString(StandardCharsets.UTF_8);
    }
}