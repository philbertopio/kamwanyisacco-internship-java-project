package com.kimwanyisacco.dto.request;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRegistrationRequest {

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^[A-Za-z0-9_\\-]{4,50}$",
            message = "Username must be 4–50 characters and may only contain letters, digits, underscores, or hyphens"
    )
    private String username;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email address is too long")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,128}$",
            message = "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a number, and a special character"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    private String fullName;

    @NotBlank(message = "National ID number is required")
    @Pattern(
            regexp = "^[A-Za-z0-9]{6,20}$",
            message = "National ID must be 6–20 alphanumeric characters (letters and digits only)"
    )
    private String nationalId;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+?[0-9]{9,15}$",
            message = "Please enter a valid phone number (e.g. +254712345678 or 0712345678)"
    )
    private String phone;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
}

