package com.mechuragi.mechuragi_server.domain.diary.repository;

import com.mechuragi.mechuragi_server.domain.diary.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    // 가장 많이 사용된 태그 조회 (추천 태그용)
    @Query("SELECT t FROM Tag t " +
            "JOIN FoodDiaryTag fdt ON fdt.tag.id = t.id " +
            "GROUP BY t.id " +
            "ORDER BY COUNT(fdt.id) DESC")
    List<Tag> findPopularTags();
}
