package org.wiki.controller;

import org.wiki.entity.Memory;
import org.wiki.entity.Event;
import org.wiki.entity.Policy;
import org.wiki.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/memories")
@CrossOrigin(origins = "*") // 允许跨域请求，便于前端访问
public class MemoryController {
    private final MemoryService memoryService;

    @Autowired
    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    // 创建记忆
    @PostMapping
    public Memory createMemory(@RequestBody MemoryRequest request) {
        return memoryService.createMemory(request.getMemory(), request.getScope(), request.getInputObservation());
    }

    // 更新记忆
    @PutMapping("/{id}")
    public Memory updateMemory(@PathVariable Long id, @RequestBody MemoryRequest request) {
        return memoryService.updateMemory(id, request.getMemory(), request.getScope(), request.getInputObservation());
    }

    // 删除记忆
    @DeleteMapping("/{id}")
    public void deleteMemory(@PathVariable Long id, @RequestParam String scope, @RequestParam String inputObservation) {
        memoryService.deleteMemory(id, scope, inputObservation);
    }

    // 获取所有记忆
    @GetMapping
    public List<Memory> getAllMemories(@RequestParam String scope) {
        return memoryService.getAllMemories(scope);
    }

    // 根据 ID 获取记忆
    @GetMapping("/{id}")
    public Memory getMemoryById(@PathVariable Long id) {
        return memoryService.getMemoryById(id);
    }

    // 根据类别获取记忆
    @GetMapping("/category/{category}")
    public List<Memory> getMemoriesByCategory(@PathVariable String category, @RequestParam String scope) {
        return memoryService.getMemoriesByCategory(category, scope);
    }

    // 根据重要性获取记忆
    @GetMapping("/importance/{importance}")
    public List<Memory> getMemoriesByImportance(@PathVariable int importance, @RequestParam String scope) {
        return memoryService.getMemoriesByImportance(importance, scope);
    }

    // 根据时间范围获取记忆
    @GetMapping("/time-range")
    public List<Memory> getMemoriesByTimeRange(
            @RequestParam("start") String start, 
            @RequestParam("end") String end, 
            @RequestParam String scope) {
        LocalDateTime startDateTime = LocalDateTime.parse(start);
        LocalDateTime endDateTime = LocalDateTime.parse(end);
        return memoryService.getMemoriesByTimeRange(startDateTime, endDateTime, scope);
    }

    // 根据类别和重要性获取记忆
    @GetMapping("/category/{category}/importance/{importance}")
    public List<Memory> getMemoriesByCategoryAndImportance(
            @PathVariable String category, 
            @PathVariable int importance, 
            @RequestParam String scope) {
        return memoryService.getMemoriesByCategoryAndImportance(category, importance, scope);
    }

    // 策略管理
    @PostMapping("/policies")
    public Policy createPolicy(@RequestBody Policy policy) {
        return memoryService.createPolicy(policy);
    }

    @PutMapping("/policies/{id}")
    public Policy updatePolicy(@PathVariable Long id, @RequestBody Policy policy) {
        return memoryService.updatePolicy(id, policy);
    }

    @DeleteMapping("/policies/{id}")
    public void deletePolicy(@PathVariable Long id) {
        memoryService.deletePolicy(id);
    }

    @GetMapping("/policies")
    public List<Policy> getAllPolicies() {
        return memoryService.getAllPolicies();
    }

    // 事件管理
    @GetMapping("/events/scope/{scope}")
    public List<Event> getEventsByScope(@PathVariable String scope) {
        return memoryService.getEventsByScope(scope);
    }

    @GetMapping("/events/memory/{memoryId}")
    public List<Event> getEventsByMemoryId(@PathVariable Long memoryId) {
        return memoryService.getEventsByMemoryId(memoryId);
    }



    // 内存请求对象
    public static class MemoryRequest {
        private Memory memory;
        private String scope;
        private String inputObservation;

        public Memory getMemory() {
            return memory;
        }

        public void setMemory(Memory memory) {
            this.memory = memory;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getInputObservation() {
            return inputObservation;
        }

        public void setInputObservation(String inputObservation) {
            this.inputObservation = inputObservation;
        }
    }
}

