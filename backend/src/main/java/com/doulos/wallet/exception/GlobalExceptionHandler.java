package com.doulos.wallet.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Captura errores de anotaciones @Valid en los DTOs (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorResponseDTO body = ErrorResponseDTO.conCampos(
                HttpStatus.BAD_REQUEST,
                "Error de validación en los datos enviados.",
                request.getRequestURI(),
                errores
        );
        return ResponseEntity.badRequest().body(body);
    }

    // Captura JSON malformado o valores de enum inválidos (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> manejarJsonInvalido(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es inválido o tiene un formato incorrecto.",
                request
        );
    }

    // Captura reglas de negocio inválidas como saldo negativo o cuenta externa (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> manejarArgumentoInvalido(
            IllegalArgumentException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // Captura fallos de login en UsuarioController (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> manejarCredencialesInvalidas(
            BadCredentialsException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos.", request);
    }

    // Captura intentos de operar sobre recursos de otro usuario (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarAccesoDenegado(
            AccessDeniedException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // Captura registros inexistentes por ID (404)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> manejarNoEncontrado(
            EntityNotFoundException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // Captura conflictos de estado como operaciones sobre empleados dados de baja (409)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> manejarConflictoEstado(
            IllegalStateException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // Respaldo para errores inesperados sin exponer stacktraces (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarErrorGeneral(
            Exception ex, HttpServletRequest request) {
        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno inesperado en el servidor.",
                request
        );
    }

    // Captura parámetros de URL o query params con formato inválido
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> manejarTipoParametroInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor o formato inválido.",
                request
        );
    }

    private ResponseEntity<ErrorResponseDTO> construirRespuesta(
            HttpStatus status, String mensaje, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponseDTO.of(status, mensaje, request.getRequestURI()));
    }
}