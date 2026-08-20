package com.linqibin.blog.ai.application;

import java.util.UUID;

import com.linqibin.blog.post.domain.Post;

public interface AiCorpusSync {

    void upsert(Post post);

    void delete(UUID postId);
}
