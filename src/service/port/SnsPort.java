package service.port;

import java.util.List;

public interface SnsPort {
    // 로그인/뷰 결과
    record UserView(int userId, String userName) {}

    // 피드용 뷰
    record PostView(
            int postId,
            int authorId, String authorName,
            String filePath, String fileName,
            String uploadTime,
            int likeCount, int dislikeCount,
            String myState) {}

    // --- Auth ---
    /** 사용자명으로 로그인(성공 시 사용자 정보, 실패 시 null) */
    UserView login(String username, String password);

    /** (선택) 이메일로 로그인 – 구현체가 미지원일 수 있음 */
    default UserView loginByEmail(String email, String password) {
        return null; // 구현체에서 필요 시 오버라이드
    }

    /** 표준: 사용자명 + 이메일 + 비밀번호 회원가입 (성공 시 사용자 정보, 중복 등 실패 시 null) */
    default UserView register(String username, String email, String password) {
        throw new UnsupportedOperationException("register(username, email, password) not implemented");
    }

    /** 호환: 2-인자 회원가입(이메일 없는 구현체용). 기본은 더미 이메일로 위임 */
    default UserView register(String username, String password) {
        return register(username, username + "@local", password);
    }

    // --- Feed ---
    List<PostView> listRecent(int requesterId, int limit);
    List<PostView> search(int requesterId, String keyword, int limit);
    /** type = "LIKE" | "DISLIKE" */
    String toggleReaction(int userId, int postId, String type);
    void createPost(int userId, String filePath, String fileName);
}
