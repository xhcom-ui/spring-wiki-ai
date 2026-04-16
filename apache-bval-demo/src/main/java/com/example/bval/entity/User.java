package com.example.bval.entity;

import lombok.Data;
import org.apache.bval.extras.constraints.checkdigit.IBAN;
import org.apache.bval.extras.constraints.checkdigit.Visa;

import javax.validation.constraints.*;

@Data
public class User {
    @NotNull
    private String email;

    @Size(min = 6, message = "密码不能小于6位")
    private String password;

    @Size(min = 1, max = 20)
    private String name;

    @Min(value = 1, message = "年龄必须大于等于1")
    @Max(value = 150, message = "年龄不能超过150")
    private Integer age;

    @Visa(message = "卡号错误")
    private String cardNumber;

    @IBAN(message = "无效的IBAN")
    private String iban;

    public User() {
    }

    public User(String email, String password, String name, Integer age) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.age = age;
    }

    public User(String email, String password, String name, Integer age, String cardNumber, String iban) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.age = age;
        this.cardNumber = cardNumber;
        this.iban = iban;
    }
}
