package org.wiki.controller;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.wiki.manager.SseEmitterManager;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/sse")
public class SseController {

    private final SseEmitterManager sseEmitterManager;

    public SseController(SseEmitterManager sseEmitterManager) {
        this.sseEmitterManager = sseEmitterManager;
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(BAD_REQUEST, "userId 不能为空");
        }
        return sseEmitterManager.createConnection(userId.trim());
    }
}
