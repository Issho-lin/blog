package com.linqibin.blog.ai.infrastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.linqibin.blog.ai.application.AiCorpusSync;
import com.linqibin.blog.post.domain.Post;

@Component
public class HttpAiCorpusSync implements AiCorpusSync {

    private final AgentClient agentClient;

    public HttpAiCorpusSync(AgentClient agentClient) {
        this.agentClient = agentClient;
    }

    @Override
    public void upsert(Post post) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("slug", post.slug());
        metadata.put("url", "/posts/" + post.slug());
        if (post.excerpt() != null) {
            metadata.put("excerpt", post.excerpt());
        }
        agentClient.upsertDocument(
                post.id(),
                "published",
                post.title(),
                buildText(post),
                metadata
        );
    }

    @Override
    public void delete(UUID postId) {
        agentClient.deleteDocument(postId);
    }

    private static String buildText(Post post) {
        String excerpt = post.excerpt() == null ? "" : post.excerpt().trim();
        String markdown = post.markdownContent() == null ? "" : post.markdownContent();
        if (excerpt.isEmpty()) {
            return markdown;
        }
        return excerpt + "\n\n" + markdown;
    }
}
