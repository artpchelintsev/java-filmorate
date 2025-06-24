package ru.yandex.practicum.filmorate.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private Map<String, String> validationErrors; // Для MethodArgumentNotValidException

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
