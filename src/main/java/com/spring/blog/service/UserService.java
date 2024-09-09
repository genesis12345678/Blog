package com.spring.blog.service;

import com.spring.blog.common.annotation.DuplicateCheck;
import com.spring.blog.common.enums.SocialType;
import com.spring.blog.domain.User;
import com.spring.blog.dto.request.NewPasswordRequest;
import com.spring.blog.dto.response.UserInfoResponse;
import com.spring.blog.exception.duplicate.NicknameDuplicateException;
import com.spring.blog.model.ProviderUser;
import com.spring.blog.repository.ArticleLikesRepository;
import com.spring.blog.repository.BlogQueryRepository;
import com.spring.blog.repository.BlogRepository;
import com.spring.blog.repository.CommentRepository;
import com.spring.blog.repository.UserQueryRepository;
import com.spring.blog.repository.UserRepository;
import com.spring.blog.service.dto.request.EditUserServiceRequest;
import com.spring.blog.service.dto.request.FormAddUserServiceRequest;
import com.spring.blog.service.dto.request.OAuthAddUserServiceRequest;
import com.spring.blog.service.file.FileService;
import com.spring.blog.service.oauth.unlink.OAuth2UnlinkService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final FileService fileService;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommentRepository commentRepository;
    private final UserQueryRepository userQueryRepository;
    private final BlogQueryRepository blogQueryRepository;
    private final OAuth2UnlinkService oAuth2UnlinkService;
    private final ArticleLikesRepository articleLikesRepository;

    @DuplicateCheck
    @Transactional
    public String save(FormAddUserServiceRequest request) {
        User savedUser = userRepository.save(
                User.builder()
                        .email(request.getEmail())
                        .nickname(request.getNickname())
                        .phoneNumber(request.getPhoneNumber())
                        .registrationId(SocialType.NONE)
                        .password(passwordEncoder.encode(request.getPassword()))
                        .build()
        );

        return savedUser.getNickname();
    }

    @Transactional
    public void save(ProviderUser providerUser, SocialType socialType) {
        userRepository.save(
            User.builder()
                    .registrationId(socialType)
                    .email(providerUser.getEmail())
                    .profileImageUrl(providerUser.getProfileImageUrl())
                    .password(passwordEncoder.encode(providerUser.getPassword()))
                    .build()
        );
    }

    @DuplicateCheck
    @Transactional
    public String updateOAuthUser(OAuthAddUserServiceRequest request, String email) {
        User user = findByEmail(email);

        user.updateNickname(request.getNickname());
        user.updatePhoneNumber(request.getPhoneNumber());

        return user.getNickname();
    }

    @Transactional
    public void deleteUser(String email, Authentication authentication) {
        User user = findByEmail(email);

        Long userId = user.getId();

        blogQueryRepository.decreaseArticleLikesByUserId(userId);

        articleLikesRepository.deleteByUserId(userId);
        commentRepository.deleteByUserId(userId);
        blogRepository.deleteByUserId(userId);

        fileService.deleteFile(user.getProfileImageUrl());

        userRepository.delete(user);

        if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
            String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
            String name = oAuth2AuthenticationToken.getPrincipal().getName();

            oAuth2UnlinkService.unlink(registrationId, name);
        }
    }

    @Transactional
    public User editUser(EditUserServiceRequest request, String email) {
        User user = findByEmail(email);

        if (!user.getNickname().equals(request.getNickname()) &&
                userRepository.existsByNickname(request.getNickname())) {
            throw new NicknameDuplicateException();
        }

        user.updateNickname(request.getNickname());

        String oldImageUrl = user.getProfileImageUrl();

        MultipartFile imageFile = request.getFile();

        if (imageFile != null && StringUtils.hasText(imageFile.getOriginalFilename())) {

            String newImageUrl = fileService.saveFile(imageFile, "user/");
            user.updateProfileImageUrl(newImageUrl);

            fileService.deleteFile(oldImageUrl);
        }

        return user;
    }

    @Transactional
    public void setNewPassword(NewPasswordRequest request) {
        User user = findByEmail(request.getEmail());
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    public UserInfoResponse getUserInfo(String name) {
        UserInfoResponse userInfo = userQueryRepository.getUserInfo(name);
        if (userInfo == null) {
            throw new EntityNotFoundException("not found user from " + name);
        }
        return userInfo;
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("not found user from " + userId));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("not found user from " + email));
    }
}