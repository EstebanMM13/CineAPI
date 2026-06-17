package com.estebanmm13.auth_service.services.user;


import com.estebanmm13.auth_service.dtoModels.request.UserRequestDTO;
import com.estebanmm13.auth_service.dtoModels.response.UserResponseDTO;
import com.estebanmm13.auth_service.error.UserNotFoundException;
import com.estebanmm13.auth_service.mapper.UserMapper;
import com.estebanmm13.auth_service.models.Role;
import com.estebanmm13.auth_service.models.User;
import com.estebanmm13.auth_service.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import static com.estebanmm13.auth_service.error.UserNotFoundException.*;


@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Page<UserResponseDTO> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponseDTO);
    }

    @Override
    public UserResponseDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(String.format(NOT_FOUND_BY_ID, id)));
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        User user = userMapper.toEntity(userRequestDTO);
        // Encriptar contraseña aquí si no lo hace un listener
        User saved = userRepository.save(user);
        return userMapper.toResponseDTO(saved);
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(String.format(NOT_FOUND_BY_ID, id)));
        userMapper.updateEntity(existingUser, userRequestDTO);
        User updated = userRepository.save(existingUser);
        return userMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(String.format(NOT_FOUND_BY_ID, id));
        }
        userRepository.deleteById(id);
    }


    @Override
    public UserResponseDTO findUserByUsernameIgnoreCase(String username) {
        User user = userRepository.findUserByUsernameIgnoreCaseContaining(username)
                .orElseThrow(() -> new UserNotFoundException(String.format(NOT_FOUND_BY_USERNAME, username)));
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO findUserByEmail(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(String.format(NOT_FOUND_BY_EMAIL, email)));
        return userMapper.toResponseDTO(user);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }

    @Override
    public User findUserEntityByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + username));
    }

    @Override
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findUserByUsernameIgnoreCaseContaining(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + username));
    }
}