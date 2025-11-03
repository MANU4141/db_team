package service.port;

import java.util.List;

public interface SnsPort {

    record UserView(int userId, String username) {}
    record CommentView(int commentId, int postId, int userId, String authorName, String text) {}
    record PostView(int postId, int userId, String authorName,
                    String filePath, String fileName, String uploadTime,
                    int likeCount, int dislikeCount, String myState) {}

    // ----- Auth -----
    UserView login(String username, String password);
    UserView register(String username, String email, String password); // ✅ 추가

    // ----- Post -----
    List<PostView> listRecent(int requesterId, int limit);
    List<PostView> search(int requesterId, String keyword, int limit);
    void createPost(int userId, String filePath, String fileName);
    String toggleReaction(int userId, int postId, String type);

    // ----- Comment -----
    List<CommentView> listComments(int postId);
    void addComment(int userId, int postId, String text);
}
