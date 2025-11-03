package service.fake;

import service.port.SnsPort;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 메모리 기반 더미 구현 (SnsPort 시그니처에 정확히 맞춤) */
public class FakeSnsAdapter implements SnsPort {

    /* ------------ 내부 모델 ------------ */
    private static final class User {
        final int id; final String name; final String password;
        User(int id, String name, String password) {
            this.id = id; this.name = name; this.password = password;
        }
    }

    private static final class Post {
        final int id; final int authorId;
        final String filePath; final String fileName;
        final String uploadTime;
        /** userId -> "LIKE" | "DISLIKE" | "NONE" */
        final Map<Integer, String> reactions = new HashMap<>();
        Post(int id, int authorId, String filePath, String fileName, String uploadTime) {
            this.id = id; this.authorId = authorId;
            this.filePath = filePath; this.fileName = fileName;
            this.uploadTime = uploadTime;
        }
    }

    /* ------------ 저장소 ------------ */
    private final List<User> users = new ArrayList<>();
    private final List<Post> posts = new ArrayList<>();
    private int userSeq = 1;
    private int postSeq = 1;

    public FakeSnsAdapter() {
        seedUsers();
        seedPosts();
    }

    private void seedUsers() {
        // 기본 계정
        users.add(new User(userSeq++, "Alice", "1111"));
        users.add(new User(userSeq++, "Bob",   "1111"));
        users.add(new User(userSeq++, "Charlie", "1111"));
    }

    private void seedPosts() {
        // 프로젝트 루트에 sample/ 디렉토리 만들고 예시 이미지 파일명을 맞춰주면 바로 미리보기 가능
        // 없을 경우에도 PostListPanel이 플레이스홀더 화면으로 열림
        posts.add(new Post(postSeq++, idOf("Alice"), "sample", "first.png", now()));
        posts.add(new Post(postSeq++, idOf("Alice"), "sample", "db_ok.png", now()));
        posts.add(new Post(postSeq++, idOf("Bob"),   "sample", "only_text.png", now()));
        posts.add(new Post(postSeq++, idOf("Charlie"), null, null, now())); // 첨부 없는 텍스트 글
        // 초기 반응
        react(idOf("Alice"), 1, "LIKE");
        react(idOf("Bob"),   3, "DISLIKE");
    }

    private int idOf(String name) {
        return users.stream().filter(u -> u.name.equals(name)).findFirst().orElseThrow().id;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /* =====================================
                 SnsPort 구현
       ===================================== */

    @Override
    public UserView login(String username, String password) {
    String id = username == null ? "" : username.trim();
    String pw = password == null ? "" : password.trim();

        return users.stream()
            .filter(u -> u.name.equalsIgnoreCase(id) && u.password.equals(pw))
            .findFirst()
            .map(u -> new UserView(u.id, u.name))
            .orElse(null);
}


    @Override
    public List<PostView> listRecent(int requesterId, int limit) {
        return posts.stream()
                .sorted((a, b) -> Integer.compare(b.id, a.id)) // 최신 글 우선
                .limit(limit)
                .map(p -> toView(p, requesterId))
                .toList();
    }

    @Override
    public List<PostView> search(int requesterId, String keyword, int limit) {
        String kw = keyword == null ? "" : keyword.trim();
        return posts.stream()
                .filter(p -> contains(p.fileName, kw) || contains(authorName(p.authorId), kw))
                .sorted((a, b) -> Integer.compare(b.id, a.id))
                .limit(limit)
                .map(p -> toView(p, requesterId))
                .toList();
    }

    @Override
    public String toggleReaction(int userId, int postId, String type) {
        Post p = findPost(postId);
        String prev = p.reactions.getOrDefault(userId, "NONE");
        String next = prev.equals(type) ? "NONE" : type; // 같은 걸 누르면 취소
        react(userId, postId, next);
        return next;
    }

    @Override
    public void createPost(int userId, String filePath, String fileName) {
        posts.add(new Post(postSeq++, userId, filePath, fileName, now()));
    }

    /* ------------ 헬퍼 ------------ */

    private void react(int userId, int postId, String state) {
        Post p = findPost(postId);
        if ("NONE".equals(state)) p.reactions.remove(userId);
        else p.reactions.put(userId, state);
    }

    private Post findPost(int id) {
        return posts.stream().filter(p -> p.id == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("post not found: " + id));
    }

    private String authorName(int authorId) {
        return users.stream().filter(u -> u.id == authorId).findFirst().map(u -> u.name).orElse("Unknown");
    }

    private boolean contains(String s, String kw) {
        if (kw.isEmpty()) return true;
        return s != null && s.toLowerCase().contains(kw.toLowerCase());
    }

    private SnsPort.PostView toView(Post p, int requesterId) {
        int like = (int) p.reactions.values().stream().filter("LIKE"::equals).count();
        int dislike = (int) p.reactions.values().stream().filter("DISLIKE"::equals).count();
        String my = p.reactions.getOrDefault(requesterId, "NONE");
        return new SnsPort.PostView(
                p.id,
                p.authorId, authorName(p.authorId),
                p.filePath, p.fileName,
                p.uploadTime,
                like, dislike,
                my
        );
    }
}
