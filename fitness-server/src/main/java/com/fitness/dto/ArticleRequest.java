package com.fitness.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ArticleRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private String summary;
    private String category;
    private String tags;
    private String status;
}