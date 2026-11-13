package com.mycompany.progpoepart1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JOptionPane;

/**
 * Author: Keabetswe Mayisela
 * Part 2: message validation, message hashes, sending and JSON storage.
 *
 * Use one Message object for the application session.
 * Call startMessage() each time the user composes another message.
 *
 * Keeping the sent messages on this object avoids static state leaking
 * between independent application sessions or JUnit tests.
 */
public class Message {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String RECIPIENT_SUCCESS =
            "Cell phone number successfully captured.";

    private static final String RECIPIENT_FAILURE =
            "Cell phone number is incorrectly formatted or does "
                    + "not contain an international code. Please correct "
                    + "the number and try again.";

    private final List<String> sentMessages = new ArrayList<>();
    private final List<String> generatedIds = new ArrayList<>();

    private final Path storageDirectory;
    /*
     * Part 3 integration.
     * These remain unset when running the original Part 2 tests.
     */
    private MessageRepository repository;
    private String sender;

    private String messageID;
    private int messageNumber;
    private String recipient;
    private String messageText;

    /*
     * Prevents the same composed message from being sent or stored twice.
     * startMessage() resets this flag for the next message.
     */
    private boolean completed;

    /**
     * Default location for messages stored for later.
     */
    public Message() {
        this(Path.of("stored-messages"));
    }

    /**
     * Allows JUnit tests to use a temporary directory instead of
     * writing test files into the real application's storage directory.
     */
    public Message(Path storageDirectory) {
        if (storageDirectory == null) {
            throw new IllegalArgumentException(
                    "A storage directory is required.");
        }

        this.storageDirectory = storageDirectory;
    }

    /**
     * Prepares a new message.
     *
     * The application's for loop supplies the message number,
     * starting at zero as shown in the prescribed hash example.
     */
    public void startMessage(int number, String recipient, String text) {
        if (number < 0) {
            throw new IllegalArgumentException(
                    "Message number cannot be negative.");
        }

        this.messageID = generateMessageID();
        this.messageNumber = number;
        this.recipient = recipient;
        this.messageText = text;
        this.completed = false;
    }

    /**
     * Generates ten random digits.
     *
     * A String preserves leading zeroes, which are needed for hashes
     * such as 00:0:HITONIGHT.
     *
     * Checks previously generated IDs to avoid duplicates in this session.
     */
    private String generateMessageID() {
    	
        String candidate;

        do {
            StringBuilder digits = new StringBuilder();

            for (int i = 0; i < 10; i++) {
                digits.append(RANDOM.nextInt(10));
            }

            candidate = digits.toString();
        } while (generatedIds.contains(candidate)
                || (repository != null
                && repository.findByMessageID(candidate) != null));

        generatedIds.add(candidate);
        return candidate;
    }

    /**
     * Prescribed method: verifies that the ID has at most ten characters.
     * Also rejects missing IDs, empty IDs and non-digit characters.
     */
    public boolean checkMessageID() {
        return messageID != null
                && messageID.matches("[0-9]{1,10}");
    }

    /**
     * Accepts the assignment's valid example: +27718693002.
     *
     * Reuses the existing Login cellphone validation so registration
     * and messaging apply the same South African mobile format.
     */
    public String checkRecipientCell() {
        Login validator = new Login();
        validator.setCellPhone(recipient);

        return validator.checkCellPhoneNumber()
                ? RECIPIENT_SUCCESS
                : RECIPIENT_FAILURE;
    }

    /**
     * Returns the exact success/failure wording from the unit-test criteria.
     *
     * Exactly 250 characters is accepted because the requirement says
     * the message should not exceed 250 characters.
     */
    public String checkMessageLength() {
        if (messageText == null || messageText.isBlank()) {
            return "Please enter a message.";
        }

        int excess = messageText.length() - 250;

        if (excess > 0) {
            return "Message exceeds 250 characters by " + excess
                    + "; please reduce the size.";
        }

        return "Message ready to send.";
    }

