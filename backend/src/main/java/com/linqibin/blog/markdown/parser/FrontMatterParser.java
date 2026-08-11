package com.linqibin.blog.markdown.parser;

import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.Yaml;

// Front Matter 解析器：从 Markdown 内容中提取 YAML Front Matter 和剩余正文。
// Front Matter 格式为文件开头的三横线包围的 YAML 块：
// ---
// title: Hello
// tags: [a, b]
// ---
// # 正文内容
public class FrontMatterParser {

    private static final String FRONT_MATTER_DELIMITER = "---";

    private final Yaml yaml;

    public FrontMatterParser() {
        this.yaml = new Yaml();
    }

    // 解析 Markdown 内容，返回 Front Matter 元数据和剩余正文。
    // 如果没有 Front Matter，返回空 FrontMatter 和原始内容。
    @SuppressWarnings("unchecked")
    public FrontMatterParseResult parse(String content) {
        Objects.requireNonNull(content, "content 不能为空");

        // 去除 BOM 和开头的空白行。
        String normalized = stripBom(content).stripLeading();
        if (!normalized.startsWith(FRONT_MATTER_DELIMITER)) {
            return FrontMatterParseResult.withoutFrontMatter(content);
        }

        // 找到第二个 --- 的位置（Front Matter 结束标记）。
        int firstDelimiterEnd = normalized.indexOf('\n');
        if (firstDelimiterEnd < 0) {
            return FrontMatterParseResult.withoutFrontMatter(content);
        }

        int secondDelimiterStart = normalized.indexOf(FRONT_MATTER_DELIMITER, firstDelimiterEnd + 1);
        if (secondDelimiterStart < 0) {
            // 没有结束标记，不当作 Front Matter 处理。
            return FrontMatterParseResult.withoutFrontMatter(content);
        }

        // 确保第二个 --- 在行首。
        int lineStart = secondDelimiterStart;
        while (lineStart > firstDelimiterEnd && normalized.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        if (lineStart != secondDelimiterStart) {
            return FrontMatterParseResult.withoutFrontMatter(content);
        }

        String yamlContent = normalized.substring(firstDelimiterEnd + 1, secondDelimiterStart);
        int bodyStart = secondDelimiterStart + FRONT_MATTER_DELIMITER.length();
        // 跳过结束 --- 后面的换行。
        if (bodyStart < normalized.length() && normalized.charAt(bodyStart) == '\n') {
            bodyStart++;
        } else if (bodyStart < normalized.length() && normalized.charAt(bodyStart) == '\r') {
            bodyStart++;
            if (bodyStart < normalized.length() && normalized.charAt(bodyStart) == '\n') {
                bodyStart++;
            }
        }
        String body = bodyStart < normalized.length() ? normalized.substring(bodyStart) : "";
        // 去除正文开头的多余空行，与导出器的 ---\n\n 格式兼容。
        body = body.stripLeading();

        Map<String, Object> rawMap = yaml.load(yamlContent);
        FrontMatter frontMatter = FrontMatter.fromMap(rawMap);
        return new FrontMatterParseResult(frontMatter, body, true);
    }

    private String stripBom(String content) {
        if (content != null && content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }
}
