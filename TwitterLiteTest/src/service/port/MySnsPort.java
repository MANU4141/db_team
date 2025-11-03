package service.port;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import service.port.SnsPort.CommentView;
import service.port.SnsPort.PostView;
import service.port.SnsPort.UserView;

public class MySnsPort implements SnsPort {

    private final Connection conn;

    public MySnsPort(Connection conn) {
        this.conn = conn;
    }

    // ----- Auth -----
    @Override
    public UserView login(String username, String password) {
        String sql = "SELECT User_id, Name FROM USER WHERE Name=? AND Password=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return new UserView(rs.getInt("User_id"), rs.getString("Name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public UserView register(String username, String email, String password) {
        if (email == null || email.isEmpty()) {
            email = "user" + System.currentTimeMillis() + "@example.com";
        }
        try {
            String sql = "INSERT INTO USER(Name, Email, Password) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return new UserView(rs.getInt(1), username);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ----- Post -----
    @Override
    public List<PostView> listRecent(int requesterId, int limit) {
        List<PostView> posts = new ArrayList<>();

        String sql = """
            SELECT
                p.Post_id,
                p.User_id,
                u.Name,
                p.File_path,
                p.File_name,
                p.Upload_time,
                IFNULL(SUM(CASE WHEN l.L_or_D='LIKE' THEN 1 END),0) AS likeCount,
                IFNULL(SUM(CASE WHEN l.L_or_D='DISLIKE' THEN 1 END),0) AS dislikeCount,
                IFNULL(
                    (SELECT L_or_D FROM LIKE_OR_DISLIKE 
                    WHERE User_id = ? AND Post_id = p.Post_id),
                    'NONE'
                ) AS myState
            FROM POST p
            JOIN USER u ON p.User_id = u.User_id
            LEFT JOIN LIKE_OR_DISLIKE l ON p.Post_id = l.Post_id
            GROUP BY p.Post_id
            ORDER BY p.Upload_time DESC
            LIMIT ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requesterId);  // for subquery (myState)
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                posts.add(new PostView(
                    rs.getInt("Post_id"),
                    rs.getInt("User_id"),
                    rs.getString("Name"),
                    rs.getString("File_path"),
                    rs.getString("File_name"),
                    rs.getString("Upload_time"),
                    rs.getInt("likeCount"),
                    rs.getInt("dislikeCount"),
                    rs.getString("myState")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }


    @Override
    public List<PostView> search(int requesterId, String keyword, int limit) {
        List<PostView> posts = new ArrayList<>();
        String sql = "SELECT p.Post_id, p.User_id, u.Name, p.File_path, p.File_name, p.Upload_time " +
                "FROM POST p JOIN USER u ON p.User_id = u.User_id " +
                "WHERE p.File_name LIKE ? OR u.Name LIKE ? ORDER BY p.Upload_time DESC LIMIT ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            pstmt.setString(1, kw);
            pstmt.setString(2, kw);
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                posts.add(new PostView(
                        rs.getInt("Post_id"), rs.getInt("User_id"), rs.getString("Name"),
                        rs.getString("File_path"), rs.getString("File_name"), rs.getString("Upload_time"),
                        0, 0, "NONE"
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return posts;
    }

    @Override
    public void createPost(int userId, String filePath, String fileName) {
        String sql = "INSERT INTO POST(User_id, File_path, File_name) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, filePath);
            pstmt.setString(3, fileName);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public String toggleReaction(int userId, int postId, String type) {
        try {
            String sqlCheck = "SELECT L_or_D FROM LIKE_OR_DISLIKE WHERE User_id=? AND Post_id=?";
            PreparedStatement pstmt = conn.prepareStatement(sqlCheck);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, postId);
            ResultSet rs = pstmt.executeQuery();
            String current = rs.next() ? rs.getString("L_or_D") : "NONE";
            String next = current.equals(type) ? "NONE" : type;

            if (current.equals("NONE") && !next.equals("NONE")) {
                String sqlInsert = "INSERT INTO LIKE_OR_DISLIKE(User_id, Post_id, L_or_D) VALUES (?, ?, ?)";
                PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
                psInsert.setInt(1, userId);
                psInsert.setInt(2, postId);
                psInsert.setString(3, next);
                psInsert.executeUpdate();
            } else if (!current.equals("NONE")) {
                if (next.equals("NONE")) {
                    String sqlDel = "DELETE FROM LIKE_OR_DISLIKE WHERE User_id=? AND Post_id=?";
                    PreparedStatement psDel = conn.prepareStatement(sqlDel);
                    psDel.setInt(1, userId);
                    psDel.setInt(2, postId);
                    psDel.executeUpdate();
                } else {
                    String sqlUpd = "UPDATE LIKE_OR_DISLIKE SET L_or_D=? WHERE User_id=? AND Post_id=?";
                    PreparedStatement psUpd = conn.prepareStatement(sqlUpd);
                    psUpd.setString(1, next);
                    psUpd.setInt(2, userId);
                    psUpd.setInt(3, postId);
                    psUpd.executeUpdate();
                }
            }
            return next;
        } catch (SQLException e) { e.printStackTrace(); return "NONE"; }
    }

    // ----- Comment -----
    @Override
    public List<CommentView> listComments(int postId) {
        List<CommentView> comments = new ArrayList<>();
        String sql = "SELECT c.Comment_id, c.Post_id, c.User_id, u.Name, c.Comment " +
                "FROM COMMENT c JOIN USER u ON c.User_id = u.User_id " +
                "WHERE c.Post_id = ? ORDER BY c.Upload_time ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, postId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                comments.add(new CommentView(
                        rs.getInt("Comment_id"), rs.getInt("Post_id"), rs.getInt("User_id"),
                        rs.getString("Name"), rs.getString("Comment")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return comments;
    }

    @Override
    public void addComment(int userId, int postId, String text) {
        String sql = "INSERT INTO COMMENT(Post_id, User_id, Comment) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, text);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
