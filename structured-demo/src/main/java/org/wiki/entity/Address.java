package org.wiki.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String name;
    private String phone;
    private String address;
    private String city;
    private String zipCode;
}