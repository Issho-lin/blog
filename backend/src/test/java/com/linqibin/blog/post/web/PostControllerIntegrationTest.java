package com.linqibin.blog.post.web;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.common.request.RequestIdUtils;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryPostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.clear();
    }

    @Test
    void createDraftEndpointReturnsCreatedPost() throws Exception {
        mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hello Spring Boot",
                                  "markdownContent": "# Hello"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "create-post-request-id"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "create-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("create-post-request-id"))
                .andExpect(jsonPath("$.data.title").value("Hello Spring Boot"))
                .andExpect(jsonPath("$.data.slug").value("hello-spring-boot"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void listPostsEndpointReturnsAllPostsWhenKeywordIsMissing() throws Exception {
        createDraft("Spring Guide", "# spring");
        createDraft("Redis Notes", "# redis");

        mockMvc.perform(get("/api/admin/posts")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "list-posts-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "list-posts-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("list-posts-request-id"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void listPostsEndpointFiltersByKeywordIgnoringCase() throws Exception {
        createDraft("Spring Guide", "# spring");
        createDraft("Redis Notes", "# redis");

        mockMvc.perform(get("/api/admin/posts")
                        .param("keyword", "spring")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "search-posts-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "search-posts-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("search-posts-request-id"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Spring Guide"));
    }

    @Test
    void listPostsEndpointReturnsPostsInUpdatedAtDescendingOrder() throws Exception {
        UUID olderPostId = createDraft("Older Post", "# old");
        UUID newerPostId = createDraft("Newer Post", "# new");
        mockMvc.perform(put("/api/admin/posts/" + olderPostId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Older Post Updated",
                          "markdownContent": "# old"
                        }
                        """));

        mockMvc.perform(get("/api/admin/posts")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "ordered-posts-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(olderPostId.toString()))
                .andExpect(jsonPath("$.data[0].title").value("Older Post Updated"))
                .andExpect(jsonPath("$.data[1].id").value(newerPostId.toString()))
                .andExpect(jsonPath("$.data[1].title").value("Newer Post"));
    }

    @Test
    void getPostBySlugEndpointReturnsSavedPost() throws Exception {
        mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Read Me",
                                  "markdownContent": "# content"
                                }
                                """));

        mockMvc.perform(get("/api/public/posts/read-me")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "get-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "get-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("get-post-request-id"))
                .andExpect(jsonPath("$.data.title").value("Read Me"))
                .andExpect(jsonPath("$.data.slug").value("read-me"));
    }

    @Test
    void createDraftWithDuplicateRequestedSlugReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/admin/posts/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "First Post",
                          "markdownContent": "# first",
                          "slug": "custom-post"
                        }
                        """));

        mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Second Post",
                                  "markdownContent": "# second",
                                  "slug": "custom-post"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "duplicate-slug-request-id"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "duplicate-slug-request-id"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_SLUG"))
                .andExpect(jsonPath("$.message").value("slug 已存在: custom-post"))
                .andExpect(jsonPath("$.requestId").value("duplicate-slug-request-id"));
    }

    @Test
    void getMissingPostBySlugReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/public/posts/not-exists")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "missing-post-request-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "missing-post-request-id"))
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("文章不存在: not-exists"))
                .andExpect(jsonPath("$.requestId").value("missing-post-request-id"));
    }

    @Test
    void publishEndpointPublishesDraft() throws Exception {
        UUID postId = createDraft("Publish Me", "# content");

        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "publish-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "publish-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("publish-post-request-id"))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());
    }

    @Test
    void updateEndpointUpdatesDraftAndKeepsOriginalSlugWhenNotProvided() throws Exception {
        UUID postId = createDraft("Original Title", "# old");

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "markdownContent": "# new"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "update-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "update-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("update-post-request-id"))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.markdownContent").value("# new"))
                .andExpect(jsonPath("$.data.slug").value("original-title"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void updateEndpointWithBlankSlugRegeneratesSlugFromLatestTitle() throws Exception {
        UUID postId = createDraft("Original Title", "# old");

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "New Url Title",
                                  "markdownContent": "# old",
                                  "slug": ""
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "regenerate-slug-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("new-url-title"));
    }

    @Test
    void updateEndpointWithDuplicateRequestedSlugReturnsConflict() throws Exception {
        createDraftWithSlug("First Post", "# first", "custom-post");
        UUID secondPostId = createDraft("Second Post", "# second");

        mockMvc.perform(put("/api/admin/posts/" + secondPostId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Second Post",
                                  "markdownContent": "# second",
                                  "slug": "custom-post"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "duplicate-update-slug-request-id"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "duplicate-update-slug-request-id"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_SLUG"))
                .andExpect(jsonPath("$.message").value("slug 已存在: custom-post"))
                .andExpect(jsonPath("$.requestId").value("duplicate-update-slug-request-id"));
    }

    @Test
    void updatePublishedPostWithBlankContentReturnsBadRequest() throws Exception {
        UUID postId = createDraft("Published Post", "# content");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"));

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Published Post",
                                  "markdownContent": ""
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "update-published-invalid-request-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "update-published-invalid-request-id"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("正文不能为空"))
                .andExpect(jsonPath("$.requestId").value("update-published-invalid-request-id"));
    }

    @Test
    void unpublishEndpointChangesPublishedPostToUnpublished() throws Exception {
        UUID postId = createDraft("Unpublish Me", "# content");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"));

        mockMvc.perform(post("/api/admin/posts/" + postId + "/unpublish")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "unpublish-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "unpublish-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("unpublish-post-request-id"))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"));
    }

    @Test
    void trashAndRestoreEndpointsKeepPreviousStatus() throws Exception {
        UUID postId = createDraft("Trash Me", "# content");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"));
        mockMvc.perform(post("/api/admin/posts/" + postId + "/unpublish"));

        mockMvc.perform(post("/api/admin/posts/" + postId + "/trash")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "trash-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "trash-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("trash-post-request-id"))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.status").value("TRASHED"));

        mockMvc.perform(post("/api/admin/posts/" + postId + "/restore")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "restore-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "restore-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("restore-post-request-id"))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"));
    }

    @Test
    void publishEndpointReturnsBadRequestWhenContentIsBlank() throws Exception {
        UUID postId = createDraft("Blank Content", "");

        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "publish-invalid-request-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "publish-invalid-request-id"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("正文不能为空"))
                .andExpect(jsonPath("$.requestId").value("publish-invalid-request-id"));
    }

    @Test
    void restoreEndpointReturnsConflictWhenPostIsNotInTrash() throws Exception {
        UUID postId = createDraft("Restore Me", "# content");

        mockMvc.perform(post("/api/admin/posts/" + postId + "/restore")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "restore-invalid-request-id"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "restore-invalid-request-id"))
                .andExpect(jsonPath("$.code").value("INVALID_POST_STATE_TRANSITION"))
                .andExpect(jsonPath("$.message").value("文章状态 DRAFT 不允许执行操作: restoreFromTrash"))
                .andExpect(jsonPath("$.requestId").value("restore-invalid-request-id"));
    }

    private UUID createDraft(String title, String markdownContent) throws Exception {
        mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "markdownContent": "%s"
                                }
                                """.formatted(title, markdownContent)))
                .andExpect(status().isCreated());

        return postRepository.findAll().stream()
                .filter(post -> post.title().equals(title))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private UUID createDraftWithSlug(String title, String markdownContent, String slug) throws Exception {
        mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "markdownContent": "%s",
                                  "slug": "%s"
                                }
                                """.formatted(title, markdownContent, slug)))
                .andExpect(status().isCreated());

        return postRepository.findAll().stream()
                .filter(post -> post.title().equals(title))
                .findFirst()
                .orElseThrow()
                .id();
    }
}
