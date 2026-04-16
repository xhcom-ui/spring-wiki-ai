package com.mongo.plus.entity;

import com.gitee.hengboy.mongo.enums.IdType;
import com.gitee.hengboy.mongo.annotation.Field;
import com.gitee.hengboy.mongo.annotation.Id;
import com.gitee.hengboy.mongo.annotation.MongoTable;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体类
 */
@Data
@MongoTable(value = "user")
public class User implements Serializable {

    /**
     * 主键，使用MongoDB的ObjectId
     */
    @Id(type = IdType.AUTO)
    private String id;

    /**
     * 用户名
     */
    @Field(value = "name")
    private String name;

    /**
     * 年龄
     */
    @Field(value = "age")
    private Integer age;

    /**
     * 邮箱
     */
    @Field(value = "email")
    private String email;

    /**
     * 创建时间
     */
    @Field(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @Field(value = "update_time")
    private Date updateTime;

}
