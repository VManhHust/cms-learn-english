package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.User;
import com.example.cmslearnenglish.entity.enums.Role;
import com.example.cmslearnenglish.entity.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    long countByProExpiresAtAfter(Instant now);

    Page<User> findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String email, String displayName, Pageable pageable);

    @Query("""
            select user
            from User user
            where (:emailEmpty = true or lower(user.email) like concat('%', lower(:email), '%'))
              and (:displayNameEmpty = true or lower(user.displayName) like concat('%', lower(:displayName), '%'))
              and (:rolesEmpty = true or user.role in :roles)
              and (:statusesEmpty = true or user.status in :statuses)
              and (
                    :pro is null
                    or (
                        :pro = true
                        and user.proExpiresAt is not null
                        and user.proExpiresAt > :now
                        and (user.proStartsAt is null or user.proStartsAt <= :now)
                    )
                    or (
                        :pro = false
                        and (
                            user.proExpiresAt is null
                            or user.proExpiresAt <= :now
                            or user.proStartsAt > :now
                        )
                    )
              )
            """)
    Page<User> findByFilters(
            @Param("email") String email,
            @Param("emailEmpty") boolean emailEmpty,
            @Param("displayName") String displayName,
            @Param("displayNameEmpty") boolean displayNameEmpty,
            @Param("roles") List<Role> roles,
            @Param("rolesEmpty") boolean rolesEmpty,
            @Param("statuses") List<UserStatus> statuses,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("pro") Boolean pro,
            @Param("now") Instant now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
