package com.mycompany.progpoepart1;

import java.util.Arrays;
import java.util.Objects;

/**
 * Author : keabetswe Mayisela
 * Part 3: stores and searches messages using arrays.
 *
 * No assignment sample messages are hard-coded here.
 * The application and JSON loader supply records through add().
 *
 * Message IDs and hashes are parallel arrays:
 * index i refers to the same message in allMessages[i].
 */
public class MessageRepository {

    private MessageRecord[] allMessages = new MessageRecord[0];

    private MessageRecord[] sentMessages = new MessageRecord[0];
    private MessageRecord[] disregardedMessages = new MessageRecord[0];
    private MessageRecord[] storedMessages = new MessageRecord[0];

    private String[] messageIDs = new String[0];
    private String[] messageHashes = new String[0];

    /**
     * Adds a message and updates the required arrays.
     *
     * Duplicate IDs are rejected so searches and deletion
     * cannot accidentally target the wrong message.
     */
    public void add(MessageRecord message) {
        Objects.requireNonNull(message, "Message is required.");

        if (findByMessageID(message.messageID()) != null) {
            throw new IllegalArgumentException(
                    "A message with this ID already exists."
            );
        }

        allMessages = append(allMessages, message);
        rebuildArrays();
    }

    /**
     * Searches the parallel ID array and returns the matching record.
     * Returns null when no message matches.
     */
    public MessageRecord findByMessageID(String messageID) {
        if (messageID == null) {
            return null;
        }

        for (int i = 0; i < messageIDs.length; i++) {
            if (messageIDs[i].equals(messageID)) {
                return allMessages[i];
            }
        }

        return null;
    }

    /**
     * Returns the recipient and message requested by the ID-search feature.
     */
    public String searchByMessageID(String messageID) {
        MessageRecord message = findByMessageID(messageID);

        if (message == null) {
            return "Message not found.";
        }

        return "Recipient: " + message.recipient()
                + System.lineSeparator()
                + "Message: " + message.text();
    }

    /**
     * Finds sent or stored messages for a recipient.
     *
     * Disregarded messages are excluded, as required by the
     * recipient-search test.
     */
    public MessageRecord[] findByRecipient(String recipient) {
        MessageRecord[] matches = new MessageRecord[0];

        if (recipient == null) {
            return matches;
        }

        for (MessageRecord message : allMessages) {
            if (message.status() != MessageRecord.Status.DISREGARDED
                    && message.recipient().equals(recipient)) {
                matches = append(matches, message);
            }
        }

        return matches;
    }

    /**
     * Searches only the stored-message array for a recipient.
     * Used by the Stored Messages submenu.
     */
    public MessageRecord[] findStoredByRecipient(String recipient) {
        MessageRecord[] matches = new MessageRecord[0];

        if (recipient == null) {
            return matches;
        }

        for (MessageRecord message : storedMessages) {
            if (message.recipient().equals(recipient)) {
                matches = append(matches, message);
            }
        }

        return matches;
    }

    /**
     * Finds the longest stored message.
     *
     * If lengths are equal, the earliest added message wins.
     * Returns null when no stored messages exist.
     */
    public MessageRecord findLongestStoredMessage() {
        MessageRecord longest = null;

        for (MessageRecord message : storedMessages) {
            if (longest == null
                    || message.text().length() > longest.text().length()) {
                longest = message;
            }
        }

        return longest;
    }

    public String displayLongestStoredMessage() {
        MessageRecord longest = findLongestStoredMessage();

        return longest == null
                ? "No stored messages."
                : longest.text();
    }

    /**
     * Displays the sender and recipient of every stored message.
     */
    public String displayStoredSendersAndRecipients() {
        if (storedMessages.length == 0) {
            return "No stored messages.";
        }

        StringBuilder report = new StringBuilder();

        for (MessageRecord message : storedMessages) {
            if (report.length() > 0) {
                report.append(System.lineSeparator());
            }

            report.append("Sender: ")
                    .append(message.sender())
                    .append(", Recipient: ")
                    .append(message.recipient());
        }

        return report.toString();
    }

