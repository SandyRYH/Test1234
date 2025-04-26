package id.co.bsi.hello_spring.service;

import id.co.bsi.hello_spring.dto.request.LoginRequest;
import id.co.bsi.hello_spring.dto.request.RegisterRequest;
import id.co.bsi.hello_spring.dto.response.LoginResponse;
import id.co.bsi.hello_spring.dto.response.RegisterResponse;
import id.co.bsi.hello_spring.model.User;
import id.co.bsi.hello_spring.model.UserPin;
import id.co.bsi.hello_spring.repository.UserPinRepository;
import id.co.bsi.hello_spring.repository.UserRepository;
import id.co.bsi.hello_spring.util.JwtUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtility jwtUtility;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PinService pinService;

    @Autowired
    private UserPinRepository userPinRepository;


    public ResponseEntity<?> processRegisterWithPin(Map<String, String> payload) {
        String fullName = payload.get("fullName");
        String email = payload.get("email");
        String password = payload.get("password");
        String confirmationPassword = payload.get("confirmationPassword");
        String phone = payload.get("phone");
        String pin = payload.get("pin");

        // Validasi input
        if (fullName == null || fullName.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || confirmationPassword == null || !password.equals(confirmationPassword) ||
                phone == null || pin == null || pin.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields must be provided and valid."));
        }

        if (!pin.matches("\\d{6}")) {
            return ResponseEntity.badRequest().body(Map.of("message", "PIN must be exactly 6 digits."));
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Email already registered."));
        }

        // Simpan user + pin
        String result = saveUserAndPin(fullName, email, password, phone, pin);
        return ResponseEntity.ok(Map.of("message", result));
    }

    public String saveUserAndPin(String fullName, String email, String password, String phone, String pin) {
        String accountnum = generateAccountNum();

        User user = new User();
        user.setAccountnum(accountnum);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setBalance(0);
        userRepository.save(user);

        UserPin userPin = new UserPin();
        userPin.setAccountnum(accountnum);
        userPin.setPinHash(pinService.hashPin(pin));
        userPinRepository.save(userPin);

        return "Registration and PIN setup successful.";
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
        this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(req.getEmail());

        LoginResponse res = new LoginResponse();
        Optional<User> userOpt = userRepository.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            res.setStatus("error");
            res.setMessage("Email not found.");
            return res;
        }

        User user = userOpt.get();

        String token = this.jwtUtility.generateToken(userDetails, user.getAccountnum());

        res.setStatus("success");
        res.setToken(token);
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
