package com.example.bval.service;

import com.example.bval.entity.User;
import org.apache.bval.jsr.ApacheValidationProvider;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class ValidationService {
    private final Validator validator;

    public ValidationService() {
        // 1. 获取验证工厂
        ValidatorFactory validatorFactory = Validation.byProvider(ApacheValidationProvider.class)
                .configure().buildValidatorFactory();
        // 2. 获取验证器
        this.validator = validatorFactory.getValidator();
    }

    /**
     * 验证整个对象
     */
    public Set<ConstraintViolation<User>> validateUser(User user) {
        return validator.validate(user);
    }

    /**
     * 验证指定属性
     */
    public Set<ConstraintViolation<User>> validateProperty(User user, String propertyName) {
        return validator.validateProperty(user, propertyName);
    }

    /**
     * 验证值
     */
    public Set<ConstraintViolation<User>> validateValue(String propertyName, Object value) {
        return validator.validateValue(User.class, propertyName, value);
    }
}
