package com.caryanam.caryanam_broker.exception;


import com.caryanam.caryanam_broker.dto.ApiResponse;
import com.caryanam.caryanam_broker.dto.ResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {

            ApiResponse<Object> response = ApiResponse.builder()
                    .status("error")
                    .message(ex.getMessage())
                    .data(null)

                    .build();

            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ResponseDto<Map<String, String>>> handleValidationException(
                MethodArgumentNotValidException ex) {

            Map<String, String> errors = new HashMap<>();

            ex.getBindingResult().getFieldErrors()
                    .forEach(error ->
                            errors.put(error.getField(), error.getDefaultMessage())
                    );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>(400, "Validation Failed", errors));
        }


        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ResponseDto<String>> handleRuntimeException(RuntimeException ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>(400, ex.getMessage(), null));
        }


        @ExceptionHandler(Exception.class)
        public ResponseEntity<ResponseDto<String>> handleException(Exception ex) {
            log.error("Unhandled exception", ex);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto<>(500, "Something went wrong", null));
        }

        @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
        public ResponseEntity<ResponseDto<String>> handleBadCredentials() {

            return ResponseEntity.status(401)
                    .body(new ResponseDto<>(401, "Invalid email or password", null));
        }
        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<ResponseDto<String>> handleDuplicateEmail() {
            return ResponseEntity.status(400)
                    .body(new ResponseDto<>(400, "Email already exists", null));
        }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(
                new ResponseDto<>(400, ex.getMessage(), null)
        );
    }

    }




