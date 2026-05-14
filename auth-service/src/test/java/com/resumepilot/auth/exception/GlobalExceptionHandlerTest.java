package com.resumepilot.auth.exception;

import com.resumepilot.auth.dto.ErrorDetails;
import com.resumepilot.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationExceptionReturnsCollectedFieldErrors() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new RegisterRequest(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "Valid email is required"));
        bindingResult.addError(new FieldError("registerRequest", "password", "Password is required"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", RegisterRequest.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(method, 0),
                bindingResult);

        ResponseEntity<ErrorDetails> response = handler.handleValidationException(exception, mock(WebRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("email: Valid email is required");
        assertThat(response.getBody().getMessage()).contains("password: Password is required");
        assertThat(response.getBody().getDetails()).isEqualTo("Validation Failed");
    }

    @Test
    void handleRuntimeExceptionReturnsBadRequest() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/auth/login");

        ResponseEntity<ErrorDetails> response = handler.handleRuntimeException(
                new RuntimeException("Invalid email or password"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().getDetails()).isEqualTo("uri=/auth/login");
    }

    @Test
    void handleGlobalExceptionReturnsInternalServerError() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/auth/register");

        ResponseEntity<ErrorDetails> response = handler.handleGlobalException(new Exception("Unexpected"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected");
        assertThat(response.getBody().getDetails()).isEqualTo("uri=/auth/register");
    }

    @SuppressWarnings("unused")
    private void validationTarget(RegisterRequest request) {
    }
}
