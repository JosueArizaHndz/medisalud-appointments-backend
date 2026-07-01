package com.medisalud.appointments.application.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Operación exitosa", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Recurso creado exitosamente", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(true, "Operación exitosa sin contenido", null, LocalDateTime.now());
    }
}
