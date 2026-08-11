package com.linqibin.blog.markdown.parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.Extension;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

// 基于 commonmark-java 的 Markdown 渲染实现。
// 支持 CommonMark 规范 + GFM 表格和删除线扩展，覆盖博客常用的排版需求。
// 渲染后的 HTML 中标题（h1-h6）会自动带上 id 属性，用于前端锚点跳转和目录生成。
public class CommonMarkRenderer implements MarkdownRenderer {

    // 把连续的非字母数字字符压成一个短横线，用于生成标题锚点。
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    // 去掉开头和结尾多余的短横线。
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+|-+$)");

    // Parser 线程安全可复用；HtmlRenderer 每次渲染时通过 AttributeProvider 动态生成标题 ID。
    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public CommonMarkRenderer() {
        // GFM 扩展需要同时注册到 Parser 和 HtmlRenderer 才能生效。
        List<Extension> extensions = List.of(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );
        this.parser = Parser.builder()
                .extensions(extensions)
                .build();
        // 注册标题 ID 属性提供器，渲染时自动为每个标题生成锚点 id。
        this.htmlRenderer = HtmlRenderer.builder()
                .extensions(extensions)
                .attributeProviderFactory(context -> new HeadingIdAttributeProvider())
                .build();
    }

    @Override
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document);
    }

    @Override
    public List<TableOfContentsItem> extractTableOfContents(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        Node document = parser.parse(markdown);

        // 遍历 AST 收集标题，与渲染时的 AttributeProvider 使用相同的锚点生成算法。
        List<TableOfContentsItem> items = new ArrayList<>();
        Set<String> seenAnchors = new LinkedHashSet<>();

        for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof Heading heading) {
                String text = extractText(heading);
                String anchor = ensureUniqueAnchor(generateAnchor(text), seenAnchors);
                // 目录只提取 H2 和 H3，对应前端侧边目录的常见设计。
                if (heading.getLevel() >= 2 && heading.getLevel() <= 3) {
                    items.add(new TableOfContentsItem(heading.getLevel(), text, anchor));
                }
            }
        }
        return List.copyOf(items);
    }

    // 递归收集节点子树中的全部纯文本，用于从标题节点提取可读文本。
    private static String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text textNode) {
                sb.append(textNode.getLiteral());
            } else {
                sb.append(extractText(child));
            }
        }
        return sb.toString();
    }

    // 把标题文本规范化为 URL 安全的锚点：转小写 -> 非字母数字改短横线 -> 去两端短横线。
    private static String generateAnchor(String text) {
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        normalized = EDGE_HYPHENS.matcher(normalized).replaceAll("");
        if (normalized.isEmpty()) {
            normalized = "heading";
        }
        return normalized;
    }

    // 处理重复锚点：同名标题自动追加数字后缀，保证 id 唯一。
    private static String ensureUniqueAnchor(String baseAnchor, Set<String> seenAnchors) {
        if (!seenAnchors.contains(baseAnchor)) {
            seenAnchors.add(baseAnchor);
            return baseAnchor;
        }
        int suffix = 2;
        while (true) {
            String candidate = baseAnchor + "-" + suffix;
            if (!seenAnchors.contains(candidate)) {
                seenAnchors.add(candidate);
                return candidate;
            }
            suffix++;
        }
    }

    // 标题 ID 属性提供器：渲染时为每个标题生成与目录一致的锚点 id。
    // 每次渲染创建新实例，内部状态在单次渲染内有效，保证锚点唯一性。
    private static class HeadingIdAttributeProvider implements AttributeProvider {

        private final Set<String> seenAnchors = new LinkedHashSet<>();

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (node instanceof Heading heading) {
                String text = extractText(heading);
                String anchor = ensureUniqueAnchor(generateAnchor(text), seenAnchors);
                attributes.put("id", anchor);
            }
        }
    }
}
