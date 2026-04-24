package com.example.flowlong.service;

import com.example.flowlong.common.PageBuilder;
import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.PostManageRequest;
import com.example.flowlong.controller.dto.PostQueryRequest;
import com.example.flowlong.entity.Department;
import com.example.flowlong.entity.Post;
import com.example.flowlong.repository.DepartmentRepository;
import com.example.flowlong.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private final Object defaultPostsLock = new Object();

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Post> getAllPosts() {
        ensureDefaultPosts();
        return postRepository.findAll();
    }

    public List<Post> getEnabledPosts() {
        ensureDefaultPosts();
        return postRepository.findByStatus(1);
    }

    public PageResult<Post> queryPosts(PostQueryRequest query) {
        ensureDefaultPosts();
        return PageBuilder.build(
                query,
                postRepository.findPageByCondition(query),
                postRepository.countByCondition(query)
        );
    }

    public Post createPost(PostManageRequest request) {
        ensureDefaultPosts();
        validatePostRequest(request, null);
        LocalDateTime now = LocalDateTime.now();
        Post post = new Post();
        post.setName(request.getName().trim());
        post.setCode(request.getCode().trim());
        post.setDepartmentId(request.getDepartmentId());
        post.setLevel(normalizeText(request.getLevel()));
        post.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        post.setDescription(normalizeText(request.getDescription()));
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        return postRepository.save(post);
    }

    public Post updatePost(Long id, PostManageRequest request) {
        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("岗位不存在"));
        validatePostRequest(request, id);
        post.setName(request.getName().trim());
        post.setCode(request.getCode().trim());
        post.setDepartmentId(request.getDepartmentId());
        post.setLevel(normalizeText(request.getLevel()));
        post.setStatus(request.getStatus() == null ? post.getStatus() : request.getStatus());
        post.setDescription(normalizeText(request.getDescription()));
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    public void deletePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("岗位不存在"));
        postRepository.delete(post);
    }

    public long countPosts() {
        ensureDefaultPosts();
        return postRepository.findAll().size();
    }

    private void validatePostRequest(PostManageRequest request, Long currentId) {
        if (request == null) {
            throw new RuntimeException("请求不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("岗位名称不能为空");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new RuntimeException("岗位编码不能为空");
        }
        if (request.getDepartmentId() == null || request.getDepartmentId() <= 0) {
            throw new RuntimeException("所属部门不能为空");
        }
        departmentRepository.findById(request.getDepartmentId()).orElseThrow(() -> new RuntimeException("所属部门不存在"));
        Post sameCode = postRepository.findByCode(request.getCode().trim());
        if (sameCode != null && !sameCode.getId().equals(currentId)) {
            throw new RuntimeException("岗位编码已存在");
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void ensureDefaultPosts() {
        synchronized (defaultPostsLock) {
            departmentService.getAllDepartments();
            if (!postRepository.findAll().isEmpty()) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            Department rd = departmentRepository.findByCode("RD");
            Department hr = departmentRepository.findByCode("HR");
            if (rd != null) {
                postRepository.save(buildPost("流程产品经理", "FLOW_PM", rd.getId(), "P6", "负责流程方案设计", now));
                postRepository.save(buildPost("后端开发工程师", "JAVA_DEV", rd.getId(), "P5", "负责后端服务建设", now));
            }
            if (hr != null) {
                postRepository.save(buildPost("HRBP", "HRBP", hr.getId(), "P4", "负责组织与岗位配置", now));
            }
        }
    }

    private Post buildPost(String name, String code, Long departmentId, String level, String description, LocalDateTime now) {
        Post post = new Post();
        post.setName(name);
        post.setCode(code);
        post.setDepartmentId(departmentId);
        post.setLevel(level);
        post.setStatus(1);
        post.setDescription(description);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        return post;
    }
}
