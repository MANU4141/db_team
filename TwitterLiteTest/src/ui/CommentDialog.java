package ui;

import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** 댓글 보기/쓰기 */
public class CommentDialog extends JDialog {

    private final SnsPort port;
    private final int myUserId;
    private final int postId;

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JTextField input = new JTextField();

    public CommentDialog(Window owner, SnsPort port, int myUserId,
                         int postId, String authorName, String fileName) {
        super(owner, "댓글 · " + authorName + " · " + fileName, ModalityType.APPLICATION_MODAL);
        this.port = port;
        this.myUserId = myUserId;
        this.postId = postId;

        setSize(520, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        list.setBorder(new EmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setBorder(new EmptyBorder(8, 8, 8, 8));
        south.setOpaque(false);

        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRAY_200),
                new EmptyBorder(8, 10, 8, 10)
        ));
        var sendBtn = new RoundedButton("등록");
        south.add(input, BorderLayout.CENTER);
        south.add(sendBtn, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> submit());
        input.addActionListener(e -> submit());

        reload();
    }

    private void reload() {
        model.clear();
        List<SnsPort.CommentView> comments = port.listComments(postId);
        for (var c : comments) {
            model.addElement("• " + c.authorName() + ": " + c.text());
        }
        if (model.isEmpty()) model.addElement("아직 댓글이 없습니다.");
    }


    private void submit() {
        String txt = input.getText().trim();
        if (txt.isEmpty()) return;
        // 포트의 댓글 작성 API 명칭에 맞춰 호출
        port.addComment(myUserId, postId, txt);
        input.setText("");
        reload();
    }
}
