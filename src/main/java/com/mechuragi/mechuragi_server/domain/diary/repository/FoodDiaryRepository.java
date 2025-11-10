package com.mechuragi.mechuragi_server.domain.diary.repository;

import com.mechuragi.mechuragi_server.domain.diary.entity.FoodDiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodDiaryRepository extends JpaRepository<FoodDiary, Long> {

    // 특정 사용자의 일기 조회 (권한 체크용)
    Optional<FoodDiary> findByIdAndMemberId(Long id, Long memberId);

    // 특정 년월의 일기 목록 조회 (캘린더용)
    @Query("SELECT DISTINCT fd FROM FoodDiary fd " +
           "LEFT JOIN FETCH fd.images " +
           "WHERE fd.member.id = :memberId " +
           "AND YEAR(fd.diaryDate) = :year " +
           "AND MONTH(fd.diaryDate) = :month " +
           "ORDER BY fd.diaryDate DESC")
    List<FoodDiary> findByMemberIdAndYearMonth(@Param("memberId") Long memberId,
                                                 @Param("year") int year,
                                                 @Param("month") int month);

    // 특정 날짜의 일기 조회 (하루에 하나만 존재)
    Optional<FoodDiary> findByMemberIdAndDiaryDate(Long memberId, LocalDate diaryDate);

    // 일기 상세 조회 (이미지 함께 fetch)
    @Query("SELECT fd FROM FoodDiary fd " +
           "LEFT JOIN FETCH fd.images " +
           "WHERE fd.id = :id")
    Optional<FoodDiary> findByIdWithImages(@Param("id") Long id);
}
