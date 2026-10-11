package com.Practice.URL.repository;

import com.Practice.URL.entity.Click;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickRepository extends JpaRepository<Click, Long> {

    long countByUrlId(Long urlId);

    @Query("select c.clickedAt from Click c where c.url.id = :urlId")
    List<LocalDateTime> findClickedAtByUrlId(@Param("urlId") Long urlId);

    void deleteAllByUrlId(Long urlId);

    @Query("select c.browser, count(c) from Click c where c.url.id = :urlId group by c.browser")
    List<Object[]> countGroupedByBrowser(@Param("urlId") Long urlId);

    @Query("select c.device, count(c) from Click c where c.url.id = :urlId group by c.device")
    List<Object[]> countGroupedByDevice(@Param("urlId") Long urlId);

    @Query("select c.operatingSystem, count(c) from Click c where c.url.id = :urlId group by c.operatingSystem")
    List<Object[]> countGroupedByOperatingSystem(@Param("urlId") Long urlId);

    @Query("select c.referrer, count(c) from Click c where c.url.id = :urlId group by c.referrer")
    List<Object[]> countGroupedByReferrer(@Param("urlId") Long urlId);
}
