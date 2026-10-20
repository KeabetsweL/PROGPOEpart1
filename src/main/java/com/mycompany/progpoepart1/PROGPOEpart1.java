package com.mycompany.progpoepart1;

import java.io.PrintStream;
import java.util.Scanner;

/**
 * Author: Keabetswe Mayisela
 * QuickChat application integrating Part 1 and Part 2.
 *
 * Part 2 requirements:
 * 1. Messaging is available only after successful login.
 * 2. Display "Welcome to QuickChat."
 * 3. Provide the numeric menu: Send, Coming Soon, Quit.
 * 4. Use a while loop to keep the menu running until Quit.
 * 5. Capture the requested message count at session startup.
 * 6. Use a for loop to enter only that number of messages.
 * 7. Display sent-message details and the total successfully sent.
 */
public class PROGPOEpart1 {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            run(scanner, System.out, new Message());
        }
    }

    /**
     * Separates console interaction from main().
     *
     * JUnit tests can supply simulated input, capture output and use
     * temporary message storage without opening dialogs.
     */
    public static void run(
            Scanner scanner,
            PrintStream output,
            Message messages) {

        Login login = registerUser(scanner, output);

        output.println("QuickChat - Login");
        output.print("Username: ");
        String username = scanner.nextLine();

        output.print("Password: ");
        String password = scanner.nextLine();

        boolean authenticated = login.loginUser(username, password);
        output.println(login.returnLoginStatus());

        /*
         * Part 2 requirement 1:
         * Stop before displaying the messaging menu if login fails.
         */
        if (!authenticated || !login.isLoggedIn()) {
            output.println("Please restart the application to log in again.");
            return;
        }

        output.println("Welcome to QuickChat.");

        int requestedMessages = readInteger(
                scanner,
                output,
                "How many messages would you like to enter? ",
                1,
                Integer.MAX_VALUE
        );

        boolean running = true;
        boolean messageBatchEntered = false;

        /*
         * Part 2 requirement 4:
         * The menu repeats until the user selects option 3.
         */
        while (running) {
            output.println();
            output.println("1) Send Messages");
            output.println("2) Show recently sent messages");
            output.println("3) Quit");

            int option = readInteger(
                    scanner,
                    output,
                    "Choose an option: ",
                    1,
                    3
            );

            switch (option) {
                case 1 -> {
                    /*
                     * Prevents selecting option 1 again from exceeding
                     * the number requested at session startup.
                     */
                    if (messageBatchEntered) {
                        output.println(
                                "You have already entered the requested "
                                        + "number of messages."
                        );
                    } else {
                        enterMessages(
                                scanner,
                                output,
                                messages,
                                requestedMessages
                        );

                        messageBatchEntered = true;

                        output.println(
                                "Total messages sent: "
                                        + messages.returnTotalMessagess()
                        );
                    }
                }

                case 2 -> output.println("Coming Soon.");

                case 3 -> {
                    output.println(
                            "Total messages sent: "
                                    + messages.returnTotalMessagess()
                    );
                    output.println("Goodbye.");
                    running = false;
                }

                default -> throw new IllegalStateException(
                        "Unexpected menu option.");
            }
        }
    }

    /**
     * Preserves Part 1 registration and retries invalid fields.
     */
    private static Login registerUser(
            Scanner scanner,
            PrintStream output) {

        Login login = new Login();

        output.println("QuickChat - Registration");

        output.print("First name: ");
        login.setFirstName(scanner.nextLine().trim());

        output.print("Last name: ");
        login.setLastName(scanner.nextLine().trim());

        boolean validUsername = false;

        while (!validUsername) {
            output.print("Username: ");
            login.setUsername(scanner.nextLine());

            validUsername = login.checkUserName();

            if (validUsername) {
                output.println("Username successfully captured.");
            } else {
                output.println(
                        "Username is not correctly formatted; please ensure "
                                + "that your username contains an underscore "
                                + "and is no more than five characters in length."
                );
            }
        }

        boolean validPassword = false;

        while (!validPassword) {
            output.print("Password: ");
            login.setPassword(scanner.nextLine());

            validPassword = login.checkPasswordComplexity();

            if (validPassword) {
                output.println("Password successfully captured.");
            } else {
                output.println(
                        "Password is not correctly formatted; please ensure "
                                + "that the password contains at least eight "
                                + "characters, a capital letter, a number, "
                                + "and a special character."
                );
            }
        }

        boolean validCellphone = false;

        while (!validCellphone) {
            output.print("Cellphone (+27...): ");
            login.setCellPhone(scanner.nextLine());

            validCellphone = login.checkCellPhoneNumber();

            if (validCellphone) {
                output.println("Cell number successfully captured.");
            } else {
                output.println(
                        "Cell number is incorrectly formatted or does not "
                                + "contain an international code; please "
                                + "correct the number and try again."
                );
            }
        }

        output.println(login.registerUser());
        return login;
    }

    /**
     * The for loop controls both the number of messages entered
     * and the zero-based message number used in each hash.
     */
    private static void enterMessages(
            Scanner scanner,
            PrintStream output,
            Message messages,
            int requestedMessages) {

        for (int number = 0; number < requestedMessages; number++) {
            output.println();
            output.println(
                    "Message " + (number + 1) + " of " + requestedMessages
            );

            output.print("Recipient (+27...): ");
            String recipient = scanner.nextLine();

            output.print("Message: ");
            String text = scanner.nextLine();

            messages.startMessage(number, recipient, text);

            output.println(
                    "Message ID generated: " + messages.getMessageID()
            );

            boolean processed = false;

            /*
             * Invalid details can be corrected without consuming
             * another message slot in the outer for loop.
             */
            while (!processed) {
                output.println("1) Send Message");
                output.println("2) Disregard Message");
                output.println("3) Store Message");

                int action = readInteger(
                        scanner,
                        output,
                        "Choose an action: ",
                        1,
                        3
                );

                String result = messages.SentMessage(action);
                output.println(result);

                switch (result) {
                    case "Message successfully sent." -> {
                        output.println(messages.currentMessageDetails());
                        processed = true;
                    }

                    case "Press 0 to delete the message." -> {
                        readInteger(
                                scanner,
                                output,
                                "Enter 0 to confirm: ",
                                0,
                                0
                        );

                        output.println("Message disregarded.");
                        processed = true;
                    }

                    case "Message successfully stored." -> processed = true;

                    default -> {
                        output.println("1) Correct message details");
                        output.println("2) Choose another action");

                        int recovery = readInteger(
                                scanner,
                                output,
                                "Choose an option: ",
                                1,
                                2
                        );

                        if (recovery == 1) {
                            output.print("Recipient (+27...): ");
                            recipient = scanner.nextLine();

                            output.print("Message: ");
                            text = scanner.nextLine();

                            /*
                             * Retains the loop's message number.
                             * startMessage generates a fresh tracking ID.
                             */
                            messages.startMessage(number, recipient, text);

                            output.println(
                                    "Message ID generated: "
                                            + messages.getMessageID()
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * Reads a whole line to avoid Scanner's nextInt()/nextLine()
     * newline problem.
     *
     * Invalid text and out-of-range numbers are rejected without
     * terminating the application.
     */
    private static int readInteger(
            Scanner scanner,
            PrintStream output,
            String prompt,
            int minimum,
            int maximum) {

        while (true) {
            output.print(prompt);
            String input = scanner.nextLine();

            try {
                int value = Integer.parseInt(input.trim());

                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                // Continue to the validation message and retry.
            }

            if (minimum == maximum) {
                output.println("Please enter " + minimum + ".");
            } else {
                output.println(
                        "Please enter a whole number from "
                                + minimum + " to " + maximum + "."
                );
            }
        }
    }
}