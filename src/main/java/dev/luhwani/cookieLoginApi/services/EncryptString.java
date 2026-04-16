package dev.luhwani.cookieLoginApi.services;

import java.util.Scanner;
import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncryptString {

    // this class was created for those who wish to run this project
    // to create an admin, of course you would need to add a password hash to your
    // postgres query
    // so, this class' only function is for you to type the password you wish the
    // admin
    // to have, and you copy it from the console, and run the query in the
    // schema.sql file
    // in the resources folder

    private static BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static Pattern passwordPattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,30}$");

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            String password;
            while (true) {
                System.out.print("""
                        Requirements for admin password
                        1. Uppercase letter
                        2. Lowercase letter
                        3. Any digit
                        4. Between 8-30 characters long: """);
                password = scanner.nextLine();
                if (passwordPattern.matcher(password).matches()) {
                    break;
                }
                System.out.println("Invalid password!");
            }
            String passwordHash = encoder.encode(password);
            System.out.println("Hash of your password: " +  passwordHash);
            System.out.println("Ensure to copy the hash and add it to your postgres query in the resources folder");
        }
    }

}
