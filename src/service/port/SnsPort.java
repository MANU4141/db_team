package service.port;

import java.util.List;

public interface SnsPort {
    // 기존 UserView, PostView ...
    record UserView(int userId, String userName) {
    }

    record PostView(int postId, int authorId, String authorName, String content, String filePath, String fileName,
            String uploadTime, int likeCount, int dislikeCount, String myState) {
    }

    // ★ [추가] 댓글용 데이터 구조
    record CommentView(int id, String authorName, String text, String time) {
    }

    // --- Auth ---
    UserView login(String username, String password);

    default UserView loginByEmail(String email, String password) {
        return null;
    }

    default UserView register(String username, String email, String password) {
        throw new UnsupportedOperationException();
    }

    default UserView register(String username, String password) {
        return register(username, username + "@local", password);
    }

    // --- Feed ---
    List<PostView> listRecent(int requesterId, int limit);

    List<PostView> search(int requesterId, String keyword, int limit);

    String toggleReaction(int userId, int postId, String type);

    void createPost(int userId, String filePath, String fileName, String text);

    default void createPost(int userId, String filePath, String fileName) {
        createPost(userId, filePath, fileName, "");
    }

    // ★ [추가] 댓글 목록 조회 및 작성 메서드
    List<CommentView> listComments(int postId);

    void addComment(int userId, int postId, String content);

    /** 게시글 삭제 (본인 글만 삭제 가능하도록 userId 받음) */
    void deletePost(int userId, int postId);

    /** 게시글 수정 (내용만 수정) */
    void updatePost(int userId, int postId, String newContent);
}