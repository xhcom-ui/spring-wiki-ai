package com.example.flowlong.service;

import com.example.flowlong.common.PageBuilder;
import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.DepartmentManageRequest;
import com.example.flowlong.controller.dto.DepartmentQueryRequest;
import com.example.flowlong.entity.Department;
import com.example.flowlong.repository.DepartmentRepository;
import com.example.flowlong.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DepartmentService {

    private final Object defaultDepartmentsLock = new Object();

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PostRepository postRepository;

    public List<Department> getAllDepartments() {
        ensureDefaultDepartments();
        return departmentRepository.findAll();
    }

    public List<Department> getEnabledDepartments() {
        ensureDefaultDepartments();
        return departmentRepository.findByStatus(1);
    }

    public PageResult<Department> queryDepartments(DepartmentQueryRequest query) {
        ensureDefaultDepartments();
        return PageBuilder.build(
                query,
                departmentRepository.findPageByCondition(query),
                departmentRepository.countByCondition(query)
        );
    }

    public Department createDepartment(DepartmentManageRequest request) {
        ensureDefaultDepartments();
        validateDepartmentRequest(request, null);
        LocalDateTime now = LocalDateTime.now();
        Department department = new Department();
        department.setName(request.getName().trim());
        department.setCode(request.getCode().trim());
        department.setLeader(normalizeText(request.getLeader()));
        department.setParentId(normalizeParentId(request.getParentId()));
        department.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        department.setDescription(normalizeText(request.getDescription()));
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Long id, DepartmentManageRequest request) {
        Department department = departmentRepository.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        validateDepartmentRequest(request, id);
        department.setName(request.getName().trim());
        department.setCode(request.getCode().trim());
        department.setLeader(normalizeText(request.getLeader()));
        department.setParentId(normalizeParentId(request.getParentId()));
        department.setStatus(request.getStatus() == null ? department.getStatus() : request.getStatus());
        department.setDescription(normalizeText(request.getDescription()));
        department.setUpdatedAt(LocalDateTime.now());
        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id).orElseThrow(() -> new RuntimeException("部门不存在"));
        if (departmentRepository.countChildren(id) > 0) {
            throw new RuntimeException("该部门下存在子部门，无法删除");
        }
        if (postRepository.countByDepartmentId(id) > 0) {
            throw new RuntimeException("该部门下存在岗位，无法删除");
        }
        departmentRepository.delete(department);
    }

    public long countDepartments() {
        ensureDefaultDepartments();
        return departmentRepository.findAll().size();
    }

    private void validateDepartmentRequest(DepartmentManageRequest request, Long currentId) {
        if (request == null) {
            throw new RuntimeException("请求不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("部门名称不能为空");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new RuntimeException("部门编码不能为空");
        }
        Department sameCode = departmentRepository.findByCode(request.getCode().trim());
        if (sameCode != null && !sameCode.getId().equals(currentId)) {
            throw new RuntimeException("部门编码已存在");
        }
        Long parentId = normalizeParentId(request.getParentId());
        if (parentId != null) {
            Department parent = departmentRepository.findById(parentId).orElseThrow(() -> new RuntimeException("上级部门不存在"));
            if (currentId != null && currentId.equals(parent.getId())) {
                throw new RuntimeException("上级部门不能选择自己");
            }
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? 0L : parentId;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void ensureDefaultDepartments() {
        synchronized (defaultDepartmentsLock) {
            if (!departmentRepository.findAll().isEmpty()) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            Department headOffice = buildDepartment("总部", "HQ", "admin", 0L, "企业总部管理部门", now);
            Department savedHeadOffice = departmentRepository.save(headOffice);
            departmentRepository.save(buildDepartment("研发中心", "RD", "manager", savedHeadOffice.getId(), "流程与系统研发团队", now));
            departmentRepository.save(buildDepartment("人力资源部", "HR", "employee", savedHeadOffice.getId(), "组织与人事管理部门", now));
        }
    }

    private Department buildDepartment(String name, String code, String leader, Long parentId, String description, LocalDateTime now) {
        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setLeader(leader);
        department.setParentId(parentId);
        department.setStatus(1);
        department.setDescription(description);
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        return department;
    }
}
