package com.Practice.URL.repository;

import com.Practice.URL.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select u from Url u
            where u.user.id = :userId
              and (lower(u.title) like :keyword
                or lower(u.description) like :keyword
                or lower(u.originalUrl) like :keyword
                or lower(u.shortCode) like :keyword)
            order by u.createdAt desc
            """)
    List<Url> searchByUserId(@Param("userId") Long userId, @Param("keyword") String keyword);
}