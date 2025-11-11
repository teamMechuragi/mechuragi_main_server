package com.mechuragi.mechuragi_server.domain.vote.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class VoteCompletedEvent {
    private final Long voteId;
    private final String title;
}
