package com.linqibin.blog.ai.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateAiSettingsRequest(
        boolean enabled,
        boolean assistantEnabled,
        @Size(max = 500, message = "对话接口地址过长")
        String chatBaseUrl,
        @Size(max = 500, message = "对话密钥过长")
        String chatApiKey,
        @Size(max = 200, message = "对话模型名过长")
        String chatModel,
        @Size(max = 500, message = "向量接口地址过长")
        String embedBaseUrl,
        @Size(max = 500, message = "向量密钥过长")
        String embedApiKey,
        @Size(max = 200, message = "向量模型名过长")
        String embedModel,
        @Min(value = 8, message = "向量维度需在 8 到 4096 之间")
        @Max(value = 4096, message = "向量维度需在 8 到 4096 之间")
        int embedDimensions,
        @Size(max = 2000, message = "助手人设不能超过 2000 个字符")
        String assistantPersona,
        @Min(value = 1, message = "每分钟次数需在 1 到 120 之间")
        @Max(value = 120, message = "每分钟次数需在 1 到 120 之间")
        int ratePerMinute,
        @Min(value = 1, message = "每天次数需在 1 到 2000 之间")
        @Max(value = 2000, message = "每天次数需在 1 到 2000 之间")
        int ratePerDay
) {
}
