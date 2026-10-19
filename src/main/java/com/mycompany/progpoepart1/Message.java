/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.progpoepart1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a message created in the QuickChat application.
 * Part 2 - Sending Messages.
 * @author Keabetswe Maisela
 */
public class Message {
    private String messageID;
    private int messageNumber;
    private String recipient;
    private String messageText;
    private String messageHash;
    
    private static int totalMessagesSent = 0;
    private static final List<Message> sentMessages = new ArrayList<>();
    
    public Message(int messageNumber, String recipient, String messageText) {
        this.messageNumber = messageNumber;
        this.recipient = recipient;
        this.messageText = messageText;

        this.messageID = generateMessageID();
        this.messageHash = createMessageHash();
    }
    
    /**
     * Generates a random ten-digit message ID.
     */
    private String generateMessageID() {
        Random random = new Random();

        long number = 1_000_000_000L
                + (long) (random.nextDouble() * 9_000_000_000L);

        return String.valueOf(number);
    }
    
    /**
     * Creates the required message hash:
     * first two digits of Message ID : message number :
     * first word + last word.
     * @return 
     */
    public String createMessageHash() {

        String[] words = messageText.trim().split("\\s+");

        String firstWord = words[0];
        String lastWord = words[words.length - 1];

        return (messageID.substring(0, 2)
                + ":"
                + messageNumber
                + ":"
                + firstWord
                + lastWord).toUpperCase();
    }
    
    /**
     * Processes the user's decision:
     * 1 = Send
     * 2 = Disregard
     * 3 = Store
     * @param option
     * @return 
     */
    public String SentMessage(int option) {

        switch (option) {

            case 1 -> {
                sentMessages.add(this);
                totalMessagesSent++;
                return "Message successfully sent.";
            }

            case 2 -> {
                return "Press 0 to delete the message.";
            }

            case 3 -> {
                return "Message successfully stored.";
            }

            default -> {
                return "Invalid option.";
            }
        }
    }
    
    /**
     * Validates the recipient's South African cellphone number.
     * Not more than 10 characters
     * Should start with an international dialing code
     * @return 
     */
    public String checkRecipientCell() {

        if (recipient != null
                && recipient.matches("^\\+27[6-8][0-9]{8}$")) {

            return "Cell phone number successfully captured.";
        }

        return "Cell phone number is incorrectly formatted or does not "
                + "contain an international code. Please correct the "
                + "number and try again.";
    }
    
    /**
     * Ensures that the Message ID is not more than ten characters.
     * @return 
     */
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= 10;
    }
    
    /**
     * Checks that the message contains no more than 250 characters.
     * @return 
     */
    public String checkMessageLength() {

        if (messageText != null && messageText.length() <= 250) {
            return "Message ready to send.";
        }

        int excessCharacters = messageText == null
                ? 0
                : messageText.length() - 250;

        return "Message exceeds 250 characters by "
                + excessCharacters
                + "; please reduce the size.";
    }
    
    public String getMessageID() {
        return messageID;
    }
    
    public String getMessageHash() {
        return messageHash;
    }
}
