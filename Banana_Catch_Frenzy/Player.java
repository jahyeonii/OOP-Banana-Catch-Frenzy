import java.awt.*;
import java.awt.image.BufferedImage;
 
public class Player {
    public int x = 375, y = 470;
    public int w = 120, h = 98;
    public int lives = 3;
    public int score = 0;
    public int speed = 20;
    private BufferedImage sprite;
 
    public void move(int dx, int maxW) {
        x = Math.max(0, Math.min(maxW - w, x + dx));
    }
 
    public void addScore(int s) { score += s; }
    public void loseLife()      { lives--; }
 
    public Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }
 
    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
    }
 
    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 
        System.out.println("Drawing player, sprite is null: " + (sprite == null));
        if (sprite != null) {
            g2.drawImage(sprite, x, y, w, h, null);
            g2.dispose();
            return;
        }
 
        // Basket body (trapezoid)
        int[] bx = { x, x + w, x + w - 8, x + 8 };
        int[] by = { y, y, y + h, y + h };
        g2.setColor(new Color(196, 137, 42));
        g2.fillPolygon(bx, by, 4);
 
        // Weave horizontal lines
        g2.setColor(new Color(139, 94, 26));
        g2.setStroke(new BasicStroke(2f));
        for (int i = 1; i < 4; i++) {
            int yy = y + (h / 4) * i;
            g2.drawLine(x + 2, yy, x + w - 2, yy);
        }
        // Weave vertical lines
        for (int i = 1; i < 4; i++) {
            int xx = x + (w / 4) * i;
            g2.drawLine(xx, y, xx - 4, y + h);
        }
 
        // Rim
        g2.setColor(new Color(139, 94, 26));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRect(x, y, w, 8);
        g2.setColor(new Color(220, 160, 60));
        g2.fillRect(x, y, w, 8);
        g2.setColor(new Color(139, 94, 26));
        g2.drawRect(x, y, w, 8);
 
        // Handle arc
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(139, 94, 26));
        g2.drawArc(x + 4, y - 18, w - 8, 28, 0, 180);
 
        g2.dispose();
    }
}
