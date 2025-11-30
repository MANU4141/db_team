package ui;

import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;
import ui.ImageUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PostListPanel extends JPanel {

    private final SnsPort port;
    private final int myUserId;

    private final JTable table;
    private final PostTableModel model;
    private final TableRowSorter<PostTableModel> sorter;

    public PostListPanel(SnsPort port, int myUserId) {
        this.port = port;
        this.myUserId = myUserId;

        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.WHITE);

        // 상단 액션 바
        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        action.setOpaque(false);
        var previewBtn = new RoundedButton("열기", true);
        var commentBtn = new RoundedButton("댓글…", true);
        var dislikeBtn = new RoundedButton("싫어요", true);
        var likeBtn = new RoundedButton("좋아요");
        action.add(previewBtn);
        action.add(commentBtn);
        action.add(dislikeBtn);
        action.add(likeBtn);
        action.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(action, BorderLayout.NORTH);

        model = new PostTableModel();
        table = new JTable(model);
        styleTable(table);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        // ID 내림차순(최신글 위로)
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.DESCENDING)));

        JScrollPane sp = new JScrollPane(table);
        add(sp, BorderLayout.CENTER);

        // 우클릭 메뉴
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("상세 보기");
        JMenuItem commentItem = new JMenuItem("댓글…");
        menu.add(openItem);
        menu.add(commentItem);
        table.setComponentPopupMenu(menu);
        sp.getViewport().setComponentPopupMenu(menu);

        MouseAdapter rowSelector = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0)
                    table.setRowSelectionInterval(row, row);
                table.requestFocusInWindow();
            }
        };
        table.addMouseListener(rowSelector);
        sp.getViewport().addMouseListener(rowSelector);

        // 클릭 이벤트: 작성자(1) 또는 내용(2) 클릭 시 상세 열기
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                int c = table.columnAtPoint(e.getPoint());
                if (r >= 0 && (c == 1 || c == 2)) { // 작성자 or 내용 클릭
                    openDetail();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    openDetail();
            }
        });

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
        table.getActionMap().put("open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDetail();
            }
        });

        previewBtn.addActionListener(e -> openDetail());
        commentBtn.addActionListener(e -> openComments());
        likeBtn.addActionListener(e -> toggle("LIKE"));
        dislikeBtn.addActionListener(e -> toggle("DISLIKE"));
        openItem.addActionListener(e -> openDetail());
        commentItem.addActionListener(e -> openComments());

        reload();
    }

    public void reload() {
        model.setRows(port.listRecent(myUserId, 100));
    }

    public void search(String keyword) {
        model.setRows(port.search(myUserId, keyword, 100));
    }

    private int ensureSelected() {
        int vr = table.getSelectedRow();
        if (vr == -1 && table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            vr = 0;
        }
        if (vr == -1) {
            JOptionPane.showMessageDialog(this, "표시할 게시물이 없습니다.");
        }
        return vr;
    }

    private void toggle(String type) {
        int vr = ensureSelected();
        if (vr < 0)
            return;
        int mr = table.convertRowIndexToModel(vr);
        var p = model.getAt(mr);
        var state = port.toggleReaction(myUserId, p.postId(), type);
        reload();
        if (vr < table.getRowCount())
            table.setRowSelectionInterval(vr, vr);
        JOptionPane.showMessageDialog(this, "현재 내 상태: " + state);
    }

    private void openDetail() {
        int vr = ensureSelected();
        if (vr < 0)
            return;
        int mr = table.convertRowIndexToModel(vr);
        var p = model.getAt(mr);

        // [수정] port, myUserId, 그리고 콜백(reload 메서드)을 전달합니다.
        // 상세 창에서 글을 수정하거나 삭제하면 -> 목록을 다시 불러옵니다(this::reload).
        new PostDetailDialog(SwingUtilities.getWindowAncestor(this), port, myUserId, p, this::reload // <- 수정/삭제 후 실행할
                                                                                                     // 동작 (새로고침)
        ).setVisible(true);
    }

    private void openComments() {
        int vr = ensureSelected();
        if (vr < 0)
            return;
        int mr = table.convertRowIndexToModel(vr);
        var p = model.getAt(mr);

        new CommentDialog(SwingUtilities.getWindowAncestor(this), port, myUserId, p.postId(), p.authorName(),
                p.fileName()).setVisible(true);
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(32);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        t.getTableHeader().setFont(Theme.fontBold(13));
        ((DefaultTableCellRenderer) t.getTableHeader().getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);

        var center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        // [수정] 파일명이 빠져서 총 7개 컬럼 (0~6)
        // 0:ID, 1:작성자, 2:내용, 3:업로드, 4:좋아요, 5:싫어요, 6:내상태
        t.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        t.getColumnModel().getColumn(4).setCellRenderer(center); // 좋아요
        t.getColumnModel().getColumn(5).setCellRenderer(center); // 싫어요
        t.getColumnModel().getColumn(6).setCellRenderer(center); // 내상태

        // [수정] 너비 재조정 (내용을 더 넓게)
        t.getColumnModel().getColumn(0).setPreferredWidth(40); // ID
        t.getColumnModel().getColumn(1).setPreferredWidth(80); // 작성자
        t.getColumnModel().getColumn(2).setPreferredWidth(400); // 내용 (아주 넓게)
        t.getColumnModel().getColumn(3).setPreferredWidth(120); // 시간
        t.getColumnModel().getColumn(4).setPreferredWidth(50); // 좋아요
        t.getColumnModel().getColumn(5).setPreferredWidth(50); // 싫어요
        t.getColumnModel().getColumn(6).setPreferredWidth(60); // 상태
    }

    /* ===== TableModel ===== */
    static class PostTableModel extends AbstractTableModel {
        // [수정] 화면에 보이는 컬럼 목록에서 "파일명"을 제거했습니다.
        private final String[] cols = { "ID", "작성자", "내용", "업로드", "좋아요", "싫어요", "내상태" };
        private List<SnsPort.PostView> rows = List.of();

        public void setRows(List<SnsPort.PostView> list) {
            rows = list;
            fireTableDataChanged();
        }

        public SnsPort.PostView getAt(int r) {
            return rows.get(r);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Object getValueAt(int r, int c) {
            var p = rows.get(r);
            // [수정] 파일명 케이스를 제거하고 순서를 당겼습니다.
            return switch (c) {
            case 0 -> p.postId();
            case 1 -> p.authorName();
            case 2 -> p.content(); // 내용
            // case 파일명 -> 제외됨 (화면엔 안 보이지만 p 객체엔 데이터가 있음)
            case 3 -> p.uploadTime();
            case 4 -> p.likeCount();
            case 5 -> p.dislikeCount();
            case 6 -> p.myState();
            default -> "";
            };
        }
    }
}