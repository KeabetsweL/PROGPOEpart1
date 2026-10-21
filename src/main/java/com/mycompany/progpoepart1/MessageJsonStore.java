package com.mycompany.progpoepart1;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

/**
 * Author: Keabetswe Mayisela
 * Part 3: reads stored JSON messages into a MessageRecord array.
 *
 * Also writes stored messages and deletes their files.
 *
 * JSON library attribution:
 * Google Gson User Guide:
 * https://github.com/google/gson/blob/main/UserGuide.md
 *
 * File operations use java.nio.file.Files.
 *
 * Compatible with the Part 2 JSON fields:
 * messageID, messageNumber, messageHash, recipient and message.
 *
 * New files also retain sender and status.
 */
public class MessageJsonStore {

    private final Path directory;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setStrictness(Strictness.STRICT)
            .create();

    public MessageJsonStore(Path directory) {
        this.directory = Objects.requireNonNull(
                directory,
                "Storage directory is required."
        );
    }

    /**
     * Stores one message per file.
     *
     * CREATE_NEW prevents overwriting an existing message.
     * Only STORED records belong in this directory.
     */
    public Path save(MessageRecord message) throws IOException {
        Objects.requireNonNull(message, "Message is required.");

        if (message.status() != MessageRecord.Status.STORED) {
            throw new IllegalArgumentException(
                    "Only stored messages can be saved here."
            );
        }

        Files.createDirectories(directory);

        JsonObject json = new JsonObject();

        json.addProperty("messageID", message.messageID());
        json.addProperty("messageNumber", message.messageNumber());
        json.addProperty("messageHash", message.messageHash());
        json.addProperty("sender", message.sender());
        json.addProperty("recipient", message.recipient());
        json.addProperty("message", message.text());
        json.addProperty("status", message.status().name());

        Path file = messageFile(message.messageID());

        Files.writeString(
                file,
                gson.toJson(json),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return file;
    }

    /**
     * Reads every JSON file into an actual array.
     *
     * Files are sorted by filename for predictable loading order.
     * A missing directory means there are no stored messages.
     *
     * Invalid files cause an IOException rather than being
     * silently ignored or presented as successful loading.
     */
    public MessageRecord[] loadAll() throws IOException {
        if (Files.notExists(directory)) {
            return new MessageRecord[0];
        }

        Path[] files;

        try (var entries = Files.list(directory)) {
            files = entries
                    .filter(path -> path.getFileName()
                            .toString().endsWith(".json"))
                    .sorted()
                    .toArray(Path[]::new);
        }

        MessageRecord[] messages = new MessageRecord[files.length];

        for (int i = 0; i < files.length; i++) {
            messages[i] = read(files[i]);
        }

        return messages;
    }

    /**
     * Deletes the stored file for a message ID.
     *
     * The application will find the message by hash first,
     * delete its file, and then remove it from the repository.
     *
     * Returns false when the file does not exist.
     */
    public boolean delete(String messageID) throws IOException {
        Path file = messageFile(messageID);

        if (Files.notExists(file)) {
            return false;
        }

        // Validate the file before deleting it.
        read(file);

        Files.delete(file);
        return true;
    }

    /**
     * Reads and validates one stored message.
     */
    private MessageRecord read(Path file) throws IOException {
        String content = Files.readString(
                file,
                StandardCharsets.UTF_8
        );

        try {
            JsonObject json = gson.fromJson(content, JsonObject.class);

            if (json == null) {
                throw new IllegalArgumentException("Empty JSON file.");
            }

            String id = requiredString(json, "messageID");

            String expectedFilename = id + ".json";

            if (!file.getFileName().toString().equals(expectedFilename)) {
                throw new IllegalArgumentException(
                        "Filename does not match the message ID."
                );
            }

            JsonElement numberValue = json.get("messageNumber");

            if (numberValue == null
                    || !numberValue.isJsonPrimitive()
                    || !numberValue.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException(
                        "A numeric messageNumber is required."
                );
            }

            /*
             * intValueExact rejects fractional and out-of-range values
             * instead of truncating them.
             */
            int number = new BigDecimal(
                    numberValue.getAsString()
            ).intValueExact();

            /*
             * Part 2 files did not capture a sender.
             * Do not attribute old messages to the current user.
             */
            String sender = json.has("sender")
                    ? requiredString(json, "sender")
                    : "Unknown sender (Part 2 file)";

            String status = json.has("status")
                    ? requiredString(json, "status")
                    : "STORED";

            if (!"STORED".equals(status)) {
                throw new IllegalArgumentException(
                        "The file must contain a stored message."
                );
            }

            MessageRecord message = new MessageRecord(
                    id,
                    number,
                    sender,
                    requiredString(json, "recipient"),
                    requiredString(json, "message"),
                    MessageRecord.Status.STORED
            );

            String savedHash = requiredString(json, "messageHash");

            if (!message.messageHash().equals(savedHash)) {
                throw new IllegalArgumentException(
                        "Saved hash does not match the message details."
                );
            }

            return message;

        } catch (RuntimeException exception) {
            throw new IOException(
                    "Invalid stored message file: "
                            + file.getFileName(),
                    exception
            );
        }
    }

    /**
     * Rejects missing, null and non-string fields.
     */
    private static String requiredString(
            JsonObject json,
            String field) {

        JsonElement value = json.get(field);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(
                    "A string field is required: " + field
            );
        }

        return value.getAsString();
    }

    /**
     * Numeric IDs prevent path separators from becoming filenames.
     */
    private Path messageFile(String messageID) {
        if (messageID == null
                || !messageID.matches("[0-9]{10}")) {
            throw new IllegalArgumentException(
                    "Message ID must contain exactly ten digits."
            );
        }

        return directory.resolve(messageID + ".json");
    }
}