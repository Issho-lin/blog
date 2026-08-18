package com.linqibin.blog.post.application;

public record ImportedImage(String originalFilename, String contentType, byte[] content) {
}
