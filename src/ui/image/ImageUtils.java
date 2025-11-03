package ui.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/** 이미지 로딩/스케일 유틸리티 */
public final class ImageUtils {
    private ImageUtils(){}

    /** 파일 경로로 이미지 읽기 (실패 시 null) */
    public static BufferedImage read(String fullPath){
        try { return ImageIO.read(new File(fullPath)); }
        catch (Exception e){ return null; }
    }

    /** 긴 변 기준으로 비율 유지 스케일 */
    public static Image scaled(BufferedImage src, int maxW, int maxH){
        if (src == null) return null;
        double rw = (double)maxW / src.getWidth();
        double rh = (double)maxH / src.getHeight();
        double r = Math.min(rw, rh);
        int w = Math.max(1, (int)Math.round(src.getWidth() * r));
        int h = Math.max(1, (int)Math.round(src.getHeight() * r));
        return src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    /** 플레이스홀더 Icon (간단 표시에 사용) */
    public static Icon placeholder(int w, int h, String text){
        BufferedImage img = placeholderImage(w, h, "이미지 없음", text);
        return new ImageIcon(img);
    }

    /** 플레이스홀더 BufferedImage (뷰어에 직접 전달하려고 추가) */
    public static BufferedImage placeholderImage(int w, int h, String title, String subtitle){
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 배경
            g.setColor(new Color(245, 248, 255));
            g.fillRect(0, 0, w, h);
            // 테두리
            g.setColor(new Color(200, 210, 240));
            g.drawRoundRect(8, 8, w - 16, h - 16, 20, 20);
            // 텍스트
            g.setColor(new Color(40, 70, 160));
            g.setFont(new Font("맑은 고딕", Font.BOLD, 28));
            drawCentered(g, title == null ? "" : title, w, h/2 - 10);
            g.setColor(new Color(90, 110, 170));
            g.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            drawCentered(g, subtitle == null ? "" : subtitle, w, h/2 + 22);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static void drawCentered(Graphics2D g, String s, int w, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (w - fm.stringWidth(s)) / 2;
        g.drawString(s, x, y);
    }
}
