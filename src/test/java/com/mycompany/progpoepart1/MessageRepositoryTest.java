package com.mycompany.progpoepart1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Part 3: tests the assignment's array and reporting criteria.
 *
 * The five prescribed examples are supplied as test fixtures.
 * Production code does not hard-code these examples.
 *
 * Paper ambiguities:
 * 1. Message 4 uses "It is dinner time!" from the expected-results table.
 * 2. "0838884567" is used as message 4's ID because the ID-search
 *    test explicitly supplies it.
 * 3. Deletion preserves the original message punctuation rather
 *    than silently changing the stored text.
 */
public class MessageRepositoryTest {

    private MessageRepository repository;

    private MessageRecord first;
    private MessageRecord second;
    private MessageRecord third;
    private MessageRecord fourth;
    private MessageRecord fifth;

    @BeforeEach
    void setUp() {
        repository = new MessageRepository();

        String sender = "+27838968976";

        first = new MessageRecord(
                "0012345678",
                0,
                sender,
                "+27834557896",
                "Did you get the cake?",
                MessageRecord.Status.SENT
        );

        second = new MessageRecord(
                "1123456789",
                1,
                sender,
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                MessageRecord.Status.STORED
        );

        third = new MessageRecord(
                "2234567890",
                2,
                sender,
                "+27834484567",
                "Yohoooo, I am at your gate.",
                MessageRecord.Status.DISREGARDED
        );

        fourth = new MessageRecord(
                "0838884567",
                3,
                sender,
                "+27838884567",
                "It is dinner time!",
                MessageRecord.Status.SENT
        );

        fifth = new MessageRecord(
                "4456789012",
                4,
                sender,
                "+27838884567",
                "Ok, I am leaving without you.",
                MessageRecord.Status.STORED
        );
    }

    /**
     * Adds examples 1–4 for criteria explicitly referring to messages 1–4.
     */
    private void addFirstFour() {
        repository.add(first);
        repository.add(second);
        repository.add(third);
        repository.add(fourth);
    }

    private void addAllFive() {
        addFirstFour();
        repository.add(fifth);
    }

    @Test
    @DisplayName("Criterion: sent array contains the two expected messages")
    void populatesSentMessagesArray() {
        addFirstFour();

        assertArrayEquals(
                new String[]{
                        "Did you get the cake?",
                        "It is dinner time!"
                },
                repository.getSentMessageTexts()
        );

        assertArrayEquals(
                new MessageRecord[]{first, fourth},
                repository.getSentMessages()
        );
    }

    @Test
    @DisplayName("Stored and disregarded records populate their own arrays")
    void populatesOtherStatusArrays() {
        addAllFive();

        assertArrayEquals(
                new MessageRecord[]{second, fifth},
                repository.getStoredMessages()
        );

        assertArrayEquals(
                new MessageRecord[]{third},
                repository.getDisregardedMessages()
        );
    }

    @Test
    @DisplayName("ID and hash arrays retain matching positions")
    void populatesParallelTrackingArrays() {
        addAllFive();

        assertArrayEquals(
                new String[]{
                        "0012345678",
                        "1123456789",
                        "2234567890",
                        "0838884567",
                        "4456789012"
                },
                repository.getMessageIDs()
        );

        assertArrayEquals(
                new String[]{
                        "00:0:DIDCAKE",
                        "11:1:WHERETIME",
                        "22:2:YOHOOOOGATE",
                        "08:3:ITTIME",
                        "44:4:OKYOU"
                },
                repository.getMessageHashes()
        );
    }

    @Test
    @DisplayName("Criterion: the longest message matches the supplied example")
    void displaysLongestStoredMessage() {
        addFirstFour();

        assertEquals(
                "Where are you? You are late! I have asked you to be on time.",
                repository.displayLongestStoredMessage()
        );

        assertEquals(second, repository.findLongestStoredMessage());
    }

    @Test
    @DisplayName("Longest stored lookup excludes longer sent messages")
    void longestStoredSearchUsesStoredArrayOnly() {
        repository.add(second);

        repository.add(new MessageRecord(
                "5567890123",
                5,
                first.sender(),
                first.recipient(),
                "A".repeat(200),
                MessageRecord.Status.SENT
        ));

        assertEquals(second, repository.findLongestStoredMessage());
    }

    @Test
    @DisplayName("Criterion: ID 0838884567 finds message four")
    void searchesByPrescribedMessageID() {
        addFirstFour();

        assertEquals(
                fourth,
                repository.findByMessageID("0838884567")
        );

        assertEquals(
                "It is dinner time!",
                repository.findByMessageID("0838884567").text()
        );

        assertEquals(
                "Recipient: +27838884567"
                        + System.lineSeparator()
                        + "Message: It is dinner time!",
                repository.searchByMessageID("0838884567")
        );
    }

