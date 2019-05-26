package com.sunrich.pam.pammsidentity.service;

import com.sunrich.pam.common.domain.User;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.common.exception.NotFoundException;
import com.sunrich.pam.pammsidentity.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    User saveOrUpdate(User user) {
        return userRepository.save(user);
    }

    User findByUserName(String userName) {
        Optional<User> optUser = userRepository.findByUserNameAndRecordStatusTrue(userName);
        if (!optUser.isPresent()) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found!");
        }
        return optUser.get();
    }

    User findById(Long id) {
        Optional<User> optUser = userRepository.findById(id);
        if (!optUser.isPresent()) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found!");
        }
        return optUser.get();
    }
}
