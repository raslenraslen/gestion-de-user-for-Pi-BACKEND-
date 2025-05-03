package com.gamax.userservice.Service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Feign.CommunityServiceClient;
import com.gamax.userservice.TDO.UserDTO;
import feign.FeignException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class BaseService<T> implements IUserRepository<T> {
    @Autowired
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    protected CommunityServiceClient communityServiceClient;

    protected abstract JpaRepository<T, Long> getRepository();

    @Override
    public T save(T user) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null");
        return getRepository().save(user);
    }

    @Override
    public List<T> getAll() {
        return getRepository().findAll();
    }

    @Override
    public Optional<T> getById(long id) {
        return getRepository().findById(id);
    }

    public T update(long id, T entityDetails) {
        if (((User) entityDetails).getPassword() != null)
            ((User) entityDetails).setPassword(
                    passwordEncoder.encode(((User) entityDetails).getPassword())
            );
        T entity = getRepository().findById(id).orElseThrow(() -> new RuntimeException("Entity not found"));
        BeanUtils.copyProperties(entityDetails, entity);

        return getRepository().save(entity);
    }

    @Override
    public boolean delete(long id) {
        Optional<T> user = getRepository().findById(id);
        if (!user.isPresent())
            return false;
        cascadedelete(id);
        getRepository().deleteById(id);
        return true;
    }

    @Override
    public boolean delete(T user) {
        return delete(((User) user).getUserId());
    }

    public UserDTO convertUserToTDO(User user) {
        UserDTO userTDO = new UserDTO(user.getUserId(), user.getFirstName(), user.getLastName());
        return userTDO;
    }

    public boolean cascadedelete(Long userId) {
        FeignFunction(communityServiceClient::cascadeDeleteFromCommunities, userId, "Community Service");
        return true;
    }

    protected <T, R> Optional<?> FeignFunction(Function<T, R> funct, T parametre, String errorText) {
        try {
            return Optional.ofNullable(funct.apply(parametre));
        } catch (FeignException e) {
            throw new RuntimeException("Unable to connect to " + errorText + "\n" + e.getMessage());
        }
    }
}