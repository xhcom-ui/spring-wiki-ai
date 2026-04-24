package com.example.flowlong.repository;

import com.example.flowlong.controller.dto.PostQueryRequest;
import com.example.flowlong.entity.Post;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Post selectById(Long id);

    Post findByCode(String code);

    List<Post> findAll();

    List<Post> findByStatus(Integer status);

    List<Post> findPageByCondition(@Param("query") PostQueryRequest query);

    long countByCondition(@Param("query") PostQueryRequest query);

    long countByDepartmentId(Long departmentId);

    int insert(Post post);

    int update(Post post);

    int removeById(Long id);

    default Optional<Post> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default Post save(Post post) {
        if (post.getId() == null) {
            insert(post);
        } else {
            update(post);
        }
        return post;
    }

    default void delete(Post post) {
        if (post != null && post.getId() != null) {
            removeById(post.getId());
        }
    }
}
