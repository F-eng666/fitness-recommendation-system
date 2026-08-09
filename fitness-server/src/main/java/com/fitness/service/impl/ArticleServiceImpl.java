package com.fitness.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.ArticleRequest;
import com.fitness.dto.CommentRequest;
import com.fitness.entity.*;
import com.fitness.mapper.*;
import com.fitness.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleCommentMapper articleCommentMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public Result getArticleList(Integer page, Integer size, String category, String keyword, String orderBy) {
        Page<Article> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, "PUBLISHED");
        if (StringUtils.hasText(category)) {
            wrapper.eq(Article::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or().like(Article::getContent, keyword));
        }
        if ("hot".equals(orderBy)) {
            wrapper.orderByDesc(Article::getLikeCount);
        } else {
            wrapper.orderByDesc(Article::getCreateTime);
        }
        Page<Article> pageResult = articleMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(pageResult));
    }

    @Override
    public Result getArticleDetail(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        articleMapper.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1);

        SysUser author = sysUserMapper.selectById(article.getAuthorId());

        Map<String, Object> result = new HashMap<>();
        result.put("article", article);
        if (author != null) {
            Map<String, Object> authorInfo = new HashMap<>();
            authorInfo.put("nickname", author.getNickname());
            authorInfo.put("avatar", author.getAvatar());

            LambdaQueryWrapper<Article> authorWrapper = new LambdaQueryWrapper<>();
            authorWrapper.eq(Article::getAuthorId, article.getAuthorId())
                    .eq(Article::getStatus, "PUBLISHED");
            List<Article> authorArticles = articleMapper.selectList(authorWrapper);
            int articleCount = authorArticles.size();
            double avgLikes = authorArticles.stream()
                    .mapToInt(a -> a.getLikeCount() != null ? a.getLikeCount() : 0)
                    .average().orElse(0);
            boolean isExpert = articleCount >= 10 && avgLikes >= 50;
            authorInfo.put("isExpert", isExpert);
            authorInfo.put("articleCount", articleCount);
            result.put("author", authorInfo);
        }

        return Result.success(result);
    }

    @Override
    public Result createArticle(Long userId, ArticleRequest request) {
        Article article = new Article();
        BeanUtil.copyProperties(request, article);
        article.setAuthorId(userId);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setFavoriteCount(0);
        article.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "DRAFT");
        article.setIsTop(0);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(article);
        return Result.success(article);
    }

    @Override
    public Result updateArticle(Long id, ArticleRequest request, Long userId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new BusinessException("无权修改该文章");
        }
        BeanUtil.copyProperties(request, article, "id", "authorId", "viewCount", "likeCount",
                "commentCount", "favoriteCount", "isTop", "createTime", "updateTime");
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
        return Result.success(article);
    }

    @Override
    public Result deleteArticle(Long id, Long userId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new BusinessException("无权删除该文章");
        }
        LambdaQueryWrapper<ArticleComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(ArticleComment::getArticleId, id);
        articleCommentMapper.delete(commentWrapper);

        LambdaQueryWrapper<ArticleLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(ArticleLike::getArticleId, id);
        articleLikeMapper.delete(likeWrapper);

        articleMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @Override
    public Result toggleLike(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        ArticleLike existing = articleLikeMapper.selectByArticleAndUser(articleId, userId);
        if (existing != null) {
            articleLikeMapper.deleteByArticleAndUser(articleId, userId);
            articleMapper.updateLikeCount(articleId, -1);
            return Result.success(Map.of("isLiked", false));
        }

        ArticleLike articleLike = new ArticleLike();
        articleLike.setArticleId(articleId);
        articleLike.setUserId(userId);
        articleLike.setCreateTime(LocalDateTime.now());
        articleLikeMapper.insert(articleLike);
        articleMapper.updateLikeCount(articleId, 1);

        return Result.success(Map.of("isLiked", true));
    }

    @Override
    public Result createComment(Long articleId, Long userId, CommentRequest request) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        ArticleComment comment = new ArticleComment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());
        articleCommentMapper.insert(comment);

        articleMapper.updateCommentCount(articleId, 1);

        return Result.success(comment);
    }

    @Override
    public Result getComments(Long articleId, Integer page, Integer size) {
        Page<ArticleComment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ArticleComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleComment::getArticleId, articleId)
                .isNull(ArticleComment::getParentId)
                .orderByDesc(ArticleComment::getCreateTime);
        Page<ArticleComment> topLevelPage = articleCommentMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> nestedComments = new ArrayList<>();
        for (ArticleComment topComment : topLevelPage.getRecords()) {
            Map<String, Object> commentMap = buildCommentMap(topComment);

            List<ArticleComment> replies = articleCommentMapper.selectRepliesByParentId(topComment.getId());
            List<Map<String, Object>> replyMaps = new ArrayList<>();
            if (replies != null) {
                for (ArticleComment reply : replies) {
                    replyMaps.add(buildCommentMap(reply));
                }
            }
            commentMap.put("replies", replyMaps);
            nestedComments.add(commentMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", topLevelPage.getTotal());
        result.put("page", topLevelPage.getCurrent());
        result.put("size", topLevelPage.getSize());
        result.put("records", nestedComments);
        return Result.success(result);
    }

    private Map<String, Object> buildCommentMap(ArticleComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("articleId", comment.getArticleId());
        map.put("userId", comment.getUserId());
        map.put("content", comment.getContent());
        map.put("parentId", comment.getParentId());
        map.put("replyToUserId", comment.getReplyToUserId());
        map.put("likeCount", comment.getLikeCount());
        map.put("createTime", comment.getCreateTime());

        // 填充用户信息
        SysUser user = sysUserMapper.selectById(comment.getUserId());
        if (user != null) {
            map.put("userName", user.getNickname() != null ? user.getNickname() : user.getUsername());
            map.put("userAvatar", user.getAvatar());
        } else {
            map.put("userName", "未知用户");
            map.put("userAvatar", null);
        }

        // 如果是回复，也填充被回复用户的信息
        if (comment.getReplyToUserId() != null) {
            SysUser replyToUser = sysUserMapper.selectById(comment.getReplyToUserId());
            if (replyToUser != null) {
                map.put("replyToUserName", replyToUser.getNickname() != null ? replyToUser.getNickname() : replyToUser.getUsername());
            }
        }

        return map;
    }

    @Override
    public Result getMyArticles(Long userId, Integer page, Integer size) {
        Page<Article> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getAuthorId, userId)
                .orderByDesc(Article::getCreateTime);
        Page<Article> pageResult = articleMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(pageResult));
    }
}
