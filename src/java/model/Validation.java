package model;

public class Validation {

    public static boolean isEmailValid(String email) {
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    public static boolean isPasswordValid(String Password) {
        return Password.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).{8,}$");
    }

    public static boolean isDouble(String value) {
//        return value.matches("");
        return true;
    }

    public static boolean isInteger(String value) {
//        return value.matches("");
        return true;
    }

    public static boolean isMobileNumberValid(String mobile) {
        return mobile.matches("^07[01245678]{1}[0-9]{7}$");
    }
}
