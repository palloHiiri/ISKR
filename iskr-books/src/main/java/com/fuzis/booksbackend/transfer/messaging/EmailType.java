package com.fuzis.booksbackend.transfer.messaging;

public enum EmailType {
    VerifyEmailEmail,
    ResetPasswordTokenEmail,
    None;

    public static EmailType getByTokenType(String type){
        return switch (type){
            case "verify_email_token" -> EmailType.VerifyEmailEmail;
            case "reset_password_token" -> EmailType.ResetPasswordTokenEmail;
            default -> EmailType.None;
        };
    }
}