    /**
     * Builds:
     * first two ID digits : message number : first word + last word
     *
     * Example:
     * ID 0012345678, number 0,
     * "Hi Mike, can you join us for dinner tonight?"
     * produces 00:0:HITONIGHT.
     */
    public String createMessageHash() {
        if (!checkMessageID() || messageID.length() != 10) {
            throw new IllegalStateException(
                    "A ten-digit message ID is required.");
        }

        if (messageText == null || messageText.isBlank()) {
            throw new IllegalStateException(
                    "A message is required to create a hash.");
        }

        String[] words = messageText.trim().split("\\s+");

        String firstWord = words[0].replaceAll("[^\\p{L}\\p{N}]", "");
        String lastWord = words[words.length - 1]
                .replaceAll("[^\\p{L}\\p{N}]", "");

        return (messageID.substring(0, 2)
                + ":" + messageNumber
                + ":" + firstWord + lastWord)
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Prescribed no-argument method.
     * Presents the send, disregard and store choices to the user.
     *
     * Tests use SentMessage(int) so no dialog blocks automated execution.
     */
    public String SentMessage() {
        String[] options = {
                "Send Message",
                "Disregard Message",
                "Store Message"
        };

        int selected = JOptionPane.showOptionDialog(
                null,
                "Choose what to do with this message.",
                "QuickChat",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selected == JOptionPane.CLOSED_OPTION) {
            return "No option selected.";
        }

        return SentMessage(selected + 1);
    }/**
     * Processes the selected action and updates the Part 3 arrays.
    *
    * Only sent messages increase the sent total.
    * Stored messages are written successfully before being added
    * to the stored-message array.
    */
   public String SentMessage(int choice) {
       if (choice < 1 || choice > 3) {
           return "Invalid option. Please choose 1, 2 or 3.";
       }

       if (messageID == null) {
           return "Please create a message first.";
       }

       if (completed) {
           return "This message has already been processed.";
       }

       if (choice == 2) {
           rememberMessage(MessageRecord.Status.DISREGARDED);
           completed = true;
           return "Press 0 to delete the message.";
       }

       if (!RECIPIENT_SUCCESS.equals(checkRecipientCell())) {
           return RECIPIENT_FAILURE;
       }

       String lengthResult = checkMessageLength();

       if (!"Message ready to send.".equals(lengthResult)) {
           return lengthResult;
       }

       if (choice == 1) {
           rememberMessage(MessageRecord.Status.SENT);
           sentMessages.add(currentMessageDetails());
           completed = true;
           return "Message successfully sent.";
       }

       try {
           storeMessage();
           rememberMessage(MessageRecord.Status.STORED);
           completed = true;
           return "Message successfully stored.";

       } catch (IOException exception) {
           return "Message could not be stored. Please try again.";
       }
   }
    /**
     * Returns every successfully sent message in this session.
     * Stored and disregarded messages are excluded.
     */
    public String printMessages() {
        return String.join(
                System.lineSeparator() + System.lineSeparator(),
                sentMessages
        );
    }

    /**
     * Preserves the exact method spelling in the assignment.
     */
    public int returnTotalMessagess() {
        return sentMessages.size();
    }

    /**
     * Details appear in the prescribed order:
     * Message ID, Message Hash, Recipient, Message.
     */
    public String currentMessageDetails() {
        return "Message ID: " + messageID
                + System.lineSeparator()
                + "Message Hash: " + createMessageHash()
                + System.lineSeparator()
                + "Recipient: " + recipient
                + System.lineSeparator()
                + "Message: " + messageText;
    }

    /**
     * Stores one JSON file per message, preserving earlier stored messages.
     *
     * JSON strings escape quotation marks, backslashes and control
     * characters. Files are written as UTF-8.
     *
     * Format reference: RFC 8259, section 7 (JSON strings).
     * Java API references: Files.writeString and StandardOpenOption.
     *
     * This method performs storage only; SentMessage(3) manages
     * the user's action and completion state.
     */
    public Path storeMessage() throws IOException {
        if (!checkMessageID()
                || !RECIPIENT_SUCCESS.equals(checkRecipientCell())
                || !"Message ready to send.".equals(checkMessageLength())) {
            throw new IllegalStateException(
                    "A valid message is required before storage.");
        }
        /*
         * Part 3 storage includes the authenticated sender and status.
         * The original Part 2 JSON format remains available when
         * Part 3 tracking has not been enabled.
         */
        if (repository != null) {
            MessageJsonStore jsonStore =
                    new MessageJsonStore(storageDirectory);

            return jsonStore.save(
                    currentRecord(MessageRecord.Status.STORED)
            );
        }

        Files.createDirectories(storageDirectory);

        String json = "{\n"
                + "  \"messageID\": " + jsonString(messageID) + ",\n"
                + "  \"messageNumber\": " + messageNumber + ",\n"
                + "  \"messageHash\": "
                + jsonString(createMessageHash()) + ",\n"
                + "  \"recipient\": " + jsonString(recipient) + ",\n"
                + "  \"message\": " + jsonString(messageText) + "\n"
                + "}\n";

        Path file = storageDirectory.resolve(messageID + ".json");

        /*
         * CREATE_NEW prevents accidentally overwriting an existing file,
         * including the unlikely case of an ID collision after a restart.
         */
        Files.writeString(
                file,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return file;
    }
    /**
     * Enables Part 3 tracking for this messaging session.
     *
     * Call once, after successful login and before composing messages.
     * The application supplies the authenticated user's cellphone number.
     */
    public void enablePartThree(
            MessageRepository repository,
            String sender) {

        if (repository == null) {
            throw new IllegalArgumentException(
                    "A message repository is required."
            );
        }

        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException(
                    "A sender is required."
            );
        }

        if (this.repository != null || messageID != null) {
            throw new IllegalStateException(
                    "Enable Part 3 once, before composing messages."
            );
        }

        this.repository = repository;
        this.sender = sender;
    }

    /**
     * Allows the application to load and delete JSON files
     * from the same directory used when storing messages.
     */
    public Path getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Takes an immutable snapshot of the current message.
     *
     * Disregarded messages may contain incomplete input.
     */
    private MessageRecord currentRecord(MessageRecord.Status status) {
        return new MessageRecord(
                messageID,
                messageNumber,
                sender,
                recipient == null ? "" : recipient,
                messageText == null ? "" : messageText,
                status
        );
    }

    /**
     * Adds the processed message to the appropriate Part 3 array.
     */
    private void rememberMessage(MessageRecord.Status status) {
        if (repository != null) {
            repository.add(currentRecord(status));
        }
    }

    /**
     * Encodes a Java String as a valid quoted JSON string.
     */
    private static String jsonString(String value) {
        StringBuilder result = new StringBuilder("\"");

        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format(
                                Locale.ROOT,
                                "\\u%04x",
                                (int) character
                        ));
                    } else {
                        result.append(character);
                    }
                }
            }
        }

        return result.append('"').toString();
    }

    public String getMessageID() {
        return messageID;
    }

    public int getMessageNumber() {
        return messageNumber;
    }
}