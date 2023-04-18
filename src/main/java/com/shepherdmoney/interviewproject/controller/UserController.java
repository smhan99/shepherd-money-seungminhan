package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.exception.ApiRequestException;
import com.shepherdmoney.interviewproject.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final UserRepository repository;

    UserController(UserRepository repository) {
        this.repository = repository;
    }

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // Check valid request param
        if (payload == null) throw new ApiRequestException("Payload is null");
        String name = payload.getName();
        String email = payload.getEmail();

        // Required Args will check for valid args
        User newUser = User.of(name, email);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(repository.save(newUser).getId());
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // If user does not exist, throw ApiException
        if (!repository.findById(userId).isPresent()) throw new ApiRequestException("User not found in DB");
        // Delete and return
        repository.deleteById(userId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body("User successfully deleted");
    }
}
