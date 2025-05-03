package com.gamax.userservice.Service;

import com.gamax.userservice.Entity.Client;
import com.gamax.userservice.Repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ClientService extends BaseService<Client> {
        @Autowired
        private ClientRepository userRepository;
        @Override
        protected JpaRepository<Client, Long> getRepository() {
                return userRepository;
        }
}