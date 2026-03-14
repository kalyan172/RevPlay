package com.revplay.musicplatform.systemplaylist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemPlaylistResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
}

