package com.aleksandar.threedforgemarket.service.user;

import com.aleksandar.threedforgemarket.exception.auth.EmailAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.auth.InvalidLoginCredentialsException;
import com.aleksandar.threedforgemarket.exception.auth.PasswordsDoNotMatchException;
import com.aleksandar.threedforgemarket.exception.auth.UserNotFoundException;
import com.aleksandar.threedforgemarket.exception.auth.UsernameAlreadyExistsException;
import com.aleksandar.threedforgemarket.model.dto.auth.LoginRequest;
import com.aleksandar.threedforgemarket.model.dto.auth.RegisterRequest;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.model.user.EditProfileRequest;
import com.aleksandar.threedforgemarket.model.user.ProfileDto;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest registerRequest) {
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new PasswordsDoNotMatchException();
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameAlreadyExistsException(registerRequest.getUsername());
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException(registerRequest.getEmail());
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.CUSTOMER)
                .build();

        userRepository.save(user);
    }

    public User login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(loginRequest.getUsernameOrEmail()))
                .orElseThrow(InvalidLoginCredentialsException::new);

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidLoginCredentialsException();
        }

        user.setLastLoginOn(LocalDateTime.now());

        return userRepository.save(user);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public ProfileDto getCurrentUserProfile(UUID userId) {
        User user = findUserById(userId);

        return toProfileDto(user);
    }

    public EditProfileRequest getEditProfileRequest(UUID userId) {
        User user = findUserById(userId);

        EditProfileRequest editProfileRequest = new EditProfileRequest();
        editProfileRequest.setUsername(user.getUsername());
        editProfileRequest.setEmail(user.getEmail());

        return editProfileRequest;
    }

    @Transactional
    public ProfileDto updateCurrentUserProfile(
            UUID userId,
            EditProfileRequest request
    ) {
        User user = findUserById(userId);

        String normalizedUsername = request.getUsername().strip();
        String normalizedEmail = request.getEmail().strip();

        if (!user.getUsername().equals(normalizedUsername)
                && userRepository.existsByUsernameAndIdNot(normalizedUsername, userId)) {
            throw new UsernameAlreadyExistsException(normalizedUsername);
        }

        if (!user.getEmail().equals(normalizedEmail)
                && userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);

        User savedUser = userRepository.save(user);

        return toProfileDto(savedUser);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private ProfileDto toProfileDto(User user) {
        return ProfileDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdOn(user.getCreatedOn())
                .lastLoginOn(user.getLastLoginOn())
                .build();
    }
}