    @Test
    @DisplayName("Criterion: stored recipient search returns messages two and five")
    void searchesStoredMessagesByPrescribedRecipient() {
        addAllFive();

        MessageRecord[] matches =
                repository.findStoredByRecipient("+27838884567");

        assertArrayEquals(
                new MessageRecord[]{second, fifth},
                matches
        );

        assertArrayEquals(
                new String[]{
                        "Where are you? You are late! "
                                + "I have asked you to be on time.",
                        "Ok, I am leaving without you."
                },
                textsOf(matches)
        );
    }

    @Test
    @DisplayName("Combined recipient search includes sent and stored messages")
    void searchesSentAndStoredMessagesByRecipient() {
        addAllFive();

        /*
         * Message 4's recipient is not specified clearly in the paper.
         * This fixture assigns it the same recipient.
         * Therefore combined search must include it as well.
         */
        assertArrayEquals(
                new MessageRecord[]{second, fourth, fifth},
                repository.findByRecipient("+27838884567")
        );

        // A disregarded message is not a sent or stored result.
        assertEquals(
                0,
                repository.findByRecipient(third.recipient()).length
        );
    }

    @Test
    @DisplayName("Criterion: message two can be deleted using its hash")
    void deletesMessageUsingHash() {
        addAllFive();

        assertEquals(
                "Message: \"Where are you? You are late! "
                        + "I have asked you to be on time.\" "
                        + "successfully deleted.",
                repository.deleteByMessageHash("11:1:WHERETIME")
        );

        assertNull(repository.findByMessageID(second.messageID()));

        assertArrayEquals(
                new MessageRecord[]{fifth},
                repository.getStoredMessages()
        );

        assertArrayEquals(
                new MessageRecord[]{first, fourth},
                repository.getSentMessages()
        );
    }

    @Test
    @DisplayName("Deleting a message keeps the parallel arrays aligned")
    void deletionUpdatesIdsAndHashes() {
        addAllFive();

        repository.deleteByMessageHash(second.messageHash());

        assertArrayEquals(
                new String[]{
                        first.messageID(),
                        third.messageID(),
                        fourth.messageID(),
                        fifth.messageID()
                },
                repository.getMessageIDs()
        );

        assertArrayEquals(
                new String[]{
                        first.messageHash(),
                        third.messageHash(),
                        fourth.messageHash(),
                        fifth.messageHash()
                },
                repository.getMessageHashes()
        );

        // ID lookup must still return the correct record after shifting.
        assertEquals(
                fourth,
                repository.findByMessageID(fourth.messageID())
        );
    }

    @Test
    @DisplayName("Criterion: sent report includes hash, recipient and message")
    void displaysSentReport() {
        addAllFive();

        assertEquals(
                first.fullDetails()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + fourth.fullDetails(),
                repository.displaySentReport()
        );

        String report = repository.displaySentReport();

        assertTrue(report.contains("Message Hash: 00:0:DIDCAKE"));
        assertTrue(report.contains("Recipient: +27834557896"));
        assertTrue(report.contains("Message: Did you get the cake?"));

        assertFalse(report.contains(second.text()));
        assertFalse(report.contains(third.text()));
        assertFalse(report.contains(fifth.text()));
    }

    @Test
    @DisplayName("Stored report contains all stored records and their full details")
    void displaysStoredReport() {
        addAllFive();

        assertEquals(
                second.fullDetails()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + fifth.fullDetails(),
                repository.displayStoredReport()
        );
    }

    @Test
    @DisplayName("Stored sender and recipient display contains the correct records")
    void displaysStoredSendersAndRecipients() {
        addAllFive();

        String line =
                "Sender: +27838968976, Recipient: +27838884567";

        assertEquals(
                line + System.lineSeparator() + line,
                repository.displayStoredSendersAndRecipients()
        );
    }

    @Test
    @DisplayName("An empty repository gives clear search and report responses")
    void handlesEmptyRepository() {
        assertEquals(0, repository.getSentMessages().length);
        assertEquals(0, repository.getStoredMessages().length);
        assertEquals(0, repository.getDisregardedMessages().length);
        assertEquals(0, repository.getMessageIDs().length);
        assertEquals(0, repository.getMessageHashes().length);

        assertNull(repository.findLongestStoredMessage());

        assertEquals(
                "No stored messages.",
                repository.displayLongestStoredMessage()
        );

        assertEquals(
                "No stored messages.",
                repository.displayStoredReport()
        );

        assertEquals(
                "No sent messages.",
                repository.displaySentReport()
        );

        assertEquals(
                "No stored messages.",
                repository.displayStoredSendersAndRecipients()
        );

        assertEquals(
                "Message not found.",
                repository.searchByMessageID("9999999999")
        );

        assertEquals(
                "Message not found.",
                repository.deleteByMessageHash("99:0:UNKNOWN")
        );
    }

