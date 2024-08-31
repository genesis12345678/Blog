package com.spring.blog.service;

import com.spring.blog.domain.Article;
import com.spring.blog.domain.Comment;
import com.spring.blog.domain.User;
import com.spring.blog.dto.request.CommentRequest;
import com.spring.blog.dto.response.CommentResponse;
import com.spring.blog.repository.BlogRepository;
import com.spring.blog.repository.CommentRepository;
import com.spring.blog.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final EntityManager em;

    @Transactional
    public CommentResponse addComment(Long articleId, CommentRequest request, String email) {

        Article foundArticle = blogRepository.findByIdWithUser(articleId).orElseThrow(
                () -> new EntityNotFoundException("not found article: " + articleId));

        User foundUser = userRepository.findByEmail(email).orElseThrow(
                () -> new EntityNotFoundException("not found user: " + email));

        Comment savedCommend = commentRepository.save(Comment.builder()
                .content(request.getComment())
                .user(foundUser)
                .article(foundArticle)
                .build());

        boolean isAuthor = foundUser.getNickname().equals(foundArticle.getUser().getNickname());

        return new CommentResponse(savedCommend, isAuthor);
    }

    @Transactional
    public void editComment(Long commentId, CommentRequest request) {
        Comment foundComment = commentRepository.findById(commentId).orElseThrow(
                () -> new EntityNotFoundException("not found comment : " + commentId));

        foundComment.editComment(request.getComment());
    }

    @Transactional
    public Comment deleteComment(Long commentId) {
        Comment foundComment = commentRepository.findById(commentId).orElseThrow(
                () -> new EntityNotFoundException("not found comment : " + commentId));

        List<Comment> comments = foundComment.getUser().getComments();
        List<Comment> comments1 = foundComment.getArticle().getComments();
        commentRepository.delete(foundComment);
        return foundComment;
    }
}