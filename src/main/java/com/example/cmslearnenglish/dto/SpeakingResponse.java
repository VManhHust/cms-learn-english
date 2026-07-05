package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeakingResponse {
    private int totalSessions;
    private List<RoomDto> recentRooms;
}
