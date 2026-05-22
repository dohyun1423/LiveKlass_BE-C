package com.be_c.liveklass.notification.controller;

import com.be_c.liveklass.notification.dto.NotificationCreateRequest;
import com.be_c.liveklass.notification.dto.NotificationResponse;
import com.be_c.liveklass.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse create(@Valid @RequestBody NotificationCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/users/{userId}")
    public List<NotificationResponse> getByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        return service.getByUser(userId, unreadOnly);
    }

    @GetMapping("/me")
    public List<NotificationResponse> getMine(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        return service.getByUser(userId, unreadOnly);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return service.markRead(id, userId);
    }
}