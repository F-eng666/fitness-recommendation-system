package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.ArticleRequest;
import com.fitness.dto.CommentRequest;

public interface ArticleService {
    Result getArticleList(Integer page, Integer size, String category, String keyword, String orderBy);
    Result getArticleDetail(Long id);
    Result createArticle(Long userId, ArticleRequest request);
    Result updateArticle(Long id, ArticleRequest request, Long userId);
    Result deleteArticle(Long id, Long userId);
    Result toggleLike(Long articleId, Long userId);
    Result createComment(Long articleId, Long userId, CommentRequest request);
    Result getComments(Long articleId, Integer page, Integer size);
    Result getMyArticles(Long userId, Integer page, Integer size);
}