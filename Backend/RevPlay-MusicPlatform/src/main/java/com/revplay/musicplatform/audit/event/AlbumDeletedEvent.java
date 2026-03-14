package com.revplay.musicplatform.audit.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AlbumDeletedEvent {
    private final Long albumId;
}
