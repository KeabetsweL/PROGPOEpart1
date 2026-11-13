package com.mycompany.progpoepart1;

import java.util.Locale;

/**
 * Author: Keabetswe Mayisela
 * Represents one message for Part 3.
 *
 * Each record retains the sender, recipient, message ID,
 * message number, text and processing status.
 *
 * Java records provide accessors automatically, for example:
 * message.messageID(), message.recipient() and message.text().
 *
 * A record is immutable: its values cannot change after creation.
 * This prevents later message entry from changing earlier messages.
 */
public record MessageRecord(
        String messageID,
        int messageNumber,
        String sender,
        String recipient,
        String text,
        Status status) {

    /**
     * Identifies the array in which a message belongs.
     */
    public enum Status {
        SENT,
        STORED,
        DISREGARDED
    }

    /**
     * Validates the information required to represent a message.
     *
     * Recipient phone-format validation remains in the sending flow.
     * A disregarded message may contain an invalid recipient and must
     * still be representable in the disregarded-messages array.
     */
    public MessageRecord {
        if (messageID == null || !messageID.matches("[0-9]{10}")) {
            throw new IllegalArgumentException(
                    "Message ID must contain exactly ten digits."
            );
        }

        if (messageNumber < 0) {
            throw new IllegalArgumentException(
                    "Message number cannot be negative."
            );
        }

        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException(
                    "Sender is required."
            );
        }

        if (recipient == null) {
            throw new IllegalArgumentException(
                    "Recipient cannot be null."
            );
        }

        if (text == null) {
            throw new IllegalArgumentException(
                    "Message text cannot be null."
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Message status is required."
            );
        }
    }

    /**
     * Uses the same hash rules as Part 2:
     *
     * First two ID digits : message number : first and last words.
     *
     * Punctuation is removed from the two words and the result
     * is converted to uppercase.
     *
     * Blank disregarded messages have an empty word component.
     */
    public String messageHash() {
        String trimmedText = text.trim();
        String wordComponent = "";

        if (!trimmedText.isEmpty()) {
            String[] words = trimmedText.split("\\s+");

            String firstWord = words[0]
                    .replaceAll("[^\\p{L}\\p{N}]", "");

            String lastWord = words[words.length - 1]
                    .replaceAll("[^\\p{L}\\p{N}]", "");

            wordComponent = firstWord + lastWord;
        }

        return (messageID.substring(0, 2)
                + ":" + messageNumber
                + ":" + wordComponent)
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Full details for Part 3 reports.
     *
     * Includes the hash, recipient and message required by
     * the report test, plus sender, ID and status.
     */
    public String fullDetails() {
        return "Message ID: " + messageID
                + System.lineSeparator()
                + "Message Hash: " + messageHash()
                + System.lineSeparator()
                + "Sender: " + sender
                + System.lineSeparator()
                + "Recipient: " + recipient
                + System.lineSeparator()
                + "Message: " + text
                + System.lineSeparator()
                + "Status: " + status;
    }
}