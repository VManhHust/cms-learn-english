package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.YoutubeChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface YoutubeChannelRepository extends JpaRepository<YoutubeChannel, Long> {
    Optional<YoutubeChannel> findByChannelYoutubeId(String channelYoutubeId);

    @Query("SELECT c FROM YoutubeChannel c ORDER BY c.subscriberCount DESC LIMIT :maxCount")
    List<YoutubeChannel> findOutstandingChannels(int maxCount);
}
