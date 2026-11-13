package com.mycompany.progpoepart1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Part 3 JSON persistence tests.
 *
 * Uses temporary directories so tests never change real application data.
 *
 * Covers:
 * - Saving and loading records.
 * - Loading JSON into a MessageRecord array.
 * - Part 2 file compatibility.
 * - Escaped text and Unicode.
 * - Persistent deletion.
 * - Invalid data and storage failures.
 */
public class MessageJsonStoreTest {

    @TempDir
    Path temporaryDirectory;

    private Path storageDirectory;
    private MessageJsonStore store;

    @BeforeEach
    void setUp() {
        storageDirectory = temporaryDirectory.resolve("stored-messages");
        store = new MessageJsonStore(storageDirectory);
    }

    private MessageRecord storedMessage() {
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
    @DisplayName("A missing storage directory gives an empty array")
    void loadsEmptyArrayWhenDirectoryDoesNotExist() throws IOException {
        assertFalse(Files.exists(storageDirectory));

        assertArrayEquals(
                new MessageRecord[0],
                store.loadAll()
        );
    }

    @Test
    @DisplayName("Saving creates the directory and a correctly named JSON file")
    void createsDirectoryAndFile() throws IOException {
        MessageRecord message = storedMessage();

        Path file = store.save(message);

        assertTrue(Files.isDirectory(storageDirectory));
        assertTrue(Files.isRegularFile(file));

        assertEquals(
                storageDirectory.resolve("1123456789.json"),
                file
        );
    }

    @Test
    @DisplayName("Saved JSON contains all required message fields")
    void savesRequiredJsonFields() throws IOException {
        MessageRecord message = storedMessage();
        Path file = store.save(message);

        JsonObject json = JsonParser.parseString(
                Files.readString(file, StandardCharsets.UTF_8)
        ).getAsJsonObject();

        assertAll(
                () -> assertEquals(
                        message.messageID(),
                        json.get("messageID").getAsString()
                ),
                () -> assertEquals(
                        message.messageNumber(),
                        json.get("messageNumber").getAsInt()
                ),
                () -> assertEquals(
                        message.messageHash(),
                        json.get("messageHash").getAsString()
                ),
                () -> assertEquals(
                        message.sender(),
                        json.get("sender").getAsString()
                ),
                () -> assertEquals(
                        message.recipient(),
                        json.get("recipient").getAsString()
                ),
                () -> assertEquals(
                        message.text(),
                        json.get("message").getAsString()
                ),
                () -> assertEquals(
                        "STORED",
                        json.get("status").getAsString()
                )
        );
    }

    @Test
    @DisplayName("A saved record survives reopening the storage class")
    void loadsSavedRecordAfterReopening() throws IOException {
        MessageRecord message = storedMessage();
        store.save(message);

        MessageJsonStore reopened =
                new MessageJsonStore(storageDirectory);

        assertArrayEquals(
                new MessageRecord[]{message},
                reopened.loadAll()
        );
    }

    @Test
    @DisplayName("Multiple JSON messages load into an array ordered by filename")
    void loadsMultipleMessagesIntoArray() throws IOException {
        MessageRecord second = storedMessage();

        MessageRecord fifth = new MessageRecord(
                "4456789012",
                4,
                second.sender(),
                second.recipient(),
                "Ok, I am leaving without you.",
                MessageRecord.Status.STORED
        );

        // Save in reverse order to verify deterministic loading.
        store.save(fifth);
        store.save(second);

        MessageRecord[] loaded = store.loadAll();

        assertArrayEquals(
                new MessageRecord[]{second, fifth},
                loaded
        );

        // Demonstrates loading the records into the required repository arrays.
        MessageRepository repository = new MessageRepository();

        for (MessageRecord message : loaded) {
            repository.add(message);
        }

        assertArrayEquals(
                new MessageRecord[]{second, fifth},
                repository.getStoredMessages()
        );

        assertEquals(
                "Where are you? You are late! I have asked you to be on time.",
                repository.displayLongestStoredMessage()
        );
    }

    @Test
    @DisplayName("Existing Part 2 JSON loads without inventing a sender")
    void loadsActualPartTwoFile() throws IOException {
        Message oldSession = new Message(storageDirectory);

        oldSession.startMessage(
                0,
                "+27718693002",
                "Hi Mike, can you join us for dinner tonight?"
        );

        oldSession.storeMessage();

        MessageRecord[] loaded = store.loadAll();

        assertEquals(1, loaded.length);

        assertEquals(
                oldSession.getMessageID(),
                loaded[0].messageID()
        );

        assertEquals(
                oldSession.createMessageHash(),
                loaded[0].messageHash()
        );

        assertEquals(
                "Unknown sender (Part 2 file)",
                loaded[0].sender()
        );

        assertEquals(
                MessageRecord.Status.STORED,
                loaded[0].status()
        );
    }

    @Test
    @DisplayName("Quotes, backslashes, newlines and Unicode survive JSON storage")
    void preservesSpecialCharacters() throws IOException {
        MessageRecord message = new MessageRecord(
                "5567890123",
                5,
                "+27838968976",
                "+27838884567",
                "Hi \"Mike\"\nUse C:\\Docs\tCafé",
                MessageRecord.Status.STORED
        );

        Path file = store.save(message);

        // Parse the JSON independently before checking the loaded record.
        JsonObject json = JsonParser.parseString(
                Files.readString(file, StandardCharsets.UTF_8)
        ).getAsJsonObject();

        assertEquals(
                message.text(),
                json.get("message").getAsString()
        );

        assertArrayEquals(
                new MessageRecord[]{message},
                store.loadAll()
        );
    }

    @Test
    @DisplayName("Deletion removes the file and remains effective after reopening")
    void deletesPersistently() throws IOException {
        MessageRecord message = storedMessage();
        Path file = store.save(message);

        assertTrue(store.delete(message.messageID()));
        assertFalse(Files.exists(file));

        MessageJsonStore reopened =
                new MessageJsonStore(storageDirectory);

        assertArrayEquals(
                new MessageRecord[0],
                reopened.loadAll()
        );
    }

    @Test
    @DisplayName("Deleting one message preserves other stored messages")
    void deletionPreservesOtherFiles() throws IOException {
        MessageRecord second = storedMessage();

        MessageRecord fifth = new MessageRecord(
                "4456789012",
                4,
                second.sender(),
                second.recipient(),
                "Ok, I am leaving without you.",
                MessageRecord.Status.STORED
        );

        store.save(second);
        Path remainingFile = store.save(fifth);

        assertTrue(store.delete(second.messageID()));
        assertTrue(Files.exists(remainingFile));

        assertArrayEquals(
                new MessageRecord[]{fifth},
                store.loadAll()
        );
    }

    @Test
    @DisplayName("Deleting a missing file returns false")
    void handlesMissingFileDeletion() throws IOException {
        assertFalse(store.delete("9999999999"));
    }

    @Test
    @DisplayName("Saving cannot overwrite an existing message")
    void preventsAccidentalOverwrite() throws IOException {
        MessageRecord original = storedMessage();
        store.save(original);

        MessageRecord replacement = new MessageRecord(
                original.messageID(),
                original.messageNumber(),
                original.sender(),
                original.recipient(),
                "Replacement text",
                MessageRecord.Status.STORED
        );

        assertThrows(
                FileAlreadyExistsException.class,
                () -> store.save(replacement)
        );

        assertArrayEquals(
                new MessageRecord[]{original},
                store.loadAll()
        );
    }

    @Test
    @DisplayName("Sent and disregarded records cannot be saved as stored messages")
    void rejectsOtherStatuses() {
        for (MessageRecord.Status status : new MessageRecord.Status[]{
                MessageRecord.Status.SENT,
                MessageRecord.Status.DISREGARDED
        }) {
            MessageRecord message = new MessageRecord(
                    "0012345678",
                    0,
                    "+27838968976",
                    "+27834557896",
                    "Did you get the cake?",
                    status
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> store.save(message)
            );
        }

        assertFalse(Files.exists(storageDirectory));
    }

    @Test
    @DisplayName("Malformed JSON produces a visible loading error")
    void rejectsMalformedJson() throws IOException {
        writeFile("1123456789.json", "{ invalid JSON");

        IOException error = assertThrows(
                IOException.class,
                () -> store.loadAll()
        );

        assertTrue(error.getMessage().contains("1123456789.json"));
    }

    @Test
    @DisplayName("A modified hash is rejected")
    void rejectsIncorrectHash() throws IOException {
        JsonObject json = validJson();
        json.addProperty("messageHash", "WRONG");

        writeFile("1123456789.json", json.toString());

        assertThrows(IOException.class, () -> store.loadAll());
    }

    @Test
    @DisplayName("The filename must match the contained message ID")
    void rejectsFilenameMismatch() throws IOException {
        writeFile("9999999999.json", validJson().toString());

        assertThrows(IOException.class, () -> store.loadAll());
    }

    @Test
    @DisplayName("Missing required fields are rejected")
    void rejectsMissingRequiredField() throws IOException {
        JsonObject json = validJson();
        json.remove("recipient");

        writeFile("1123456789.json", json.toString());

        assertThrows(IOException.class, () -> store.loadAll());
    }

    @Test
    @DisplayName("Fractional, overflowing and negative message numbers are rejected")
    void rejectsInvalidMessageNumbers() throws IOException {
        String[] invalidNumbers = {
                "1.5",
                "2147483648",
                "-1"
        };

        for (String number : invalidNumbers) {
            JsonObject json = validJson();

            json.add(
                    "messageNumber",
                    JsonParser.parseString(number)
            );

            writeFile("1123456789.json", json.toString());

            assertThrows(
                    IOException.class,
                    () -> store.loadAll(),
                    "Must reject messageNumber " + number
            );
        }
    }

    @Test
    @DisplayName("A file claiming a non-stored status is rejected")
    void rejectsIncorrectStoredStatus() throws IOException {
        JsonObject json = validJson();
        json.addProperty("status", "SENT");

        writeFile("1123456789.json", json.toString());

        assertThrows(IOException.class, () -> store.loadAll());
    }

    @Test
    @DisplayName("Files without the JSON extension are ignored")
    void ignoresNonJsonFiles() throws IOException {
        MessageRecord message = storedMessage();
        store.save(message);

        writeFile("notes.txt", "These are not message data.");

        assertArrayEquals(
                new MessageRecord[]{message},
                store.loadAll()
        );
    }

    @Test
    @DisplayName("Invalid message IDs cannot be used as deletion paths")
    void rejectsInvalidDeletionIds() {
        String[] invalidIds = {
                null,
                "",
                "../notes",
                "123",
                "12345678901"
        };

        for (String id : invalidIds) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> store.delete(id)
            );
        }
    }

    @Test
    @DisplayName("An invalid stored file is preserved when deletion fails validation")
    void doesNotDeleteInvalidFile() throws IOException {
        Path file = writeFile(
                "1123456789.json",
                "{ invalid JSON"
        );

        assertThrows(
                IOException.class,
                () -> store.delete("1123456789")
        );

        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("A regular file cannot be used as the storage directory")
    void reportsInvalidStorageDirectory() throws IOException {
        Path regularFile = temporaryDirectory.resolve("regular-file");
        Files.writeString(regularFile, "Existing content");

        MessageJsonStore invalidStore =
                new MessageJsonStore(regularFile);

        assertThrows(
                IOException.class,
                () -> invalidStore.save(storedMessage())
        );

        assertThrows(
                IOException.class,
                () -> invalidStore.loadAll()
        );

        assertEquals(
                "Existing content",
                Files.readString(regularFile)
        );
    }

    /**
     * Builds valid input independently of MessageJsonStore.save().
     * This lets validation tests change individual JSON fields.
     */
    private JsonObject validJson() {
        JsonObject json = new JsonObject();

        json.addProperty("messageID", "1123456789");
        json.addProperty("messageNumber", 1);
        json.addProperty("messageHash", "11:1:WHERETIME");
        json.addProperty("sender", "+27838968976");
        json.addProperty("recipient", "+27838884567");
        json.addProperty(
                "message",
                "Where are you? You are late! I have asked you to be on time."
        );
        json.addProperty("status", "STORED");

        return json;
    }

    private Path writeFile(String filename, String content)
            throws IOException {

        Files.createDirectories(storageDirectory);

        Path file = storageDirectory.resolve(filename);

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8
        );

        return file;
    }
}