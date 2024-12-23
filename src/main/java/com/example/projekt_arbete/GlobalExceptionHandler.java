package com.example.projekt_arbete;

import com.example.projekt_arbete.authorities.UserRole;
import com.example.projekt_arbete.dto.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationError(MethodArgumentNotValidException ex, Model model) {
        BindingResult bindingResult = ex.getBindingResult();

        model.addAttribute("roles", UserRole.values());
        model.addAttribute("user", new UserDTO("", "", null));
        model.addAttribute("status", "Form submission failed. Please correct the errors.");

        return "register";
    }
}
