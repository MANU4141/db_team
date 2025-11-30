package service.port;

import java.sql.*;
import java.util.*;

public class MySnsPort implements SnsPort {

    private final Connection conn;

    public MySnsPort(Connection conn) {
        this.conn = conn;
    }

    /* ========== Auth (로그인/회원가입) ========== */

    @Override
    public UserView login(String username, String password) {
        String sql = "SELECT User_id, Name FROM USER WHERE Name=? AND Password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new UserView(rs.getInt("User_id"), rs.getString("Name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public UserView loginByEmail(String email, String password) {
        String sql = "SELECT User_id, Name FROM USER WHERE Email=? AND Password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new UserView(rs.getInt("User_id"), rs.getString("Name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public UserView register(String username, String email, String password) {
        // 1. 중복 체크
        String checkSql = "SELECT User_id FROM USER WHERE Name=? OR Email=?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            if (ps.executeQuery().next())
                return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        // 2. 가입 처리
        String sql = "INSERT INTO USER(Name, Email, Password) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return new UserView(rs.getInt(1), username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /* ========== Feed (목록/검색) ========== */

    @Override
    public List<PostView> listRecent(int requesterId, int limit) {
        String sql = """
                    SELECT p.Post_id, p.User_id AS authorId, u.Name AS authorName,
                           p.Post_context,
                           p.File_path, p.File_name,
                           DATE_FORMAT(p.Upload_time, '%Y-%m-%d %H:%i') AS uploadTime,
                           (SELECT COUNT(*) FROM REACTION r WHERE r.Post_id=p.Post_id AND r.Type='LIKE') AS likeCount,
                           (SELECT COUNT(*) FROM REACTION r WHERE r.Post_id=p.Post_id AND r.Type='DISLIKE') AS dislikeCount,
                           (SELECT Type FROM REACTION r WHERE r.Post_id=p.Post_id AND r.User_id=? LIMIT 1) AS myState
                    FROM POST p
                    JOIN USER u ON p.User_id = u.User_id
                    ORDER BY p.Post_id DESC
                    LIMIT ?
                """;
        List<PostView> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new PostView(rs.getInt("Post_id"), rs.getInt("authorId"), rs.getString("authorName"),
                        rs.getString("Post_context"), // 본문 내용
                        rs.getString("File_path"), rs.getString("File_name"), rs.getString("uploadTime"),
                        rs.getInt("likeCount"), rs.getInt("dislikeCount"),
                        Optional.ofNullable(rs.getString("myState")).orElse("NONE")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<PostView> search(int requesterId, String keyword, int limit) {
        String sql = """
                    SELECT p.Post_id, p.User_id AS authorId, u.Name AS authorName,
                           p.Post_context,
                           p.File_path, p.File_name,
                           DATE_FORMAT(p.Upload_time, '%Y-%m-%d %H:%i') AS uploadTime,
                           (SELECT COUNT(*) FROM REACTION r WHERE r.Post_id=p.Post_id AND r.Type='LIKE') AS likeCount,
                           (SELECT COUNT(*) FROM REACTION r WHERE r.Post_id=p.Post_id AND r.Type='DISLIKE') AS dislikeCount,
                           (SELECT Type FROM REACTION r WHERE r.Post_id=p.Post_id AND r.User_id=? LIMIT 1) AS myState
                    FROM POST p
                    JOIN USER u ON p.User_id = u.User_id
                    WHERE p.Post_context LIKE ? OR p.File_name LIKE ? OR u.Name LIKE ?
                    ORDER BY p.Post_id DESC
                    LIMIT ?
                """;
        List<PostView> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            String searchPattern = "%" + keyword + "%";
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);
            ps.setInt(5, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new PostView(rs.getInt("Post_id"), rs.getInt("authorId"), rs.getString("authorName"),
                        rs.getString("Post_context"), // 본문 내용
                        rs.getString("File_path"), rs.getString("File_name"), rs.getString("uploadTime"),
                        rs.getInt("likeCount"), rs.getInt("dislikeCount"),
                        Optional.ofNullable(rs.getString("myState")).orElse("NONE")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /* ========== Reaction (좋아요/싫어요) ========== */

    @Override
    public String toggleReaction(int userId, int postId, String type) {
        String sqlCheck = "SELECT Type FROM REACTION WHERE User_id=? AND Post_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String prev = rs.getString("Type");
                if (prev.equals(type)) {
                    // 삭제
                    try (PreparedStatement del = conn
                            .prepareStatement("DELETE FROM REACTION WHERE User_id=? AND Post_id=?")) {
                        del.setInt(1, userId);
                        del.setInt(2, postId);
                        del.executeUpdate();
                        return "NONE";
                    }
                } else {
                    // 수정
                    try (PreparedStatement up = conn
                            .prepareStatement("UPDATE REACTION SET Type=? WHERE User_id=? AND Post_id=?")) {
                        up.setString(1, type);
                        up.setInt(2, userId);
                        up.setInt(3, postId);
                        up.executeUpdate();
                        return type;
                    }
                }
            } else {
                // 추가
                try (PreparedStatement ins = conn
                        .prepareStatement("INSERT INTO REACTION(User_id, Post_id, Type) VALUES (?,?,?)")) {
                    ins.setInt(1, userId);
                    ins.setInt(2, postId);
                    ins.setString(3, type);
                    ins.executeUpdate();
                    return type;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "NONE";
    }

    /* ========== Post Creation (글쓰기) ========== */

    @Override
    public void createPost(int userId, String filePath, String fileName, String text) {
        String sql = "INSERT INTO POST(User_id, File_path, File_name, Post_context, Upload_time) VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, filePath);
            ps.setString(3, fileName);
            ps.setString(4, text);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createPost(int userId, String filePath, String fileName) {
        createPost(userId, filePath, fileName, "");
    }

    /* ========== Comments (댓글 기능 추가) ========== */

    @Override
    public List<CommentView> listComments(int postId) {
        String sql = """
                    SELECT c.Comment_id, u.Name, c.Comment,
                           DATE_FORMAT(c.Upload_time, '%Y-%m-%d %H:%i') AS time
                    FROM COMMENT c
                    JOIN USER u ON c.User_id = u.User_id
                    WHERE c.Post_id = ?
                    ORDER BY c.Upload_time ASC
                """;
        List<CommentView> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CommentView(rs.getInt("Comment_id"), rs.getString("Name"), rs.getString("Comment"),
                        rs.getString("time")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void addComment(int userId, int postId, String content) {
        String sql = "INSERT INTO COMMENT(User_id, Post_id, Comment, Upload_time) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deletePost(int userId, int postId) {
        // 내 글인지 확인(User_id=?)하고 삭제
        String sql = "DELETE FROM POST WHERE Post_id=? AND User_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updatePost(int userId, int postId, String newContent) {
        // 내 글인지 확인하고 내용과 수정시간 업데이트
        String sql = "UPDATE POST SET Post_context=?, Update_time=NOW() WHERE Post_id=? AND User_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newContent);
            ps.setInt(2, postId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}