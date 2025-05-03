package com.gamax.userservice.Service;

import java.util.List;
import java.util.Optional;

public interface IUserRepository<T> {
    T save(T user);
    List<T> getAll();
    Optional<T> getById(long id);
    boolean delete(long id);
    boolean delete(T id);
}
