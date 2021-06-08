package com.example.demo.services;

import com.example.demo.dtos.RegisterRequest;
import com.example.demo.exceptions.SpringRedditException;
import com.example.demo.models.NotificationEmail;
import com.example.demo.models.User;
import com.example.demo.models.VerificationToken;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.VerificationTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;

//    @Autowired // Thanks to the @AllArgsConstructor
//    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
//        this.passwordEncoder = passwordEncoder;
//        this.userRepository = userRepository;
//    }

    @Transactional
    public void signup(RegisterRequest registerRequest) {
        // Create and save a user
        User user = new User();

        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setCreated(Instant.now());
        user.setEnabled(false);

        userRepository.save(user);

        // Create and save a new token
        String token = generateVerificationToken(user);
        String mailSubject = "Please Activate Your Account";
        String mailBody = "Thank you for signing up to Spring Reddit, " +
                "please click on the link below to activate your account: " +
                "http://localhost:8080/api/auth/accountVerification/" + token;

        // Send an email
        mailService.sendMail(new NotificationEmail(mailSubject, user.getEmail(), mailBody));
    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    public void verifyAccount(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);
        verificationToken.orElseThrow(() -> new SpringRedditException("Invalid Token"));

        fetchUserAndEnable(verificationToken.get());
    }

    @Transactional
    public void fetchUserAndEnable(VerificationToken verificationToken) {
        Long userId = verificationToken.getUser().getUserId();
        User user = userRepository.getById(userId);
        user.setEnabled(true);

        userRepository.save(user);
    }
}
