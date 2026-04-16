package com.example.bval;

import com.example.bval.entity.User;
import com.example.bval.service.ValidationService;

import javax.validation.ConstraintViolation;
import java.util.Set;

public class TestValidation {
    public static void main(String[] args) {
        ValidationService validationService = new ValidationService();

        // 测试 1：验证整个对象
        System.out.println("=== 测试 1：验证整个对象 ===");
        User user1 = new User("pack@gmail.com", "12345", "pack_xg", 0);
        Set<ConstraintViolation<User>> violations1 = validationService.validateUser(user1);
        violations1.forEach(cv -> System.err.println(cv.getPropertyPath() + ", " + cv.getMessage()));

        // 测试 2：验证指定属性
        System.out.println("\n=== 测试 2：验证指定属性 ===");
        User user2 = new User("pack@gmail.com", "12345", "pack_xg", 0);
        Set<ConstraintViolation<User>> ageViolations = validationService.validateProperty(user2, "age");
        ageViolations.forEach(cv -> System.err.println(cv.getPropertyPath() + ", " + cv.getMessage()));

        // 测试 3：验证值
        System.out.println("\n=== 测试 3：验证值 ===");
        Set<ConstraintViolation<User>> valueViolations = validationService.validateValue("age", 200);
        valueViolations.forEach(cv -> System.err.println(cv.getPropertyPath() + ", " + cv.getMessage()));

        // 测试 4：验证信用卡号
        System.out.println("\n=== 测试 4：验证信用卡号 ===");
        User user4 = new User();
        user4.setCardNumber("4111111111111111"); // 有效的 Visa 卡号
        Set<ConstraintViolation<User>> cardViolations = validationService.validateProperty(user4, "cardNumber");
        if (cardViolations.isEmpty()) {
            System.out.println("信用卡号验证通过");
        } else {
            cardViolations.forEach(cv -> System.err.println(cv.getPropertyPath() + ", " + cv.getMessage()));
        }

        // 测试 5：验证 IBAN
        System.out.println("\n=== 测试 5：验证 IBAN ===");
        User user5 = new User();
        user5.setIban("DE89370400440532013000"); // 有效的 IBAN
        Set<ConstraintViolation<User>> ibanViolations = validationService.validateProperty(user5, "iban");
        if (ibanViolations.isEmpty()) {
            System.out.println("IBAN 验证通过");
        } else {
            ibanViolations.forEach(cv -> System.err.println(cv.getPropertyPath() + ", " + cv.getMessage()));
        }
    }
}
