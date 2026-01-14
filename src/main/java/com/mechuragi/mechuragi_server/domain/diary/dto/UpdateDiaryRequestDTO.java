package com.mechuragi.mechuragi_server.domain.diary.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDiaryRequestDTO {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 2000, message = "내용은 2000자 이하여야 합니다")
    private String content;

    @NotNull(message = "별점은 필수입니다")
    @DecimalMin(value = "0.0", message = "별점은 0.0 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다")
    private BigDecimal rating;

    @Size(max = 20, message = "이미지는 최대 20장까지 첨부 가능합니다")
    private List<String> imageUrls;

    @Size(max = 20, message = "태그는 최대 20개까지 추가 가능합니다")
    private List<String> tags;
}
