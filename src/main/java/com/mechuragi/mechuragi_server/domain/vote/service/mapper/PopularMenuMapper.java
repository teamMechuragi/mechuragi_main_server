package com.mechuragi.mechuragi_server.domain.vote.service.mapper;

import com.mechuragi.mechuragi_server.domain.vote.dto.PopularMenuResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.MenuScoreDTO;
import org.springframework.stereotype.Component;

@Component
public class PopularMenuMapper {

    /**
     * MenuScore를 PopularMenuResponseDTO로 변환
     */
    public PopularMenuResponseDTO toDTO(MenuScoreDTO menuScoreDTO) {
        return PopularMenuResponseDTO.builder()
                .menu(menuScoreDTO.getMenuName())
                .score(menuScoreDTO.getScore())
                .mentionCount(menuScoreDTO.getMentionCount())
                .averageVotePercentage(menuScoreDTO.getAverageVotePercentage())
                .averageRecency(menuScoreDTO.getAverageRecency())
                .build();
    }
}
