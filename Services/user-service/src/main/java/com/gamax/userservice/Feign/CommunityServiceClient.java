package com.gamax.userservice.Feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "COMMUNITY-SERVICE")
public interface CommunityServiceClient {
    @DeleteMapping("/api/community/remove-user/{userId}")
    public ResponseEntity<?>cascadeDeleteFromCommunities(@PathVariable Long userId);
}
