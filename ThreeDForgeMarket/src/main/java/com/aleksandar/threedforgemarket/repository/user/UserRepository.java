package com.aleksandar.threedforgemarket.repository.user;

import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    long countByRoleAndActiveTrue(UserRole role);

    @Query("""
            SELECT u
            FROM User u
            WHERE (:keyword IS NULL
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:role IS NULL OR u.role = :role)
                AND (:active IS NULL OR u.active = :active)
                AND (:createdFrom IS NULL OR u.createdOn >= :createdFrom)
                AND (:createdToExclusive IS NULL OR u.createdOn < :createdToExclusive)
                AND (:lastLoginFrom IS NULL OR u.lastLoginOn >= :lastLoginFrom)
                AND (:lastLoginToExclusive IS NULL OR u.lastLoginOn < :lastLoginToExclusive)
            ORDER BY u.createdOn DESC
            """)
    List<User> findUsersForAdmin(
            @Param("keyword") String keyword,
            @Param("role") UserRole role,
            @Param("active") Boolean active,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdToExclusive") LocalDateTime createdToExclusive,
            @Param("lastLoginFrom") LocalDateTime lastLoginFrom,
            @Param("lastLoginToExclusive") LocalDateTime lastLoginToExclusive
    );
}
