package com.linqibin.blog.post.infrastructure.persistence;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("jpa")
@TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=blog",
        "DB_USER=blog",
        "DB_PASSWORD=blog"
})
class PostJpaPersistenceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private SpringDataPostRepository springDataPostRepository;

    @BeforeEach
    void setUp() {
        springDataPostRepository.deleteAll();
    }

    @Test
    void createDraftPersistsPostToPostgreSql() {
        Post createdPost = postService.createDraft("Integration Draft", "# integration", null, null, null);

        PostEntity savedEntity = springDataPostRepository.findById(createdPost.id()).orElseThrow();

        assertThat(savedEntity.getId()).isEqualTo(createdPost.id());
        assertThat(savedEntity.getTitle()).isEqualTo("Integration Draft");
        assertThat(savedEntity.getSlug()).isEqualTo("integration-draft");
        assertThat(savedEntity.getMarkdownContent()).isEqualTo("# integration");
        assertThat(savedEntity.getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    void createDraftDefaultsNullMarkdownContentToEmptyStringInPostgreSql() {
        Post createdPost = postService.createDraft("Empty Content Draft", null, null, null, null);

        PostEntity savedEntity = springDataPostRepository.findById(createdPost.id()).orElseThrow();

        assertThat(savedEntity.getMarkdownContent()).isEmpty();
        assertThat(savedEntity.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(savedEntity.getPublishedAt()).isNull();
        assertThat(savedEntity.getPreviousStatusBeforeTrash()).isNull();
    }

    @Test
    void publishAndUpdateKeepOriginalSlugWhenRequestedSlugIsNull() {
        Post createdPost = postService.createDraft("Lifecycle Post", "# draft", null, null, null);
        UUID postId = createdPost.id();

        postService.publish(postId);
        postService.updatePost(postId, "Lifecycle Post Updated", "# published", null, null, null, null);

        PostEntity savedEntity = springDataPostRepository.findById(postId).orElseThrow();

        assertThat(savedEntity.getTitle()).isEqualTo("Lifecycle Post Updated");
        assertThat(savedEntity.getSlug()).isEqualTo("lifecycle-post");
        assertThat(savedEntity.getMarkdownContent()).isEqualTo("# published");
        assertThat(savedEntity.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(savedEntity.getPublishedAt()).isNotNull();
    }

    @Test
    void unpublishMoveToTrashAndRestorePersistStatusFieldsToPostgreSql() {
        Post createdPost = postService.createDraft("Trash Me", "# content", null, null, null);
        UUID postId = createdPost.id();

        postService.publish(postId);
        postService.unpublish(postId);
        postService.moveToTrash(postId);

        PostEntity trashedEntity = springDataPostRepository.findById(postId).orElseThrow();

        assertThat(trashedEntity.getStatus()).isEqualTo(PostStatus.TRASHED);
        assertThat(trashedEntity.getPublishedAt()).isNotNull();
        assertThat(trashedEntity.getPreviousStatusBeforeTrash()).isEqualTo(PostStatus.UNPUBLISHED);

        postService.restoreFromTrash(postId);

        PostEntity restoredEntity = springDataPostRepository.findById(postId).orElseThrow();

        assertThat(restoredEntity.getStatus()).isEqualTo(PostStatus.UNPUBLISHED);
        assertThat(restoredEntity.getPublishedAt()).isNotNull();
        assertThat(restoredEntity.getPreviousStatusBeforeTrash()).isNull();
    }
}
