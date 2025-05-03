package com.gamax.userservice.Service;

import com.gamax.userservice.Entity.Admin;
import com.gamax.userservice.Repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;


@Service
public class AdminService extends BaseService<Admin> {
    @Autowired
    private AdminRepository userRepository;
    @Override
    protected JpaRepository<Admin, Long> getRepository() {
        return userRepository;
    }
}