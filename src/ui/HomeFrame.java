package ui;

import java.awt.*;
import javax.swing.*;
import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;

public class HomeFrame extends JFrame {
    private final SnsPort port;
    private final int myUserId;
    private final String myName;

    private final PostListPanel listPanel;

    public HomeFrame(SnsPort port, int myUserId, String myName){
        super("홈 · " + myName);
        this.port = port; this.myUserId = myUserId; this.myName = myName;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // 헤더
        var header = Theme.header("SNS 피드");
        var userLabel = new JLabel(myName + " 님"); // 이모지 제거
        userLabel.setFont(Theme.fontRegular(14));
        header.add(userLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // 목록
        listPanel = new PostListPanel(port, myUserId);
        add(listPanel, BorderLayout.CENTER);

        // 하단
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        var searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRAY_200),
                BorderFactory.createEmptyBorder(8,8,8,8)
        ));
        var searchBtn = new RoundedButton("검색", true);
        JPanel left = new JPanel(new BorderLayout(6,0));
        left.add(searchField, BorderLayout.CENTER);
        left.add(searchBtn, BorderLayout.EAST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        var refreshBtn = new RoundedButton("새로고침", true);
        var writeBtn   = new RoundedButton("글쓰기");
        right.add(refreshBtn); right.add(writeBtn);

        bottom.add(left, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // 이벤트
        refreshBtn.addActionListener(e -> listPanel.reload());
        searchBtn.addActionListener(e -> listPanel.search(searchField.getText().trim()));

        // 글쓰기: 파일 + 캡션 지원
        writeBtn.addActionListener(e -> {
            PostWriteDialog dlg = new PostWriteDialog(this);
            dlg.setVisible(true);               // 모달
            if (!dlg.isApproved()) return;      // 취소

            String path    = dlg.getFilePath();   // null 가능
            String name    = dlg.getFileName();   // null 가능
            String caption = dlg.getCaption();    // null/빈문자 가능

            Integer newPostId = null;

            // 1) 포트에 createPostWithCaption가 있으면 그것을 사용
            try {
                var m = port.getClass().getMethod(
                        "createPostWithCaption", int.class, String.class, String.class, String.class);
                newPostId = (Integer) m.invoke(port, myUserId, path, name, caption);
                listPanel.reload();
                if (newPostId != null) listPanel.selectPostId(newPostId);
                return;
            } catch (NoSuchMethodException ignore) {
                // 없으면 폴백으로 진행
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "글쓰기 오류: " + ex.getMessage());
                return;
            }

            // 2) 폴백: createPost → (캡션이 있으면) 첫 댓글로 저장
            try {
                port.createPost(myUserId, path, name);
                listPanel.reload();

                var rows = listPanel.getModelRows();
                if (!rows.isEmpty()) {
                    int pid = rows.get(0).postId();   // 최신글이 맨 위라고 가정
                    newPostId = pid;

                    if (caption != null && !caption.isBlank()) {
                        try {
                            var cmt = port.getClass().getMethod("addComment", int.class, int.class, String.class);
                            cmt.invoke(port, pid, myUserId, caption);
                        } catch (NoSuchMethodException ignore) {
                            // 댓글 API가 없으면 생략
                        }
                    }
                    listPanel.reload();
                    listPanel.selectPostId(pid);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "글쓰기 오류: " + ex.getMessage());
            }
        });
    }
}
