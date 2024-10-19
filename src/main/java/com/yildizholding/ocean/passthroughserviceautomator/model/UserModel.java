package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

import java.util.List;

@Data
public class UserModel {
    private String username;
    private String firstname;
    private String lastname;

    private List<Payment> payments;
}
