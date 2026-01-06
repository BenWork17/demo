package com.baohoanhao.demo.repository;

import com.baohoanhao.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    // Kiểm tra trùng lặp cực nhanh
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // Tìm kiếm dynamic cho chức năng đăng nhập
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.phone = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);
}