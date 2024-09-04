package com.spring.blog.controller.validator;

import com.spring.blog.dto.request.AddUserRequest;
import com.spring.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class AddUserValidator implements Validator {

    private final UserService userService;

    @Override
    public boolean supports(Class<?> clazz) {
        return AddUserRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        AddUserRequest request = (AddUserRequest) target;

        if (userService.existsByEmail(request.getEmail())) {
            errors.rejectValue("email", "duplicate", new String[]{"이메일"}, null);
        }
        if (userService.existsByNickname(request.getNickname())) {
            errors.rejectValue("nickname","duplicate",new String[]{"이름"}, null);
        }
        if (userService.existsByPhoneNumber(request.getPhoneNumber())) {
            errors.rejectValue("phoneNumber","duplicate",new String[]{"전화번호"}, null);
        }
    }
}
