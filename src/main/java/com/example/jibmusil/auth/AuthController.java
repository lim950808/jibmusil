package com.example.jibmusil.auth;

import com.example.jibmusil.security.JwtUtil;
import com.example.jibmusil.user.User;
import com.example.jibmusil.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        // 임시로 패스워드 인코딩 없이 저장 (나중에 수정)
        userRepository.save(user);
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody User loginUser) {
        User user = userRepository.findByUsername(loginUser.getUsername()).orElseThrow();
        // 임시로 단순 문자열 비교 (나중에 수정)
        if (loginUser.getPassword().equals(user.getPassword())) {
            return ResponseEntity.ok(jwtUtil.generateToken(user.getUsername()));
        }
        return ResponseEntity.badRequest().body("Invalid credentials");
    }
}