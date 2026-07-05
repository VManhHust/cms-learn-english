package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.RoomDto;
import com.example.cmslearnenglish.dto.SpeakingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpeakingService {

    public SpeakingResponse getSpeakingData(Long userId) {
        // Stub
        return new SpeakingResponse(0, List.of());
    }

    public List<RoomDto> getRooms() {
        return List.of();
    }

    public RoomDto createRoom(Long userId, String roomName, int maxMembers, boolean isPublic) {
        // Stub
        return new RoomDto(null, roomName, maxMembers, isPublic, 1);
    }
}
