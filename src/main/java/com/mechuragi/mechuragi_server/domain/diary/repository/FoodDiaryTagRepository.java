package com.mechuragi.mechuragi_server.domain.diary.repository;

import com.mechuragi.mechuragi_server.domain.diary.entity.FoodDiaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodDiaryTagRepository extends JpaRepository<FoodDiaryTag, Long> {

    /**
     * 특정 일기의 모든 태그 삭제
     * @param diaryId 일기 ID
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FoodDiaryTag dt WHERE dt.foodDiary.id = :diaryId")
    void deleteAllByFoodDiaryId(@Param("diaryId") Long diaryId);
}