    @Test
    @DisplayName("Unknown and null searches return no matches")
    void handlesMissingSearchValues() {
        addAllFive();

        assertNull(repository.findByMessageID(null));
        assertNull(repository.findByMessageID("9999999999"));

        assertEquals(
                "Message not found.",
                repository.searchByMessageID(null)
        );

        assertEquals(0, repository.findByRecipient(null).length);
        assertEquals(0, repository.findStoredByRecipient(null).length);

        assertEquals(
                0,
                repository.findByRecipient("+27700000000").length
        );

        assertEquals(
                "Message not found.",
                repository.deleteByMessageHash(null)
        );
    }

    @Test
    @DisplayName("Duplicate message IDs cannot overwrite existing records")
    void rejectsDuplicateMessageID() {
        repository.add(first);

        MessageRecord duplicate = new MessageRecord(
                first.messageID(),
                9,
                first.sender(),
                first.recipient(),
                "Different message",
                MessageRecord.Status.STORED
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.add(duplicate)
        );

        assertEquals(1, repository.getMessageIDs().length);
        assertEquals(first, repository.findByMessageID(first.messageID()));
        assertEquals(0, repository.getStoredMessages().length);
    }

    @Test
    @DisplayName("Returned arrays cannot be used to change repository contents")
    void protectsInternalArrays() {
        addAllFive();

        MessageRecord[] sent = repository.getSentMessages();
        MessageRecord[] stored = repository.getStoredMessages();
        MessageRecord[] disregarded = repository.getDisregardedMessages();
        String[] ids = repository.getMessageIDs();
        String[] hashes = repository.getMessageHashes();
        String[] texts = repository.getSentMessageTexts();

        sent[0] = null;
        stored[0] = null;
        disregarded[0] = null;
        ids[0] = "9999999999";
        hashes[0] = "CHANGED";
        texts[0] = "CHANGED";

        assertEquals(first, repository.getSentMessages()[0]);
        assertEquals(second, repository.getStoredMessages()[0]);
        assertEquals(third, repository.getDisregardedMessages()[0]);
        assertEquals(first.messageID(), repository.getMessageIDs()[0]);
        assertEquals(first.messageHash(), repository.getMessageHashes()[0]);
        assertEquals(first.text(), repository.getSentMessageTexts()[0]);
    }

    @Test
    @DisplayName("An ambiguous hash does not delete either matching message")
    void refusesAmbiguousHashDeletion() {
        repository.add(first);

        MessageRecord collision = new MessageRecord(
                "0099999999",
                0,
                first.sender(),
                first.recipient(),
                first.text(),
                MessageRecord.Status.STORED
        );

        repository.add(collision);

        assertEquals(first.messageHash(), collision.messageHash());

        assertEquals(
                "Multiple messages have this hash; no message was deleted.",
                repository.deleteByMessageHash(first.messageHash())
        );

        assertEquals(2, repository.getMessageIDs().length);
        assertEquals(first, repository.findByMessageID(first.messageID()));
        assertEquals(
                collision,
                repository.findByMessageID(collision.messageID())
        );
    }

    @Test
    @DisplayName("The earliest stored message wins when longest lengths tie")
    void handlesEqualLengthMessages() {
        MessageRecord earlier = new MessageRecord(
                "5567890123",
                5,
                first.sender(),
                first.recipient(),
                "Hello Mike",
                MessageRecord.Status.STORED
        );

        MessageRecord later = new MessageRecord(
                "6678901234",
                6,
                first.sender(),
                first.recipient(),
                "Hello Jane",
                MessageRecord.Status.STORED
        );

        repository.add(earlier);
        repository.add(later);

        assertEquals(earlier, repository.findLongestStoredMessage());
    }

    @Test
    @DisplayName("A null record is rejected without changing the arrays")
    void rejectsNullRecord() {
        assertThrows(
                NullPointerException.class,
                () -> repository.add(null)
        );

        assertEquals(0, repository.getMessageIDs().length);
    }

    private String[] textsOf(MessageRecord[] messages) {
        String[] texts = new String[messages.length];

        for (int i = 0; i < messages.length; i++) {
            texts[i] = messages[i].text();
        }

        return texts;
    }
}