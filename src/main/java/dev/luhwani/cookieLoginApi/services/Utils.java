package dev.luhwani.cookieLoginApi.services;

import java.util.regex.Pattern;

//trying to make this class useless

public final class Utils {

    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^(?=.*[A-Za-z])[A-Za-z0-9_-]{5,14}$");

    private Utils() {
    }

    public static boolean validEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean validUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean validPassword(String password) {
        if (password == null) {
            return false;
        }
        if (password.length() < 8 || password.length() > 64) {
            return false;
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasWhitespace = password.chars().anyMatch(Character::isWhitespace);

        return hasLetter && hasDigit && !hasWhitespace;
    }

}

/*
@Valid

@NotBlank

@Email

@Size

a cusom annotation for validiity
 */