    /**
     * Deletes a message from the in-memory arrays using its hash.
     *
     * Hashes can collide because the assignment's hash contains
     * only part of the ID and message. Ambiguous deletion is rejected.
     *
     * Persistent JSON deletion will be handled by the storage class
     * when this method is connected to the application.
     */
    public String deleteByMessageHash(String hash) {
        if (hash == null) {
            return "Message not found.";
        }

        int matchingIndex = -1;

        for (int i = 0; i < messageHashes.length; i++) {
            if (messageHashes[i].equals(hash)) {
                if (matchingIndex != -1) {
                    return "Multiple messages have this hash; "
                            + "no message was deleted.";
                }

                matchingIndex = i;
            }
        }

        if (matchingIndex == -1) {
            return "Message not found.";
        }

        MessageRecord deleted = allMessages[matchingIndex];

        MessageRecord[] remaining =
                new MessageRecord[allMessages.length - 1];

        /*
         * Copy every record except the matching message.
         * Rebuilding then keeps all parallel arrays aligned.
         */
        int destination = 0;

        for (int source = 0; source < allMessages.length; source++) {
            if (source != matchingIndex) {
                remaining[destination] = allMessages[source];
                destination++;
            }
        }

        allMessages = remaining;
        rebuildArrays();

        return "Message: \"" + deleted.text()
                + "\" successfully deleted.";
    }

    /**
     * Full report for the Stored Messages submenu.
     */
    public String displayStoredReport() {
        return buildReport(storedMessages, "No stored messages.");
    }

    /**
     * Report required by the assignment's sent-message report test.
     */
    public String displaySentReport() {
        return buildReport(sentMessages, "No sent messages.");
    }

    /**
     * Returns message texts for straightforward assertArrayEquals tests.
     */
    public String[] getSentMessageTexts() {
        String[] texts = new String[sentMessages.length];

        for (int i = 0; i < sentMessages.length; i++) {
            texts[i] = sentMessages[i].text();
        }

        return texts;
    }

    /*
     * Defensive copies prevent callers from replacing elements
     * inside the repository's arrays.
     *
     * MessageRecord itself is immutable.
     */
    public MessageRecord[] getSentMessages() {
        return Arrays.copyOf(sentMessages, sentMessages.length);
    }

    public MessageRecord[] getDisregardedMessages() {
        return Arrays.copyOf(
                disregardedMessages,
                disregardedMessages.length
        );
    }

    public MessageRecord[] getStoredMessages() {
        return Arrays.copyOf(storedMessages, storedMessages.length);
    }

    public String[] getMessageIDs() {
        return Arrays.copyOf(messageIDs, messageIDs.length);
    }

    public String[] getMessageHashes() {
        return Arrays.copyOf(messageHashes, messageHashes.length);
    }

    /**
     * Rebuilds the status arrays and parallel tracking arrays
     * after addition or deletion.
     */
    private void rebuildArrays() {
        sentMessages = new MessageRecord[0];
        disregardedMessages = new MessageRecord[0];
        storedMessages = new MessageRecord[0];

        messageIDs = new String[allMessages.length];
        messageHashes = new String[allMessages.length];

        for (int i = 0; i < allMessages.length; i++) {
            MessageRecord message = allMessages[i];

            messageIDs[i] = message.messageID();
            messageHashes[i] = message.messageHash();

            switch (message.status()) {
                case SENT ->
                    sentMessages = append(sentMessages, message);

                case DISREGARDED ->
                    disregardedMessages =
                            append(disregardedMessages, message);

                case STORED ->
                    storedMessages = append(storedMessages, message);
            }
        }
    }

    /**
     * Expands an array and appends a record.
     */
    private static MessageRecord[] append(
            MessageRecord[] existing,
            MessageRecord message) {

        MessageRecord[] expanded =
                Arrays.copyOf(existing, existing.length + 1);

        expanded[existing.length] = message;
        return expanded;
    }

    private static String buildReport(
            MessageRecord[] messages,
            String emptyMessage) {

        if (messages.length == 0) {
            return emptyMessage;
        }

        StringBuilder report = new StringBuilder();

        for (MessageRecord message : messages) {
            if (report.length() > 0) {
                report.append(System.lineSeparator())
                        .append(System.lineSeparator());
            }

            report.append(message.fullDetails());
        }

        return report.toString();
    }
}