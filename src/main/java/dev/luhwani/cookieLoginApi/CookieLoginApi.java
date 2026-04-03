package dev.luhwani.cookieLoginApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CookieLoginApi {

	public static void main(String[] args) {
		SpringApplication.run(CookieLoginApi.class, args);
	}

}

// Hold up, is there a need to rate limit potected endpoints, cuz no matter what, Spring security
// keeps sending responses that this is an unauthorized access, but doesn't that slow the system
// dowm already. And... can't someone try to brute force a protected endpoint
// Also ask Ai what it means when they tell you null annotation types have been detected

// the usual patterns
// same eail, same ip
// same email, different ips
// same ip, different emails
// different email, different ip


// For unregistered emails, I can track failed login attempts by email and block the email after a certain number of failures.
// for same ip, but different mails, just do the same, but ip as the counter
// Maybe a recaptcha for automated bot atttempts for the ip and email,
// then for different ips and mails let's say the endpoint itself will calculate how many failed attempts within
// a particular time frame, if it's high, it'll block the endpoint compltely
// don't forget to log for any of these
