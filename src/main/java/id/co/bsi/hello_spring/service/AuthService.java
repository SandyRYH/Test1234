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
import java.util.*;

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

    // Temp storage backend
    private final Map<String, Map<String, String>> tempRegisterData = new HashMap<>();

    // Step 1: Terima data register, simpan sementara
    public ResponseEntity<?> processRegisterTemp(Map<String, String> payload) {
        String fullName = payload.get("fullName");
        String email = payload.get("email");
        String password = payload.get("password");
        String confirmationPassword = payload.get("confirmationPassword");
        String phone = payload.get("phone");

        if (fullName == null || email == null || password == null || confirmationPassword == null || phone == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields must be provided."));
        }

        if (!password.equals(confirmationPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match."));
        }

        Optional<User> existingEmail = userRepository.findByEmail(email);
        if (existingEmail.isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Email already registered."));
        }

        Optional<User> existingPhone = userRepository.findByPhone(phone);
        if (existingPhone.isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Phone number already registered."));
        }

        String accountnum = generateAccountNum();

        Map<String, String> tempData = new HashMap<>();
        tempData.put("fullName", fullName);
        tempData.put("email", email);
        tempData.put("password", password);
        tempData.put("phone", phone);
        tempRegisterData.put(accountnum, tempData);

        return ResponseEntity.ok(Map.of("accountnum", accountnum, "message", "Proceed to PIN entry."));
    }


    // Step 2: Finalisasi PIN → Simpan DB
    public ResponseEntity<?> saveRegisterWithPinFromTemp(Map<String, String> payload) {
        String accountnum = payload.get("accountnum");
        String pin = payload.get("pin");

        if (accountnum == null || pin == null || pin.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Account number and PIN are required."));
        }

        Map<String, String> tempData = tempRegisterData.get(accountnum);
        if (tempData == null) {
            return ResponseEntity.status(404).body(Map.of("message", "No registration data found for this account number."));
        }

        User user = new User();
        user.setAccountnum(accountnum);
        user.setFullName(tempData.get("fullName"));
        user.setEmail(tempData.get("email"));
        user.setPhone(tempData.get("phone"));
        user.setPasswordHash(passwordEncoder.encode(tempData.get("password")));
        user.setBalance(0);
        userRepository.save(user);

        UserPin userPin = new UserPin();
        userPin.setAccountnum(accountnum);
        userPin.setPinHash(pinService.hashPin(pin));
        userPinRepository.save(userPin);

        tempRegisterData.remove(accountnum);

        return ResponseEntity.ok(Map.of("message", "Registration and PIN setup successful."));
    }

    private String generateAccountNum() {
        Random random = new Random();
        StringBuilder accountnum = new StringBuilder("7");
        for (int i = 0; i < 9; i++) {
            accountnum.append(random.nextInt(10));
        }
        return accountnum.toString();
    }

    // Validasi Password Kuat
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

    // Login Tetap
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
