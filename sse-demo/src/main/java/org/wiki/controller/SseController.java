package org.wiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.wiki.manager.SseEmitterManager;

@RestController
@RequestMapping("/sse")
public class SseController {
    
    @Autowired
    private SseEmitterManager sseEmitterManager;
    
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String userId) {
        return sseEmitterManager.createConnection(userId);
    }
}