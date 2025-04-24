package id.co.bsi.hello_spring.controller;

import id.co.bsi.hello_spring.dto.request.LoginRequest;
import id.co.bsi.hello_spring.dto.request.RegisterRequest;
import id.co.bsi.hello_spring.dto.response.LoginResponse;
import id.co.bsi.hello_spring.dto.response.RegisterResponse;
import id.co.bsi.hello_spring.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/api/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            RegisterResponse response = new RegisterResponse();
            response.setStatus("error");
            response.setMessage("Email cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        RegisterResponse response = authService.register(request);
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            LoginResponse response = new LoginResponse();
            response.setStatus("error");
            response.setMessage("Email cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        LoginResponse response = this.authService.login(request);
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
