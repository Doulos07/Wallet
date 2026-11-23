package com.doulos.wallet.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> erroresCampos
) {
    public static ErrorResponseDTO of(HttpStatus status, String mensaje, String path) {
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                path,
                null
        );
    }

    public static ErrorResponseDTO conCampos(HttpStatus status, String mensaje, String path, Map<String, String> campos) {
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                path,
                campos
        );
    }
}