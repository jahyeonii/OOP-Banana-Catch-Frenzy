import java.awt.*;
import java.awt.geom.*;
 
public class Banana {
    public float x, y;
    float speed;
    public int type; // 0=normal, 1=golden
    double rot, rotSpeed;
 
    public Banana(float x, float speed) {
        this.x = x;
        this.y = -30;
        this.speed = speed;
        this.type = Math.random() < 0.15 ? 1 : 0;
        this.rot = Math.random() * Math.PI * 2;
        this.rotSpeed = (Math.random() - 0.5) * 0.1;
    }
 
    public void update() {
        y += speed;
        rot += rotSpeed;
    }
 
    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, 30, 30);
    }
 
    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x + 15, y + 15);
        g2.rotate(rot);
 
        if (type == 1) {
            // Golden glow
            g2.setColor(new Color(255, 224, 0, 80));
            g2.fillOval(-18, -18, 36, 36);
        }
 
        // Banana body
        Color fill   = type == 1 ? new Color(255, 224, 0)   : new Color(245, 216, 0);
        Color stroke = type == 1 ? new Color(196, 160, 0)   : new Color(160, 128, 0);
 
        Path2D banana = new Path2D.Float();
        banana.moveTo(-4, -14);
        banana.quadTo(18, -12, 15, 14);
        banana.quadTo(14, 16, 10, 14);
        banana.quadTo(12, 0, -6, -10);
        banana.closePath();
 
        g2.setColor(fill);
        g2.fill(banana);
        g2.setColor(stroke);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(banana);
 
        // Tip
        g2.setColor(stroke);
        g2.fillOval(-7, -17, 6, 6);
 
        g2.dispose();
    }
}
