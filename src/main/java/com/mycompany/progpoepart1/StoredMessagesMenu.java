package com.mycompany.progpoepart1;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

/**
 * Author: Keabetswe Mayisela
 * Part 3: console menu for stored-message operations.
 *
 * Uses the repository arrays for searches and reports.
 * Uses MessageJsonStore for persistent deletion.
 *
 * Console input and output are supplied so JUnit tests can
 * exercise the menu without manual typing.
 */
public class StoredMessagesMenu {

    private final MessageRepository repository;
    private final MessageJsonStore store;

    public StoredMessagesMenu(
            MessageRepository repository,
            MessageJsonStore store) {

        this.repository = Objects.requireNonNull(
                repository,
                "A repository is required."
        );

        this.store = Objects.requireNonNull(
                store,
                "A JSON store is required."
        );
    }

    /**
     * Repeats until the user selects 0 to return to the main menu.
     */
    public void run(Scanner scanner, PrintStream output) {
        boolean running = true;

        while (running) {
            output.println();
            output.println("Stored Messages");
            output.println("1) Display senders and recipients");
            output.println("2) Display longest stored message");
            output.println("3) Search by message ID");
            output.println("4) Search stored messages by recipient");
            output.println("5) Delete stored message by hash");
            output.println("6) Display full stored-message report");
            output.println("7) Display sent-message report");
            output.println("8) Search sent or stored messages by recipient");
            output.println("0) Return to main menu");
            output.print("Choose an option: ");

            String option = scanner.nextLine().trim();

            switch (option) {
                case "1" -> output.println(
                        repository.displayStoredSendersAndRecipients()
                );

                case "2" -> output.println(
                        repository.displayLongestStoredMessage()
                );

                case "3" -> {
                    output.print("Message ID: ");

                    String messageID = scanner.nextLine().trim();

                    output.println(
                            repository.searchByMessageID(messageID)
                    );
                }

                case "4" -> {
                    output.print("Recipient: ");

                    String recipient = scanner.nextLine().trim();

                    printMatches(
                            repository.findStoredByRecipient(recipient),
                            output
                    );
                }

                case "5" -> {
                    output.print("Message hash: ");

                    String hash = scanner.nextLine().trim();

                    output.println(deleteStoredByHash(hash));
                }

                case "6" -> output.println(
                        repository.displayStoredReport()
                );

                case "7" -> output.println(
                        repository.displaySentReport()
                );

                case "8" -> {
                    output.print("Recipient: ");

                    String recipient = scanner.nextLine().trim();

                    printMatches(
                            repository.findByRecipient(recipient),
                            output
                    );
                }

                case "0" -> running = false;

                default -> output.println(
                        "Please choose an option from 0 to 8."
                );
            }
        }
    }

    /**
     * Deletes a uniquely identified stored message.
     *
     * Checks every tracked hash because the assignment's hash
     * algorithm can produce collisions.
     *
     * If file deletion fails, the repository is left unchanged.
     */
    public String deleteStoredByHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "Message not found.";
        }

        String[] hashes = repository.getMessageHashes();
        String[] ids = repository.getMessageIDs();

        MessageRecord matchingMessage = null;

        for (int i = 0; i < hashes.length; i++) {
            if (hashes[i].equals(hash)) {
                if (matchingMessage != null) {
                    return "Multiple messages have this hash; "
                            + "no message was deleted.";
                }

                matchingMessage =
                        repository.findByMessageID(ids[i]);
            }
        }

        if (matchingMessage == null) {
            return "Message not found.";
        }

        if (matchingMessage.status() != MessageRecord.Status.STORED) {
            return "This option deletes stored messages only.";
        }

        try {
            /*
             * A missing file is already absent from persistent storage.
             * Removing its remaining in-memory record is therefore valid.
             *
             * An IOException stops deletion before the arrays change.
             */
            store.delete(matchingMessage.messageID());

            return repository.deleteByMessageHash(hash);

        } catch (IOException exception) {
            return "Message could not be deleted; "
                    + "the message remains in the arrays.";
        }
    }

    /**
     * Displays all matching records rather than only the first.
     */
    private static void printMatches(
            MessageRecord[] matches,
            PrintStream output) {

        if (matches.length == 0) {
            output.println("No matching messages.");
            return;
        }

        for (int i = 0; i < matches.length; i++) {
            if (i > 0) {
                output.println();
            }

            output.println(matches[i].fullDetails());
        }
    }
}