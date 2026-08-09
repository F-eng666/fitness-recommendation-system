package com.fitness.service;

import com.fitness.common.Result;

public interface FavoriteService {
    Result toggleFavorite(Long userId, String targetType, Long targetId);
    Result getMyFavorites(Long userId, Integer page, Integer size, String targetType);
    Result isFavorited(Long userId, String targetType, Long targetId);
}