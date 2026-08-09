package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.ArticleRequest;
import com.fitness.dto.CommentRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String orderBy) {
        log.info("Get article list: page={}, size={}, category={}, keyword={}, orderBy={}",
                page, size, category, keyword, orderBy);
        return articleService.getArticleList(page, size, category, keyword, orderBy);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        log.info("Get article detail: id={}", id);
        return articleService.getArticleDetail(id);
    }

    @PostMapping("/create")
    public Result create(@RequestBody @Valid ArticleRequest request) {
        Long userId = getCurrentUserId();
        log.info("Create article: userId={}, title={}", userId, request.getTitle());
        return articleService.createArticle(userId, request);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id,
                         @RequestBody @Valid ArticleRequest request) {
        Long userId = getCurrentUserId();
        log.info("Update article: id={}, userId={}, title={}", id, userId, request.getTitle());
        return articleService.updateArticle(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("Delete article: id={}, userId={}", id, userId);
        return articleService.deleteArticle(id, userId);
    }

    @PostMapping("/{id}/like")
    public Result toggleLike(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("Toggle like: articleId={}, userId={}", id, userId);
        return articleService.toggleLike(id, userId);
    }

    @GetMapping("/{id}/comments")
    public Result comments(@PathVariable Long id,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size) {
        log.info("Get comments: articleId={}, page={}, size={}", id, page, size);
        return articleService.getComments(id, page, size);
    }

    @PostMapping("/{id}/comment")
    public Result addComment(@PathVariable Long id,
                             @RequestBody @Valid CommentRequest request) {
        Long userId = getCurrentUserId();
        log.info("Add comment: articleId={}, userId={}", id, userId);
        return articleService.createComment(id, userId, request);
    }

    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        log.info("Get my articles: userId={}, page={}, size={}", userId, page, size);
        return articleService.getMyArticles(userId, page, size);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "请先登录");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String) {
            String str = (String) principal;
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException e) {
                var user = sysUserMapper.selectByUsername(str);
                if (user != null) {
                    return user.getId();
                }
            }
        }
        throw new BusinessException(401, "无法获取用户信息");
    }
}
