package com.linqibin.blog.post.application;

import java.util.List;

import com.linqibin.blog.post.domain.Post;

public record ImportOutcome(Post post, List<String> warnings) {
}
