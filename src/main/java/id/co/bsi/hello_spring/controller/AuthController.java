package id.co.bsi.hello_spring.controller;

import id.co.bsi.hello_spring.dto.request.LoginRequest;
import id.co.bsi.hello_spring.dto.request.RegisterRequest;
import id.co.bsi.hello_spring.dto.response.LoginResponse;
import id.co.bsi.hello_spring.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerTemp(@RequestBody Map<String, String> payload) {
        return authService.processRegisterTemp(payload);
    }

    @PostMapping("/pin")
    public ResponseEntity<?> pinFinalize(@RequestBody Map<String, String> payload) {
        return authService.saveRegisterWithPinFromTemp(payload);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            LoginResponse response = new LoginResponse();
            response.setStatus("error");
            response.setMessage("Email cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        LoginResponse response = this.authService.login(request);
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
