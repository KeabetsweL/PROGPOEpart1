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
        
    }
}
