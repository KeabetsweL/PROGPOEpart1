package com.mycompany.progpoepart1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the message records used by the Part 3 arrays.
 *
 * Verifies:
 * - Preservation of message details.
 * - Hash construction.
 * - Report formatting.
 * - Required-field validation.
 * - Representation of disregarded messages with invalid details.
 */
public class MessageRecordTest {

    private MessageRecord validMessage() {
        return new MessageRecord(
                "0012345678",
                0,
                "+27838968976",
                "+27834557896",
                "Did you get the cake?",
                MessageRecord.Status.SENT
        );
    }

    @Test
    @DisplayName("A record preserves every supplied message field")
    void preservesMessageDetails() {
        MessageRecord message = validMessage();

        assertAll(
                () -> assertEquals("0012345678", message.messageID()),
                () -> assertEquals(0, message.messageNumber()),
                () -> assertEquals("+27838968976", message.sender()),
                () -> assertEquals("+27834557896", message.recipient()),
                () -> assertEquals(
                        "Did you get the cake?",
                        message.text()
                ),
                () -> assertEquals(
                        MessageRecord.Status.SENT,
                        message.status()
                )
        );
    }

    @Test
    @DisplayName("Part 3 message one produces the expected hash")
    void createsHashForFirstPartThreeMessage() {
        assertEquals(
                "00:0:DIDCAKE",
                validMessage().messageHash()
        );
    }

    @Test
    @DisplayName("The Part 2 prescribed hash remains consistent")
    void preservesPartTwoHashRules() {
        MessageRecord message = new MessageRecord(
                "0012345678",
                0,
                "+27838968976",
                "+27718693002",
                "Hi Mike, can you join us for dinner tonight?",
                MessageRecord.Status.SENT
        );

        assertEquals("00:0:HITONIGHT", message.messageHash());
    }

    @Test
    @DisplayName("Multiple Part 3 hashes are checked in a loop")
    void createsHashesForPartThreeExamples() {
        String[] texts = {
                "Did you get the cake?",
                "Where are you? You are late! I have asked you to be on time.",
                "Yohoooo, I am at your gate.",
                "It is dinner time !",
                "Ok, I am leaving without you."
        };

        /*
         * The fourth example ends with a separate punctuation-only token.
         * These expectations preserve the existing Part 2 hash algorithm:
         * punctuation is removed from the first and last whitespace tokens.
         */
        String[] expectedHashes = {
                "00:0:DIDCAKE",
                "00:1:WHERETIME",
                "00:2:YOHOOOOGATE",
                "00:3:IT",
                "00:4:OKYOU"
        };

        for (int number = 0; number < texts.length; number++) {
            MessageRecord message = new MessageRecord(
                    "0012345678",
                    number,
                    "+27838968976",
                    "+27838884567",
                    texts[number],
                    MessageRecord.Status.STORED
            );

            assertEquals(
                    expectedHashes[number],
                    message.messageHash(),
                    "Incorrect hash for example " + (number + 1)
            );
        }
    }

    @Test
    @DisplayName("Full details contain all fields in the documented order")
    void formatsFullDetails() {
        String expected = String.join(
                System.lineSeparator(),
                "Message ID: 0012345678",
                "Message Hash: 00:0:DIDCAKE",
                "Sender: +27838968976",
                "Recipient: +27834557896",
                "Message: Did you get the cake?",
                "Status: SENT"
        );

        assertEquals(expected, validMessage().fullDetails());
    }

    @Test
    @DisplayName("A message ID must contain exactly ten digits")
    void rejectsInvalidMessageIds() {
        String[] invalidIds = {
                null,
                "",
                "123456789",
                "12345678901",
                "12345ABCDE"
        };

        for (String id : invalidIds) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MessageRecord(
                            id,
                            0,
                            "+27838968976",
                            "+27834557896",
                            "Hello",
                            MessageRecord.Status.SENT
                    )
            );
        }
    }

    @Test
    @DisplayName("Negative message numbers are rejected")
    void rejectsNegativeMessageNumber() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageRecord(
                        "0012345678",
                        -1,
                        "+27838968976",
                        "+27834557896",
                        "Hello",
                        MessageRecord.Status.SENT
                )
        );
    }

    @Test
    @DisplayName("A sender is required")
    void rejectsMissingSender() {
        String[] invalidSenders = {null, "", "   "};

        for (String sender : invalidSenders) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MessageRecord(
                            "0012345678",
                            0,
                            sender,
                            "+27834557896",
                            "Hello",
                            MessageRecord.Status.SENT
                    )
            );
        }
    }

    @Test
    @DisplayName("A null recipient is rejected")
    void rejectsNullRecipient() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageRecord(
                        "0012345678",
                        0,
                        "+27838968976",
                        null,
                        "Hello",
                        MessageRecord.Status.SENT
                )
        );
    }

    @Test
    @DisplayName("Null message text is rejected")
    void rejectsNullText() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageRecord(
                        "0012345678",
                        0,
                        "+27838968976",
                        "+27834557896",
                        null,
                        MessageRecord.Status.SENT
                )
        );
    }

    @Test
    @DisplayName("A processing status is required")
    void rejectsNullStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageRecord(
                        "0012345678",
                        0,
                        "+27838968976",
                        "+27834557896",
                        "Hello",
                        null
                )
        );
    }

    @Test
    @DisplayName("Disregarded messages can retain invalid recipient input")
    void preservesInvalidRecipientForDisregardedMessage() {
        MessageRecord message = new MessageRecord(
                "0012345678",
                0,
                "+27838968976",
                "invalid-number",
                "Abandoned message",
                MessageRecord.Status.DISREGARDED
        );

        assertEquals("invalid-number", message.recipient());

        assertEquals(
                MessageRecord.Status.DISREGARDED,
                message.status()
        );
    }

    @Test
    @DisplayName("Blank disregarded messages have an empty hash word component")
    void handlesBlankDisregardedMessage() {
        MessageRecord message = new MessageRecord(
                "0012345678",
                0,
                "+27838968976",
                "",
                "   ",
                MessageRecord.Status.DISREGARDED
        );

        assertEquals("00:0:", message.messageHash());
    }

    @Test
    @DisplayName("Hash generation handles extra spaces and punctuation")
    void handlesWhitespaceAndPunctuation() {
        MessageRecord message = new MessageRecord(
                "9876543210",
                4,
                "+27838968976",
                "+27834557896",
                "  Hello,   Mike!  ",
                MessageRecord.Status.STORED
        );

        assertEquals("98:4:HELLOMIKE", message.messageHash());
    }
}