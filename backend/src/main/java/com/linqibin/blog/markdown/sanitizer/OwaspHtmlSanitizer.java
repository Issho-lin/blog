package com.linqibin.blog.markdown.sanitizer;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

// 基于 OWASP Java HTML Sanitizer 的 HTML 清洗实现。
// 使用白名单策略：只允许 Markdown 正常生成的标签和属性，其余一律移除。
public class OwaspHtmlSanitizer implements HtmlSanitizer {

    private final PolicyFactory policy;

    public OwaspHtmlSanitizer() {
        this.policy = buildPolicy();
    }

    private PolicyFactory buildPolicy() {
        return new HtmlPolicyBuilder()
                // 文本结构标签：标题、段落、引用、列表、水平线、换行
                .allowElements(
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "p", "blockquote", "ul", "ol", "li",
                        "hr", "br"
                )
                // 标题允许 id 属性，用于前端锚点跳转和目录导航。
                .allowAttributes("id").onElements("h1", "h2", "h3", "h4", "h5", "h6")
                // 行内格式标签：加粗、斜体、删除线、代码
                .allowElements("strong", "em", "del", "code")
                // 代码块标签：允许 class 属性用于前端语法高亮
                .allowElements("pre")
                .allowAttributes("class").onElements("pre", "code")
                // 图片标签：只允许 src、alt、title 属性
                .allowElements("img")
                .allowAttributes("src", "alt", "title").onElements("img")
                // 链接标签：只允许 href、title，强制添加 rel="nofollow"
                .allowElements("a")
                .allowAttributes("href", "title").onElements("a")
                // GFM 表格相关标签
                .allowElements("table", "thead", "tbody", "tr", "th", "td")
                .allowAttributes("align").onElements("th", "td")
                // 只允许 http、https 和 mailto 协议，禁止 javascript: 等危险协议
                .allowStandardUrlProtocols()
                // 链接统一添加 rel="noopener noreferrer nofollow"，防止钓鱼和权重外泄
                .requireRelNofollowOnLinks()
                .toFactory();
    }

    @Override
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return policy.sanitize(html);
    }
}
