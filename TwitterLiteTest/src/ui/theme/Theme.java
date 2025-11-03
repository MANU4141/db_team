package ui.theme;

import java.awt.*;
import javax.swing.*;

public class Theme {

    // 블루 팔레트
    public static final Color BLUE       = new Color( 68,130,245);
    public static final Color BLUE_DARK  = new Color( 48,100,205);
    public static final Color BLUE_LIGHT = new Color(230,240,255);
    public static final Color WHITE      = Color.WHITE;
    public static final Color BLACK      = new Color(33,33,33);
    public static final Color GRAY_100   = new Color(245,245,245);
    public static final Color GRAY_200   = new Color(220,220,220);
    public static final Color GRAY_700   = new Color(68,68,68);

    public static final int RADIUS = 12;

    public static Font fontRegular(int size){ return new Font("맑은 고딕", Font.PLAIN, size); }
    public static Font fontBold(int size){    return new Font("맑은 고딕", Font.BOLD,  size); }

    public static void initLaf(){
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore) {}
        UIManager.put("Panel.background", WHITE);
        UIManager.put("Label.font",  fontRegular(13));
        UIManager.put("Button.font", fontBold(13));
        UIManager.put("Table.font",  fontRegular(13));
        UIManager.put("Table.gridColor", GRAY_200);
        UIManager.put("Table.foreground", BLACK);
        UIManager.put("Table.selectionBackground", new Color(190,215,255));
        UIManager.put("Table.selectionForeground", BLACK);
        UIManager.put("OptionPane.messageFont", fontRegular(14));
        UIManager.put("OptionPane.buttonFont",  fontBold(13));
    }

    // 상단 헤더(제목 + 구분선)
    public static JPanel header(String titleLeft){
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(10,16,10,16));

        JLabel title = new JLabel(titleLeft);
        title.setFont(fontBold(18));
        title.setForeground(BLACK);
        p.add(title, BorderLayout.WEST);

        JSeparator sep = new JSeparator();
        sep.setForeground(GRAY_200);
        p.add(sep, BorderLayout.SOUTH);
        return p;
    }
}
