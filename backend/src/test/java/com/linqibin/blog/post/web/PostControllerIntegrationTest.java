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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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
    void listPostsEndpointFiltersByCategoryAndTag() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String categoryId = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Java-%s"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        String tagId = mockMvc.perform(post("/api/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Spring-%s"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        UUID matchedId = createDraft("Matched Post", "# body");
        createDraft("Unmatched Post", "# body");
        mockMvc.perform(put("/api/admin/posts/" + matchedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Matched Post",
                          "markdownContent": "# body",
                          "categoryId": "%s",
                          "tagIds": ["%s"]
                        }
                        """.formatted(categoryId, tagId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/posts")
                        .param("categoryId", categoryId)
                        .param("tagId", tagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(matchedId.toString()));
    }

    @Test
    void batchUnpublishEndpointUnpublishesPublishedPosts() throws Exception {
        UUID publishedId = createDraft("Live Post", "# content");
        UUID draftId = createDraft("Draft Post", "# content");
        mockMvc.perform(post("/api/admin/posts/" + publishedId + "/publish"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/posts/batch-unpublish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids": ["%s", "%s"]}
                                """.formatted(publishedId, draftId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.succeeded.length()").value(1))
                .andExpect(jsonPath("$.data.succeeded[0].id").value(publishedId.toString()))
                .andExpect(jsonPath("$.data.succeeded[0].status").value("UNPUBLISHED"))
                .andExpect(jsonPath("$.data.failed.length()").value(1))
                .andExpect(jsonPath("$.data.failed[0].id").value(draftId.toString()));
    }

    @Test
    void batchTrashEndpointMovesPublishedAndDraftPostsToTrash() throws Exception {
        UUID publishedId = createDraft("Live Post", "# content");
        UUID draftId = createDraft("Draft Post", "# content");
        mockMvc.perform(post("/api/admin/posts/" + publishedId + "/publish"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/posts/batch-trash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids": ["%s", "%s"]}
                                """.formatted(publishedId, draftId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.succeeded.length()").value(2))
                .andExpect(jsonPath("$.data.failed.length()").value(0));

        mockMvc.perform(get("/api/admin/posts/" + publishedId))
                .andExpect(jsonPath("$.data.status").value("TRASHED"));
        mockMvc.perform(get("/api/admin/posts/" + draftId))
                .andExpect(jsonPath("$.data.status").value("TRASHED"));
    }

    @Test
    void dashboardEndpointReturnsCountsAndRecentPosts() throws Exception {
        createDraft("Dashboard Draft", "# draft");
        UUID publishedId = createDraft("Dashboard Live", "# live");
        mockMvc.perform(post("/api/admin/posts/" + publishedId + "/publish"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "dashboard-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.requestId").value("dashboard-request-id"))
                .andExpect(jsonPath("$.data.counts.total").value(2))
                .andExpect(jsonPath("$.data.counts.published").value(1))
                .andExpect(jsonPath("$.data.counts.draft").value(1))
                .andExpect(jsonPath("$.data.counts.unpublished").value(0))
                .andExpect(jsonPath("$.data.recentlyEdited.length()").value(2))
                .andExpect(jsonPath("$.data.recentlyPublished.length()").value(1))
                .andExpect(jsonPath("$.data.recentlyPublished[0].id").value(publishedId.toString()))
                .andExpect(jsonPath("$.data.recentlyEdited[0].markdownContent").doesNotExist());
    }

    @Test
    void getPostBySlugEndpointReturnsSavedPost() throws Exception {
        // 创建草稿后需要先发布，公开接口只返回已发布文章。
        String postId = mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Read Me",
                                  "markdownContent": "# content"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/public/posts/read-me")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "get-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "get-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("get-post-request-id"))
                .andExpect(jsonPath("$.data.title").value("Read Me"))
                .andExpect(jsonPath("$.data.slug").value("read-me"))
                // 详情接口应返回渲染后的 HTML。
                .andExpect(jsonPath("$.data.html").isNotEmpty())
                // 详情接口应返回摘要。
                .andExpect(jsonPath("$.data.summary").isNotEmpty())
                // 详情接口应返回阅读时长。
                .andExpect(jsonPath("$.data.readingTimeMinutes").isNumber())
                // 详情接口应返回 SEO 字段。
                .andExpect(jsonPath("$.data.seoTitle").value("Read Me"))
                .andExpect(jsonPath("$.data.canonicalUrl").value("/posts/read-me"));
    }

    @Test
    void getPostBySlugIncludesPreviousAndNextNeighbors() throws Exception {
        UUID oldestId = createDraft("Oldest Neighbor", "# a");
        mockMvc.perform(post("/api/admin/posts/" + oldestId + "/publish")).andExpect(status().isOk());
        UUID middleId = createDraft("Middle Neighbor", "# b");
        mockMvc.perform(post("/api/admin/posts/" + middleId + "/publish")).andExpect(status().isOk());
        UUID newestId = createDraft("Newest Neighbor", "# c");
        mockMvc.perform(post("/api/admin/posts/" + newestId + "/publish")).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/posts/middle-neighbor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousPost.slug").value("oldest-neighbor"))
                .andExpect(jsonPath("$.data.previousPost.title").value("Oldest Neighbor"))
                .andExpect(jsonPath("$.data.nextPost.slug").value("newest-neighbor"))
                .andExpect(jsonPath("$.data.nextPost.title").value("Newest Neighbor"));

        mockMvc.perform(get("/api/public/posts/newest-neighbor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousPost.slug").value("middle-neighbor"))
                .andExpect(jsonPath("$.data.nextPost").value(nullValue()));

        mockMvc.perform(get("/api/public/posts/oldest-neighbor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousPost").value(nullValue()))
                .andExpect(jsonPath("$.data.nextPost.slug").value("middle-neighbor"));
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
    void previewEndpointRendersDraftWithoutPublishing() throws Exception {
        UUID postId = createDraft("Preview Draft", "# Hello preview");

        mockMvc.perform(get("/api/admin/posts/" + postId + "/preview")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "preview-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "preview-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.requestId").value("preview-post-request-id"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value("Preview Draft"))
                .andExpect(jsonPath("$.data.html").isNotEmpty())
                .andExpect(jsonPath("$.data.viewCount").value(0));

        mockMvc.perform(get("/api/admin/posts/" + postId))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
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
    void permanentlyDeleteEndpointRemovesTrashedPost() throws Exception {
        UUID postId = createDraft("Delete Me", "# content");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/trash"));

        mockMvc.perform(delete("/api/admin/posts/" + postId)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "delete-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "delete-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/admin/posts/" + postId))
                .andExpect(status().isNotFound());
    }

    @Test
    void permanentlyDeleteEndpointReturnsConflictWhenPostIsNotInTrash() throws Exception {
        UUID postId = createDraft("Keep Draft", "# content");

        mockMvc.perform(delete("/api/admin/posts/" + postId)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "delete-invalid-request-id"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "delete-invalid-request-id"))
                .andExpect(jsonPath("$.code").value("INVALID_POST_STATE_TRANSITION"))
                .andExpect(jsonPath("$.message").value("文章状态 DRAFT 不允许执行操作: permanentlyDelete"))
                .andExpect(jsonPath("$.requestId").value("delete-invalid-request-id"));
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

    @Test
    void updateWithCorrectExpectedVersionSucceedsAndIncrementsVersion() throws Exception {
        UUID postId = createDraft("Version Post", "# content");
        // 新建草稿的 version 为 0
        assertEquals(0L, postRepository.findById(postId).orElseThrow().version());

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Version Post Updated",
                                  "markdownContent": "# updated",
                                  "expectedVersion": 0
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "version-update-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "version-update-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void updateWithStaleExpectedVersionReturnsConflict() throws Exception {
        UUID postId = createDraft("Concurrent Post", "# content");
        // 先正常更新一次，version 从 0 递增到 1
        mockMvc.perform(put("/api/admin/posts/" + postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Concurrent Post Updated",
                          "markdownContent": "# updated",
                          "expectedVersion": 0
                        }
                        """));

        // 用过期的 expectedVersion=0 再次更新，应返回 409
        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Stale Update",
                                  "markdownContent": "# stale",
                                  "expectedVersion": 0
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "stale-version-request-id"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "stale-version-request-id"))
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("文章已被其他人修改")))
                .andExpect(jsonPath("$.requestId").value("stale-version-request-id"))
                // 409 响应中应包含服务端当前文章数据，让前端对比后决定处理方式。
                .andExpect(jsonPath("$.data.expectedVersion").value(0))
                .andExpect(jsonPath("$.data.actualVersion").value(1))
                .andExpect(jsonPath("$.data.currentPost.title").value("Concurrent Post Updated"))
                .andExpect(jsonPath("$.data.currentPost.version").value(1));
    }

    @Test
    void updateWithoutExpectedVersionSkipsVersionCheck() throws Exception {
        UUID postId = createDraft("No Version Check", "# content");
        // 不传 expectedVersion 时跳过版本检查，即使版本不匹配也能更新
        mockMvc.perform(put("/api/admin/posts/" + postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "No Version Check Updated",
                          "markdownContent": "# updated"
                        }
                        """));

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Second Update",
                                  "markdownContent": "# second"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "no-version-check-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Second Update"));
    }

    @Test
    void getPostByIdEndpointReturnsSinglePost() throws Exception {
        UUID postId = createDraft("Single Post", "# content");

        mockMvc.perform(get("/api/admin/posts/" + postId)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "get-post-by-id-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "get-post-by-id-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("get-post-by-id-request-id"))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.title").value("Single Post"))
                .andExpect(jsonPath("$.data.markdownContent").value("# content"))
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void getSaveStatusEndpointReturnsLightweightStatus() throws Exception {
        UUID postId = createDraft("Status Post", "# content");

        mockMvc.perform(get("/api/admin/posts/" + postId + "/save-status")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "save-status-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "save-status-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("save-status-request-id"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    void updateWithUnchangedContentDoesNotIncrementVersion() throws Exception {
        UUID postId = createDraft("Idempotent Post", "# content");
        // version 为 0

        // 用相同标题、相同正文、不传 slug 再次保存。
        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Idempotent Post",
                                  "markdownContent": "# content"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "idempotent-update-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void listPublishedPostsReturnsOnlyPublishedPosts() throws Exception {
        UUID firstId = createDraft("First Published", "# content one");
        createDraft("Draft Post", "# draft content");
        UUID secondId = createDraft("Second Published", "# content two");
        mockMvc.perform(post("/api/admin/posts/" + firstId + "/publish"));
        mockMvc.perform(post("/api/admin/posts/" + secondId + "/publish"));

        mockMvc.perform(get("/api/public/posts")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "list-public-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                // 列表项不应包含 markdownContent。
                .andExpect(jsonPath("$.data.items[0].markdownContent").doesNotExist())
                // 列表项应包含摘要和阅读时长。
                .andExpect(jsonPath("$.data.items[0].summary").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].readingTimeMinutes").isNumber())
                // 分页元信息。
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void listPublishedPostsSupportsCustomPagination() throws Exception {
        for (int i = 1; i <= 3; i++) {
            UUID postId = createDraft("Post " + i, "# content " + i);
            mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"));
        }

        mockMvc.perform(get("/api/public/posts")
                        .param("page", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/public/posts")
                        .param("page", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(2));
    }

    @Test
    void getPostBySlugIncrementsViewCount() throws Exception {
        UUID postId = createDraft("View Count Post", "# content");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"));

        // 第一次访问详情。
        mockMvc.perform(get("/api/public/posts/view-count-post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));

        // 第二次访问，阅读数应递增。
        mockMvc.perform(get("/api/public/posts/view-count-post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }

    @Test
    void getArchivesReturnsPublishedPostsGroupedByMonth() throws Exception {
        UUID postId = createDraft("Archive Post", "# content");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"));
        createDraft("Draft Not In Archive", "# draft");

        mockMvc.perform(get("/api/public/posts/archives")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "archive-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.requestId").value("archive-request-id"))
                // 至少有一个分组。
                .andExpect(jsonPath("$.data[0].year").isNumber())
                .andExpect(jsonPath("$.data[0].month").isNumber())
                .andExpect(jsonPath("$.data[0].items[0].title").value("Archive Post"))
                .andExpect(jsonPath("$.data[0].items[0].slug").value("archive-post"));
    }

    @Test
    void archivesDoesNotReturnDrafts() throws Exception {
        createDraft("Draft Only", "# draft");

        mockMvc.perform(get("/api/public/posts/archives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void publicApisReturnCustomExcerptAndCoverUrl() throws Exception {
        UUID postId = createDraftWithSlug("Cover Story", "# markdown body that is longer than excerpt", "cover-story");

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cover Story",
                                  "markdownContent": "# markdown body that is longer than excerpt",
                                  "excerpt": "Handwritten summary",
                                  "coverUrl": "/uploads/cover.jpg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.excerpt").value("Handwritten summary"))
                .andExpect(jsonPath("$.data.coverUrl").value("/uploads/cover.jpg"));

        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].summary").value("Handwritten summary"))
                .andExpect(jsonPath("$.data.items[0].coverUrl").value("/uploads/cover.jpg"));

        mockMvc.perform(get("/api/public/posts/cover-story"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("Handwritten summary"))
                .andExpect(jsonPath("$.data.coverUrl").value("/uploads/cover.jpg"))
                .andExpect(jsonPath("$.data.seoDescription").value("Handwritten summary"));
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
