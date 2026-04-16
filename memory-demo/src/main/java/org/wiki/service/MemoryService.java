package org.wiki.service;

import org.wiki.entity.Memory;
import org.wiki.entity.Event;
import org.wiki.entity.Policy;
import org.wiki.repository.MemoryRepository;
import org.wiki.repository.EventRepository;
import org.wiki.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MemoryService {
    private final MemoryRepository memoryRepository;
    private final EventRepository eventRepository;
    private final PolicyRepository policyRepository;

    @Autowired
    public MemoryService(MemoryRepository memoryRepository, EventRepository eventRepository, 
                       PolicyRepository policyRepository) {
        this.memoryRepository = memoryRepository;
        this.eventRepository = eventRepository;
        this.policyRepository = policyRepository;
    }

    // 创建记忆
    public Memory createMemory(Memory memory, String scope, String inputObservation) {
        // 应用写入策略
        Policy writePolicy = getPolicyByScope(scope);
        if (memory.getImportance() < writePolicy.getWriteThreshold()) {
            // 不满足写入阈值，返回null或抛出异常
            return null;
        }

        // 保存记忆
        memory.setCreatedAt(LocalDateTime.now());
        memory.setUpdatedAt(LocalDateTime.now());
        Memory savedMemory = memoryRepository.save(memory);

        // 创建事件记录
        Event event = new Event();
        event.setScope(scope);
        event.setTimestamp(LocalDateTime.now());
        event.setInputObservation(inputObservation);
        event.setSystemAction("CREATE_MEMORY");
        event.setMemoryChange("ADD");
        event.setMemoryId(savedMemory.getId());
        eventRepository.save(event);

        return savedMemory;
    }

    // 更新记忆
    public Memory updateMemory(Long id, Memory memory, String scope, String inputObservation) {
        Memory existingMemory = memoryRepository.findById(id).orElseThrow();
        existingMemory.setContent(memory.getContent());
        existingMemory.setCategory(memory.getCategory());
        existingMemory.setImportance(memory.getImportance());
        existingMemory.setUpdatedAt(LocalDateTime.now());
        Memory updatedMemory = memoryRepository.save(existingMemory);

        // 创建事件记录
        Event event = new Event();
        event.setScope(scope);
        event.setTimestamp(LocalDateTime.now());
        event.setInputObservation(inputObservation);
        event.setSystemAction("UPDATE_MEMORY");
        event.setMemoryChange("UPDATE");
        event.setMemoryId(updatedMemory.getId());
        eventRepository.save(event);

        return updatedMemory;
    }

    // 删除记忆
    public void deleteMemory(Long id, String scope, String inputObservation) {
        // 创建事件记录
        Event event = new Event();
        event.setScope(scope);
        event.setTimestamp(LocalDateTime.now());
        event.setInputObservation(inputObservation);
        event.setSystemAction("DELETE_MEMORY");
        event.setMemoryChange("DELETE");
        event.setMemoryId(id);
        eventRepository.save(event);

        // 删除记忆
        memoryRepository.deleteById(id);


    }

    // 获取所有记忆
    public List<Memory> getAllMemories(String scope) {
        // 应用读取策略
        Policy readPolicy = getPolicyByScope(scope);
        List<Memory> allMemories = memoryRepository.findAll();
        // 应用读取限制
        if (allMemories.size() > readPolicy.getReadLimit()) {
            return allMemories.subList(0, readPolicy.getReadLimit());
        }
        return allMemories;
    }

    // 根据 ID 获取记忆
    public Memory getMemoryById(Long id) {
        return memoryRepository.findById(id).orElseThrow();
    }

    // 根据类别获取记忆
    public List<Memory> getMemoriesByCategory(String category, String scope) {
        // 应用读取策略
        Policy readPolicy = getPolicyByScope(scope);
        List<Memory> memories = memoryRepository.findByCategory(category);
        // 应用读取限制
        if (memories.size() > readPolicy.getReadLimit()) {
            return memories.subList(0, readPolicy.getReadLimit());
        }
        return memories;
    }

    // 根据重要性获取记忆
    public List<Memory> getMemoriesByImportance(int importance, String scope) {
        // 应用读取策略
        Policy readPolicy = getPolicyByScope(scope);
        List<Memory> memories = memoryRepository.findByImportanceGreaterThanEqual(importance);
        // 应用读取限制
        if (memories.size() > readPolicy.getReadLimit()) {
            return memories.subList(0, readPolicy.getReadLimit());
        }
        return memories;
    }

    // 根据时间范围获取记忆
    public List<Memory> getMemoriesByTimeRange(LocalDateTime start, LocalDateTime end, String scope) {
        // 应用读取策略
        Policy readPolicy = getPolicyByScope(scope);
        List<Memory> memories = memoryRepository.findByCreatedAtBetween(start, end);
        // 应用读取限制
        if (memories.size() > readPolicy.getReadLimit()) {
            return memories.subList(0, readPolicy.getReadLimit());
        }
        return memories;
    }

    // 根据类别和重要性获取记忆
    public List<Memory> getMemoriesByCategoryAndImportance(String category, int importance, String scope) {
        // 应用读取策略
        Policy readPolicy = getPolicyByScope(scope);
        List<Memory> memories = memoryRepository.findByCategoryAndImportanceGreaterThanEqual(category, importance);
        // 应用读取限制
        if (memories.size() > readPolicy.getReadLimit()) {
            return memories.subList(0, readPolicy.getReadLimit());
        }
        return memories;
    }

    // 获取策略
    private Policy getPolicyByScope(String scope) {
        List<Policy> policies = policyRepository.findByScope(scope);
        if (policies.isEmpty()) {
            // 如果没有找到对应范围的策略，创建默认策略
            return createDefaultPolicy(scope);
        }
        return policies.get(0);
    }

    // 创建默认策略
    private Policy createDefaultPolicy(String scope) {
        Policy policy = new Policy();
        policy.setName("Default Policy for " + scope);
        policy.setScope(scope);
        policy.setReadLimit(10); // 每次最多读取10条
        policy.setWriteThreshold(3); // 重要性大于等于3才写入
        policy.setUpdateInterval(24); // 24小时更新一次
        policy.setRetentionDays(30); // 保留30天
        policy.setForgettingStrategy("FIFO"); // 先进先出
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        return policyRepository.save(policy);
    }



    // 管理策略
    public Policy createPolicy(Policy policy) {
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        return policyRepository.save(policy);
    }

    public Policy updatePolicy(Long id, Policy policy) {
        Policy existingPolicy = policyRepository.findById(id).orElseThrow();
        existingPolicy.setName(policy.getName());
        existingPolicy.setScope(policy.getScope());
        existingPolicy.setReadLimit(policy.getReadLimit());
        existingPolicy.setWriteThreshold(policy.getWriteThreshold());
        existingPolicy.setUpdateInterval(policy.getUpdateInterval());
        existingPolicy.setRetentionDays(policy.getRetentionDays());
        existingPolicy.setForgettingStrategy(policy.getForgettingStrategy());
        existingPolicy.setUpdatedAt(LocalDateTime.now());
        return policyRepository.save(existingPolicy);
    }

    public void deletePolicy(Long id) {
        policyRepository.deleteById(id);
    }

    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    // 管理事件
    public List<Event> getEventsByScope(String scope) {
        return eventRepository.findByScope(scope);
    }

    public List<Event> getEventsByMemoryId(Long memoryId) {
        return eventRepository.findByMemoryId(memoryId);
    }


}

