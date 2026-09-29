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
            System.out.println("Username is not correctly formatted; please ensure that your username does not contain an underscore and is no more than five characters in length.");
        }
        
        System.out.print("Password: "); 
        login.setPassword(scanner.nextLine());
        
        System.out.print("Cellphone (+27...): "); 
        login.setCellPhone(scanner.nextLine());
        
    }
}
