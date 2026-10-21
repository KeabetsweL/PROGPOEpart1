package com.mycompany.progpoepart1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests integration between Message, MessageRepository
 * and MessageJsonStore.
 *
 * These tests process messages through the real send/store/disregard
 * methods rather than adding records directly to the repository.
 */
public class MessagePartThreeTest {

    private static final String SENDER = "+27838968976";
    private static final String RECIPIENT = "+27838884567";

    @TempDir
    Path temporaryDirectory;

    private Message messages;
    private MessageRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MessageRepository();
        messages = new Message(temporaryDirectory);
        messages.enablePartThree(repository, SENDER);
    }

    @Test
    @DisplayName("Sending populates the sent array and tracking arrays")
    void sendingPopulatesRepository() {
        messages.startMessage(
                0,
                RECIPIENT,
                "Did you get the cake?"
        );

        String id = messages.getMessageID();
        String hash = messages.createMessageHash();

        assertEquals(
                "Message successfully sent.",
                messages.SentMessage(1)
        );

        MessageRecord[] sent = repository.getSentMessages();

        assertEquals(1, sent.length);
        assertEquals(id, sent[0].messageID());
        assertEquals(SENDER, sent[0].sender());
        assertEquals(RECIPIENT, sent[0].recipient());
        assertEquals("Did you get the cake?", sent[0].text());
        assertEquals(MessageRecord.Status.SENT, sent[0].status());

        assertArrayEquals(
                new String[]{id},
                repository.getMessageIDs()
        );

        assertArrayEquals(
                new String[]{hash},
                repository.getMessageHashes()
        );

        assertEquals(1, messages.returnTotalMessagess());
        assertEquals(0, repository.getStoredMessages().length);
        assertEquals(0, repository.getDisregardedMessages().length);
    }

    @Test
    @DisplayName("Storing populates the stored array and retains sender in JSON")
    void storingPopulatesRepositoryAndJson() throws IOException {
        messages.startMessage(
                0,
                RECIPIENT,
                "Ok, I am leaving without you."
        );

        assertEquals(
                "Message successfully stored.",
                messages.SentMessage(3)
        );

        MessageRecord[] stored = repository.getStoredMessages();

        assertEquals(1, stored.length);
        assertEquals(SENDER, stored[0].sender());
        assertEquals(MessageRecord.Status.STORED, stored[0].status());

        MessageJsonStore reopened =
                new MessageJsonStore(temporaryDirectory);

        assertArrayEquals(stored, reopened.loadAll());

        assertEquals(0, messages.returnTotalMessagess());
        assertEquals(0, repository.getSentMessages().length);
        assertEquals(0, repository.getDisregardedMessages().length);
    }

    @Test
    @DisplayName("Disregarding retains the message in the disregarded array")
    void disregardingPopulatesRepository() {
        messages.startMessage(
                0,
                "invalid-number",
                "Yohoooo, I am at your gate."
        );

        assertEquals(
                "Press 0 to delete the message.",
                messages.SentMessage(2)
        );

        MessageRecord[] disregarded =
                repository.getDisregardedMessages();

        assertEquals(1, disregarded.length);
        assertEquals("invalid-number", disregarded[0].recipient());
        assertEquals(
                "Yohoooo, I am at your gate.",
                disregarded[0].text()
        );

        assertEquals(
                MessageRecord.Status.DISREGARDED,
                disregarded[0].status()
        );

        assertEquals(0, messages.returnTotalMessagess());
        assertEquals(0, repository.getSentMessages().length);
        assertEquals(0, repository.getStoredMessages().length);
    }

    @Test
    @DisplayName("Invalid sending does not populate any array")
    void invalidSendingDoesNotCreateRecord() {
        messages.startMessage(
                0,
                "08575975889",
                "Hello Mike"
        );

        assertEquals(
                "Cell phone number is incorrectly formatted or does "
                        + "not contain an international code. Please correct "
                        + "the number and try again.",
                messages.SentMessage(1)
        );

        assertEquals(0, repository.getMessageIDs().length);
        assertEquals(0, repository.getMessageHashes().length);
        assertEquals(0, messages.returnTotalMessagess());
    }

    @Test
    @DisplayName("Overlength messages cannot enter the stored array or disk")
    void invalidStorageDoesNotCreateRecordOrFile()
            throws IOException {

        messages.startMessage(
                0,
                RECIPIENT,
                "A".repeat(251)
        );

        assertEquals(
                "Message exceeds 250 characters by 1; please reduce the size.",
                messages.SentMessage(3)
        );

        assertEquals(0, repository.getStoredMessages().length);

        assertArrayEquals(
                new MessageRecord[0],
                new MessageJsonStore(temporaryDirectory).loadAll()
        );
    }

    @Test
    @DisplayName("Composing another message does not change earlier records")
    void preservesEarlierMessageSnapshot() {
        messages.startMessage(
                0,
                RECIPIENT,
                "Did you get the cake?"
        );

        messages.SentMessage(1);

        MessageRecord original = repository.getSentMessages()[0];

        messages.startMessage(
                1,
                "+27834557896",
                "Another message"
        );

        messages.SentMessage(1);

        assertEquals(2, repository.getSentMessages().length);

        assertEquals(
                original,
                repository.getSentMessages()[0]
        );

        assertEquals(
                "Did you get the cake?",
                repository.getSentMessages()[0].text()
        );

        assertEquals(
                "Another message",
                repository.getSentMessages()[1].text()
        );
    }

    @Test
    @DisplayName("Processing a message twice cannot duplicate repository records")
    void preventsDuplicateRecords() {
        messages.startMessage(0, RECIPIENT, "Hello Mike");

        assertEquals(
                "Message successfully sent.",
                messages.SentMessage(1)
        );

        assertEquals(
                "This message has already been processed.",
                messages.SentMessage(1)
        );

        assertEquals(
                "This message has already been processed.",
                messages.SentMessage(3)
        );

        assertEquals(1, repository.getSentMessages().length);
        assertEquals(1, repository.getMessageIDs().length);
        assertEquals(0, repository.getStoredMessages().length);
        assertEquals(1, messages.returnTotalMessagess());
    }

    @Test
    @DisplayName("Storage failure leaves the array empty and permits retry")
    void storageFailureDoesNotCreateStoredRecord()
            throws IOException {

        Path blockedDirectory = temporaryDirectory.resolve("blocked");
        Files.writeString(blockedDirectory, "A regular file");

        MessageRepository blockedRepository = new MessageRepository();

        Message blockedMessages = new Message(blockedDirectory);
        blockedMessages.enablePartThree(blockedRepository, SENDER);
        blockedMessages.startMessage(0, RECIPIENT, "Hello Mike");

        assertEquals(
                "Message could not be stored. Please try again.",
                blockedMessages.SentMessage(3)
        );

        assertEquals(0, blockedRepository.getMessageIDs().length);
        assertEquals(0, blockedRepository.getStoredMessages().length);

        assertEquals(
                "Message successfully sent.",
                blockedMessages.SentMessage(1)
        );

        assertEquals(1, blockedRepository.getSentMessages().length);
        assertEquals(1, blockedMessages.returnTotalMessagess());
    }

    @Test
    @DisplayName("Null details can be retained safely when disregarded")
    void disregardsIncompleteInput() {
        messages.startMessage(0, null, null);

        assertEquals(
                "Press 0 to delete the message.",
                messages.SentMessage(2)
        );

        MessageRecord record =
                repository.getDisregardedMessages()[0];

        assertEquals("", record.recipient());
        assertEquals("", record.text());

        assertEquals(
                messages.getMessageID().substring(0, 2) + ":0:",
                record.messageHash()
        );
    }

    @Test
    @DisplayName("Part 3 cannot be enabled twice")
    void rejectsRepeatedConfiguration() {
        assertThrows(
                IllegalStateException.class,
                () -> messages.enablePartThree(
                        new MessageRepository(),
                        SENDER
                )
        );
    }

    @Test
    @DisplayName("Part 3 cannot be enabled after composition starts")
    void rejectsLateConfiguration() {
        Message unconfigured = new Message(temporaryDirectory);

        unconfigured.startMessage(0, RECIPIENT, "Hello Mike");

        assertThrows(
                IllegalStateException.class,
                () -> unconfigured.enablePartThree(
                        new MessageRepository(),
                        SENDER
                )
        );
    }

    @Test
    @DisplayName("Part 3 requires a repository and a nonblank sender")
    void rejectsInvalidConfiguration() {
        Message unconfigured = new Message(temporaryDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> unconfigured.enablePartThree(null, SENDER)
        );

        for (String sender : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> unconfigured.enablePartThree(
                            new MessageRepository(),
                            sender
                    )
            );
        }

        // Failed configuration attempts must not prevent valid setup.
        assertDoesNotThrow(
                () -> unconfigured.enablePartThree(
                        new MessageRepository(),
                        SENDER
                )
        );
    }
}