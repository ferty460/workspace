package com.example.workspace.repository;

import com.example.workspace.model.User;
import com.example.workspace.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final List<User> users;

    public List<User> findAll() {
        return List.copyOf(users);
    }

    public Optional<User> findById(Long id) {
        return users.stream()
                .filter(user -> id.equals(user.getId()))
                .findFirst();
    }

    public Optional<User> findByLogin(String login) {
        return users.stream()
                .filter(user -> login.equals(user.getLogin()))
                .findFirst();
    }

    public User add(User user) {
        user.setId(IdGenerator.generate(User.class));
        users.add(user);

        return user;
    }

    public User update(Long userId, User user) {
        User updatedUser = findById(userId).orElseThrow(
                () -> new NoSuchElementException("User with id " + userId + " not found")
        );

        updatedUser.setLogin(user.getLogin());

        return updatedUser;
    }

    public boolean delete(Long userId) {
        Optional<User> optionalUser = findById(userId);

        if (optionalUser.isPresent()) {
            users.remove(optionalUser.get());
            return true;
        }

        return false;
    }

}
