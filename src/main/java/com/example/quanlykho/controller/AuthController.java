package com.example.quanlykho.controller;
import com.example.quanlykho.model.Role;
import com.example.quanlykho.model.RoleName;
import com.example.quanlykho.model.User;
import com.example.quanlykho.payload.JwtAuthenticationResponse;
import com.example.quanlykho.payload.LoginRequest;
import com.example.quanlykho.payload.SignUpRequest;
import com.example.quanlykho.repository.RoleRepository;
import com.example.quanlykho.repository.UserRepository;
import com.example.quanlykho.security.JwtTokenProvider;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider tokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String jwt = tokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            // Kiểm tra nếu username đã tồn tại
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username is already taken!");
            }

            // Tạo đối tượng User và mã hóa mật khẩu
            User user = new User();
            user.setUsername(signUpRequest.getUsername());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

            // Gán quyền ROLE_USER cho người dùng mới
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("User Role not set."));

            user.setRoles(Collections.singleton(userRole));

            // Lưu người dùng vào cơ sở dữ liệu
            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while registering the user");
        }
    }
}
