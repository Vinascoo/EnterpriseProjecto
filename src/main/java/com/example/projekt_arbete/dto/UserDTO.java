package com.example.projekt_arbete.dto;

import com.example.projekt_arbete.authorities.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDTO(

        @NotBlank(message = "Cannot be empty")
        @Size(message = "Username must be at least 3, not more than 10", min = 3, max = 10)
        String username,

        @NotBlank(message = "Cannot be empty")
        @Size(message = "Username must be at least 3, not more than 10", min = 3, max = 10)
        String password,


        UserRole userRole

        ) {

}
