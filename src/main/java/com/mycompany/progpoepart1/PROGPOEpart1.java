/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.progpoepart1;

import java.util.Scanner;

/**
 *
 * @author Keabetswe Maisela
 */
public class PROGPOEpart1 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Login login = new Login();
        System.out.println("QuickChat - Registration");
        System.out.print("First name: "); 
        login.setFirstName(scanner.nextLine().trim());
        
        System.out.print("Last name: "); 
        login.setLastName(scanner.nextLine().trim());
        
        while (true) {
            System.out.print("Username: ");
            login.setUsername(scanner.nextLine());
            if (login.checkUserName()) {
                System.out.println("Username successfully captured.");
                break;
            }
            System.out.println("Username is not correctly formatted; please ensure that your username contains an underscore and is no more than five characters in length.");
        }
        
        while (true) {
            System.out.print("Password: "); 
            login.setPassword(scanner.nextLine());
            
            if (login.checkPasswordComplexity()) { 
                System.out.println("Password successfully captured."); 
                break; 
            }
            
            System.out.println("Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.");
        }
        
        while (true) {
            System.out.print("Cellphone (+27...): "); 
            login.setCellPhone(scanner.nextLine());
            
            if (login.checkCellPhoneNumber()) { 
                System.out.println("Cell phone number successfully added."); 
                break; 
            }
            
            System.out.println("Cell phone number incorrectly formatted or does not contain international code.");
        }
        
        System.out.println(login.registerUser());
        System.out.println("QuickChat - Login");
        System.out.print("Username: "); 
        String username = scanner.nextLine();
        
        System.out.print("Password: ");
        String password = scanner.nextLine();
        boolean loginSuccessful = login.loginUser(username, password);
        System.out.println(login.returnLoginStatus());
        
        // Part 2 is only available after successful login.
        if (!loginSuccessful) {
            System.out.println("You must log in successfully to use QuickChat.");
            scanner.close();
            return;
        }
        
        System.out.println("Welcome to QuickChat");
        
        boolean isSessionActive = true;
        
        int maximumMessages = readNumberOfMessages(scanner);
        int messagesEntered = 0;
        
        // Keep displaying the menu until the user selects Quit.
        while (isSessionActive) {

            System.out.println("\nQuickChat Menu");
            System.out.println("1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.print("Select an option: ");

            String menuChoice = scanner.nextLine();

            switch (menuChoice) {
                case "1" -> {
                    if (messagesEntered >= maximumMessages) {
                        System.out.println("You have entered the maximum "+ "number of messages.");
                        break;
                    }
                    
                    int messagesRemaining = maximumMessages - messagesEntered;

                    /*
                     * The for loop allows the user to enter only
                     * the number of messages specified when the
                     * application started.
                     */
                    for (int i = 0; i < messagesRemaining; i++) {

                        int messageNumber = messagesEntered;

                        System.out.println("\nMessage "+ (messageNumber + 1)+ " of "+ maximumMessages);

                        // Recipient
                        String recipient;

                        while (true) {

                            System.out.print("Enter recipient number (+27...): ");

                            recipient = scanner.nextLine();

                            /*
                             * Create a temporary message so that
                             * recipient validation can be performed.
                             */
                            Message validationMessage = new Message(messageNumber,recipient,"Temporary message");

                            String recipientResult = validationMessage.checkRecipientCell();

                            System.out.println(recipientResult);

                            if (recipientResult.equals("Cell phone number "+ "successfully captured."))
                                break;
                        }
                    }
                }
                    
                case "2" -> /*
                     * Part 2 specifically states that this
                     * functionality is still in development.
                     */
                    System.out.println("Coming Soon.");
                    
                case "3" -> {
                }
                    
                default -> System.out.println("Invalid option. Please select 1, 2 or 3.");
            }
        }
    }
    
    /**
     * Reads the maximum number of messages that the user
     * wishes to enter during this QuickChat session.
     */
    private static int readNumberOfMessages(Scanner scanner) {

        while (true) {

            System.out.print("How many messages would you like to enter? ");

            try {

                int number = Integer.parseInt(scanner.nextLine());

                if (number > 0) {
                    return number;
                }

                System.out.println("Please enter a number greater than 0.");

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
