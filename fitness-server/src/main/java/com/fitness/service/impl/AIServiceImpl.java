package com.fitness.service.impl;

import com.fitness.common.Result;
import com.fitness.config.DeepSeekProperties;
import com.fitness.dto.AIPlanRequest;
import com.fitness.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class AIServiceImpl implements AIService {

    private final DeepSeekProperties deepSeekProperties;

    public AIServiceImpl(DeepSeekProperties deepSeekProperties) {
        this.deepSeekProperties = deepSeekProperties;
    }

    @Override
    public Result generatePlan(AIPlanRequest request, Long userId) {
        BigDecimal heightInMeters = request.getHeight().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal bmi = request.getWeight().divide(
                heightInMeters.multiply(heightInMeters), 2, RoundingMode.HALF_UP);

        String genderStr = (request.getGender() != null && request.getGender() == 1) ? "男" : "女";

        String prompt = buildPrompt(request, bmi, genderStr);

        log.info("AI Plan request: apiUrl={}, model={}", deepSeekProperties.getApiUrl(), deepSeekProperties.getModel());

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepSeekProperties.getApiKey());

            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位拥有10年经验的资深健身教练和营养师，请提供专业、科学、个性化的健身和饮食建议。请使用Markdown格式输出。");

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            List<Map<String, Object>> messages = Arrays.asList(systemMessage, userMessage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekProperties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(deepSeekProperties.getApiUrl(), entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");

                    Map<String, Object> result = new HashMap<>();
                    result.put("plan", content);
                    result.put("bmi", bmi);
                    return Result.success(result);
                }
            }

            return Result.error("AI服务返回数据异常");

        } catch (Exception e) {
            log.error("AI服务调用失败", e);
            return Result.error("AI服务暂时不可用，请稍后重试: " + e.getMessage());
        }
    }

    private String buildPrompt(AIPlanRequest request, BigDecimal bmi, String genderStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位拥有10年经验的资深健身教练和营养师。请根据以下用户信息，制定一份详细、科学、个性化的健身训练和饮食计划：\n\n");
        sb.append("【基本信息】身高：").append(request.getHeight()).append("cm，体重：")
                .append(request.getWeight()).append("kg，BMI：").append(bmi)
                .append("，年龄：").append(request.getAge()).append("岁，性别：").append(genderStr).append("\n\n");
        sb.append("【健身目标】").append(request.getFitnessGoal()).append("\n");
        sb.append("【运动频率】").append(request.getExerciseFrequency()).append("\n");
        sb.append("【可用器材】").append(request.getAvailableEquipment() != null ? request.getAvailableEquipment() : "无").append("\n");
        sb.append("【饮食偏好】").append(request.getDietPreference() != null ? request.getDietPreference() : "无特殊偏好").append("\n");
        sb.append("【补充说明】").append(request.getAdditionalNotes() != null ? request.getAdditionalNotes() : "无").append("\n\n");
        sb.append("请用Markdown格式输出，包含：身体评估、训练计划（详细到每天动作组数）、饮食建议（早中晚+加餐）、注意事项、预期效果");
        return sb.toString();
    }
}
