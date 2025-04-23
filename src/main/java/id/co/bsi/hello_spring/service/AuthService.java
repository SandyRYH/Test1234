package id.co.bsi.hello_spring.service;

import id.co.bsi.hello_spring.dto.request.LoginRequest;
import id.co.bsi.hello_spring.dto.request.RegisterRequest;
import id.co.bsi.hello_spring.dto.response.LoginResponse;
import id.co.bsi.hello_spring.dto.response.RegisterResponse;
import id.co.bsi.hello_spring.model.User;
import id.co.bsi.hello_spring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public RegisterResponse register(RegisterRequest req) {
        RegisterResponse res = new RegisterResponse();

        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) {
            res.setStatus("error");
            res.setMessage("Full name cannot be empty.");
            return res;
        }

        if (req.getPhone() == null || req.getPhone().toString().trim().isEmpty()) {
            res.setStatus("error");
            res.setMessage("Phone number cannot be empty.");
            return res;
        }

        if (!isValidPassword(req.getPassword())) {
            res.setStatus("error");
            res.setMessage("Password must be at least 8 characters and include uppercase, lowercase, number, and symbol.");
            return res;
        }

        if (req.getPassword() == null || req.getConfirmationPassword() == null || !req.getPassword().equals(req.getConfirmationPassword())) {
            res.setStatus("error");
            res.setMessage("Password and confirmation password do not match.");
            return res;
        }

        Optional<User> existing = userRepository.findByEmail(req.getEmail());
        if (existing.isPresent()) {
            res.setStatus("error");
            res.setMessage("Email already registered.");
            return res;
        }

        String accountnum = generateAccountNum();

        User user = new User();
        user.setAccountnum(accountnum);
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone().toString());
        user.setPasswordHash(hash(req.getPassword()));
        user.setToken(hash(req.getEmail() + ":" + req.getPassword()));
        user.setBalance(0);

        userRepository.save(user);

        res.setStatus("success");
        res.setMessage("Registration successful.");
        res.setAccountnum(accountnum);
        return res;
    }


    private String generateAccountNum() {
        Random random = new Random();
        StringBuilder accountnum = new StringBuilder("7"); // Depan 7
        for (int i = 0; i < 9; i++) {
            accountnum.append(random.nextInt(10)); // Tambah 9 digit random
        }
        return accountnum.toString();
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public LoginResponse login(LoginRequest req) {
        LoginResponse res = new LoginResponse();
        Optional<User> userOpt = userRepository.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            res.setStatus("error");
            res.setMessage("Email not found.");
            return res;
        }

        User user = userOpt.get();
        if (!user.getPasswordHash().equals(hash(req.getPassword()))) {
            res.setStatus("error");
            res.setMessage("Incorrect password.");
            return res;
        }

        res.setStatus("success");
        res.setToken(user.getToken());
        res.setAccountnum(user.getAccountnum());
        res.setMessage("Login successful.");
        return res;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
