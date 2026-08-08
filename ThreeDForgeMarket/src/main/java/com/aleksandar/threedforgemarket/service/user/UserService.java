package com.aleksandar.threedforgemarket.service.user;

import com.aleksandar.threedforgemarket.exception.auth.EmailAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.auth.PasswordsDoNotMatchException;
import com.aleksandar.threedforgemarket.exception.auth.UserOperationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.auth.UserNotFoundException;
import com.aleksandar.threedforgemarket.exception.auth.UsernameAlreadyExistsException;
import com.aleksandar.threedforgemarket.model.dto.auth.RegisterRequest;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.model.user.AdminUserListItemDto;
import com.aleksandar.threedforgemarket.model.user.AdminUserSearchRequest;
import com.aleksandar.threedforgemarket.model.user.EditProfileRequest;
import com.aleksandar.threedforgemarket.model.user.ProfileDto;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

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

        User savedUser = userRepository.save(user);
        LOGGER.info("Registered user account id={} with role={}", savedUser.getId(), savedUser.getRole());
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void updateLastLogin(UUID userId) {
        User user = findUserById(userId);

        user.setLastLoginOn(LocalDateTime.now());
    }

    public List<AdminUserListItemDto> getUsersForAdmin(
            AdminUserSearchRequest searchRequest,
            UUID currentAdminId
    ) {
        findActiveAdminById(currentAdminId);

        LocalDateTime createdFrom = startOfDay(searchRequest.getCreatedFrom());
        LocalDateTime createdToExclusive = startOfNextDay(searchRequest.getCreatedTo());
        LocalDateTime lastLoginFrom = startOfDay(searchRequest.getLastLoginFrom());
        LocalDateTime lastLoginToExclusive = startOfNextDay(searchRequest.getLastLoginTo());

        return userRepository.findUsersForAdmin(
                        normalizeKeyword(searchRequest.getKeyword()),
                        searchRequest.getRole(),
                        searchRequest.getActive(),
                        createdFrom,
                        createdToExclusive,
                        lastLoginFrom,
                        lastLoginToExclusive
                )
                .stream()
                .map(user -> toAdminUserListItemDto(user, currentAdminId))
                .toList();
    }

    @Transactional
    public void changeUserRole(
            UUID targetUserId,
            UserRole newRole,
            UUID currentAdminId
    ) {
        findActiveAdminById(currentAdminId);

        if (newRole == null) {
            throw new UserOperationNotAllowedException("Please choose a valid role.");
        }

        if (currentAdminId.equals(targetUserId)) {
            throw new UserOperationNotAllowedException(
                    "You cannot change your own role."
            );
        }

        User targetUser = findUserById(targetUserId);

        if (targetUser.getRole() == UserRole.ADMIN
                && newRole != UserRole.ADMIN
                && targetUser.isActive()
                && isLastActiveAdmin()) {
            throw new UserOperationNotAllowedException(
                    "At least one active administrator must remain."
            );
        }

        targetUser.setRole(newRole);
        LOGGER.info("Changed user role to {} for user id={}", newRole, targetUserId);
    }

    @Transactional
    public void deactivateUser(UUID targetUserId, UUID currentAdminId) {
        findActiveAdminById(currentAdminId);

        if (currentAdminId.equals(targetUserId)) {
            throw new UserOperationNotAllowedException(
                    "You cannot deactivate your own account."
            );
        }

        User targetUser = findUserById(targetUserId);

        if (!targetUser.isActive()) {
            return;
        }

        if (targetUser.getRole() == UserRole.ADMIN && isLastActiveAdmin()) {
            throw new UserOperationNotAllowedException(
                    "At least one active administrator must remain."
            );
        }

        targetUser.setActive(false);
        targetUser.setDeactivatedOn(LocalDateTime.now());
        LOGGER.info("Deactivated user account id={}", targetUserId);
    }

    @Transactional
    public void reactivateUser(UUID targetUserId, UUID currentAdminId) {
        findActiveAdminById(currentAdminId);

        User targetUser = findUserById(targetUserId);

        if (targetUser.isActive()) {
            return;
        }

        targetUser.setActive(true);
        targetUser.setDeactivatedOn(null);
        LOGGER.info("Reactivated user account id={}", targetUserId);
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
        LOGGER.info("Updated profile for user id={}", savedUser.getId());

        return toProfileDto(savedUser);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private User findActiveAdminById(UUID userId) {
        User user = findUserById(userId);

        if (!user.isActive() || user.getRole() != UserRole.ADMIN) {
            throw new UserOperationNotAllowedException(
                    "Only administrators can manage user accounts."
            );
        }

        return user;
    }

    private boolean isLastActiveAdmin() {
        return userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.strip();
    }

    private LocalDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }

        return date.atStartOfDay();
    }

    private LocalDateTime startOfNextDay(LocalDate date) {
        if (date == null) {
            return null;
        }

        return date.plusDays(1).atStartOfDay();
    }

    private AdminUserListItemDto toAdminUserListItemDto(
            User user,
            UUID currentAdminId
    ) {
        return AdminUserListItemDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .createdOn(user.getCreatedOn())
                .lastLoginOn(user.getLastLoginOn())
                .deactivatedOn(user.getDeactivatedOn())
                .currentUser(user.getId().equals(currentAdminId))
                .build();
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
