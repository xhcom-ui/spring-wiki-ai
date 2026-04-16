package com.example.bval.controller;

import com.example.bval.entity.User;
import com.example.bval.service.ValidationService;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/users")
public class UserController {

    private final ValidationService validationService;

    public UserController() {
        this.validationService = new ValidationService();
    }

    @PostMapping("/validate")
    public Map<String, Object> validateUser(@RequestBody User user) {
        Set<ConstraintViolation<User>> violations = validationService.validateUser(user);
        Map<String, Object> result = new HashMap<>();
        if (violations.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "验证通过");
        } else {
            result.put("code", 400);
            result.put("msg", "验证失败");
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<User> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            result.put("errors", errors);
        }
        return result;
    }

    @PostMapping("/validate-property")
    public Map<String, Object> validateProperty(@RequestBody Map<String, Object> request) {
        String propertyName = (String) request.get("propertyName");
        User user = new User();
        if (request.containsKey("email")) user.setEmail((String) request.get("email"));
        if (request.containsKey("password")) user.setPassword((String) request.get("password"));
        if (request.containsKey("name")) user.setName((String) request.get("name"));
        if (request.containsKey("age")) user.setAge((Integer) request.get("age"));
        if (request.containsKey("cardNumber")) user.setCardNumber((String) request.get("cardNumber"));
        if (request.containsKey("iban")) user.setIban((String) request.get("iban"));

        Set<ConstraintViolation<User>> violations = validationService.validateProperty(user, propertyName);
        Map<String, Object> result = new HashMap<>();
        if (violations.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "验证通过");
        } else {
            result.put("code", 400);
            result.put("msg", "验证失败");
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<User> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            result.put("errors", errors);
        }
        return result;
    }

    @PostMapping("/validate-value")
    public Map<String, Object> validateValue(@RequestBody Map<String, Object> request) {
        String propertyName = (String) request.get("propertyName");
        Object value = request.get("value");

        Set<ConstraintViolation<User>> violations = validationService.validateValue(propertyName, value);
        Map<String, Object> result = new HashMap<>();
        if (violations.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "验证通过");
        } else {
            result.put("code", 400);
            result.put("msg", "验证失败");
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<User> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            result.put("errors", errors);
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        Set<ConstraintViolation<User>> violations = validationService.validateUser(user);
        Map<String, Object> result = new HashMap<>();
        if (violations.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "用户创建成功");
            result.put("data", user);
        } else {
            result.put("code", 400);
            result.put("msg", "用户创建失败");
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<User> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            result.put("errors", errors);
        }
        return result;
    }
}
