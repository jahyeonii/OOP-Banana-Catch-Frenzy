import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
 
public class GamePanel extends JPanel {
 
    //  Screen states 
    enum State { MENU, PLAY, SHOP, OPTIONS, GAMEOVER, PAUSE }
    State state = State.MENU;
 
    static final int W = 800, H = 560;
 
    //  Game objects 
    Player player   = new Player();
    ArrayList<Banana> bananas = new ArrayList<>();
    PowerUpManager powerUps  = new PowerUpManager();
 
    int coins, difficulty, highScore;
    int frameCount = 0;
    boolean[] keys = new boolean[65536];
 
    //  Clouds (menu + play) 
    float[] cloudX  = { 80,  320, 560 };
    float[] cloudY  = { 55,  85,  60  };
    float[] cloudW  = { 110, 80,  95  };
    float[] cloudSpd= { 0.3f,0.18f,0.25f };
 
    //  Vine sway 
    double vineAngle = 0;
    double vineDir   = 1;
 
    //  Hover tracking 
    int mx, my;
 
    //  Sign button rects 
    Rectangle rPlay    = new Rectangle(590, 100, 180, 58);
    Rectangle rShop    = new Rectangle(590, 172, 180, 58);
    Rectangle rOptions = new Rectangle(590, 244, 180, 58);
    Rectangle rExit    = new Rectangle(590, 316, 180, 58);
 
    //  Game-over / shop buttons 
    Rectangle rGoPlayAgain = new Rectangle(290, 320, 220, 52);
    Rectangle rGoMenu      = new Rectangle(290, 388, 220, 52);
    Rectangle rBuyShield   = new Rectangle(490, 210, 120, 40);
    Rectangle rBuyMagnet   = new Rectangle(490, 268, 120, 40);
    Rectangle rBuySpeed    = new Rectangle(490, 326, 120, 40);
    Rectangle rShopClose   = new Rectangle(300, 400, 160, 44);
    Rectangle rOptClose    = new Rectangle(300, 400, 160, 44);
    Rectangle rOptEasy     = new Rectangle(190, 240, 120, 38);
    Rectangle rOptMedium   = new Rectangle(330, 240, 120, 38);
    Rectangle rOptHard     = new Rectangle(470, 240, 120, 38);
    Rectangle rPauseContinue = new Rectangle(300, 260, 200, 52);
    Rectangle rPauseMenu     = new Rectangle(300, 334, 200, 52);
 
    //  Options state 
    int diffChoice = 0; // 0=Easy 1=Medium 2=Hard
    String shopMsg = "";
    boolean shopMsgOk = false;
    long shopMsgTime = 0;
    int shieldCharges = 0;
    int magnetCharges = 0;
    int speedCharges = 0;
 
    //  Fonts 
    Font fontBig, fontMed, fontSm;
 
    //  Cached background 
    BufferedImage bgCache;
    BufferedImage menuBgImage;
    BufferedImage playerSprite;
 
    // 
    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
 
        int[] data = SaveManager.load();
        coins      = data[0];
        difficulty = data[1];
        highScore  = data[2];
        diffChoice = Math.max(0, difficulty - 1);
 
        fontBig = new Font("Arial Black", Font.BOLD, 48);
        fontMed = new Font("Arial Black", Font.BOLD, 26);
        fontSm  = new Font("Arial Black", Font.BOLD, 18);

        menuBgImage = loadImage("menu_bg.png");
        playerSprite = loadImage("player.png");
        System.out.println("Player sprite loaded: " + (playerSprite != null));
        if (playerSprite != null) player.setSprite(playerSprite);
 
        // Game loop ~60fps
        new javax.swing.Timer(16, e -> tick()).start();
 
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e)  { keys[e.getKeyCode()] = true;  onKey(e.getKeyCode()); }
            public void keyReleased(KeyEvent e) { keys[e.getKeyCode()] = false; }
        });
 
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { onClick(e.getX(), e.getY()); }
        });
 
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); }
        });
    }
 
    private BufferedImage loadImage(String fileName) {
        String[] paths = {"assets/" + fileName, fileName};
        for (String path : paths) {
            try {
                File file = new File(path);
                if (file.exists()) return ImageIO.read(file);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    //  Tick 
    void tick() {
        frameCount++;
 
        // Animate clouds
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] += cloudSpd[i];
            if (cloudX[i] > W + 120) cloudX[i] = -130;
        }
 
        // Vine sway
        vineAngle += 0.018 * vineDir;
        if (Math.abs(vineAngle) > 0.18) vineDir = -vineDir;
 
        if (state == State.PLAY) updatePlay();
 
        repaint();
    }
 
    void updatePlay() {
        int moveSpeed = player.speed + (powerUps.isActive("speed") ? 4 : 0);
        if (keys[KeyEvent.VK_LEFT])  player.move(-moveSpeed, W);
        if (keys[KeyEvent.VK_RIGHT]) player.move(moveSpeed,  W);
 
        powerUps.update();
 
        // Spawn fewer bananas, but make them speed up gently over time
        double spawnRate = Math.max(0.003, 0.020 - difficulty * 0.003);
        if (Math.random() < spawnRate) {
            float bx = (float)(Math.random() * (W - 30));
            double timeBoost = Math.log1p(frameCount / 1200.0) * 0.5;
            float spd = (float)(1.8 + difficulty * 0.6 + timeBoost + Math.random() * 0.7);
            bananas.add(new Banana(bx, spd));
        }
 
        // Update bananas
        for (Iterator<Banana> it = bananas.iterator(); it.hasNext(); ) {
            Banana b = it.next();
            b.update();
 
            // Magnet
            if (powerUps.isActive("magnet")) {
                float dx = (player.x + player.w / 2f) - (b.x + 15);
                float dy = (player.y + player.h / 2f) - (b.y + 15);
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < 200) { b.x += dx * 0.08f; b.y += dy * 0.08f; }
            }
 
            // Catch
            if (b.getBounds().intersects(player.getBounds())) {
                player.addScore(b.type == 1 ? 50 : 10);
                coins += b.type == 1 ? 10 : 1;
                it.remove();
                continue;
            }
 
            // Missed
            if (b.y > H) {
                if (!powerUps.isActive("shield")) player.loseLife();
                it.remove();
            }
        }
 
        if (player.lives <= 0) {
            if (player.score > highScore) highScore = player.score;
            SaveManager.save(coins, difficulty, highScore);
            state = State.GAMEOVER;
        }
    }
 
    void onKey(int k) {
        if (k == KeyEvent.VK_ESCAPE) {
            if (state == State.PLAY) {
                state = State.PAUSE;
            } else if (state == State.PAUSE) {
                state = State.PLAY;
            }
        }
        if (state == State.PLAY) {
            if (k == KeyEvent.VK_1) tryActivatePowerUp("shield", 600);
            if (k == KeyEvent.VK_2) tryActivatePowerUp("magnet", 600);
            if (k == KeyEvent.VK_3) tryActivatePowerUp("speed", 600);
        }
    }
 
    void onClick(int cx, int cy) {
        switch (state) {
            case MENU:
                if (rPlay.contains(cx,cy))    startGame();
                if (rShop.contains(cx,cy))    { state = State.SHOP; shopMsg = ""; }
                if (rOptions.contains(cx,cy)) state = State.OPTIONS;
                if (rExit.contains(cx,cy))    System.exit(0);
                break;
            case GAMEOVER:
                if (rGoPlayAgain.contains(cx,cy)) startGame();
                if (rGoMenu.contains(cx,cy))      state = State.MENU;
                break;
            case PAUSE:
                if (rPauseContinue.contains(cx,cy)) state = State.PLAY;
                if (rPauseMenu.contains(cx,cy))     state = State.MENU;
                break;
            case SHOP:
                if (rBuyShield.contains(cx,cy)) buyOrActivatePowerUp("shield", 30, 600);
                if (rBuyMagnet.contains(cx,cy)) buyOrActivatePowerUp("magnet", 40, 600);
                if (rBuySpeed.contains(cx,cy))  buyOrActivatePowerUp("speed",  25, 600);
                if (rShopClose.contains(cx,cy)) state = State.MENU;
                break;
            case OPTIONS:
                if (rOptEasy.contains(cx,cy))   diffChoice = 0;
                if (rOptMedium.contains(cx,cy)) diffChoice = 1;
                if (rOptHard.contains(cx,cy))   diffChoice = 2;
                if (rOptClose.contains(cx,cy)) {
                    difficulty = diffChoice + 1;
                    SaveManager.save(coins, difficulty, highScore);
                    state = State.MENU;
                }
                break;
        }
    }
 
    void startGame() {
        bananas.clear();
        player.lives = 3; player.score = 0; player.x = 375;
        powerUps.reset();
        frameCount = 0;
        state = State.PLAY;
    }
 
    void buyOrActivatePowerUp(String type, int cost, int duration) {
        int charges = getCharges(type);
        if (charges == 0) {
            if (coins >= cost) {
                coins -= cost;
                addCharges(type, 1);
                shopMsg = capitalize(type) + " purchased! Activate it with the button or key.";
                shopMsgOk = true;
            } else {
                shopMsg = "Not enough coins!";
                shopMsgOk = false;
            }
        } else if (powerUps.isActive(type)) {
            shopMsg = capitalize(type) + " is already active.";
            shopMsgOk = false;
        } else {
            addCharges(type, -1);
            powerUps.activate(type, duration);
            shopMsg = capitalize(type) + " activated!";
            shopMsgOk = true;
        }
        shopMsgTime = System.currentTimeMillis();
    }
 
    void tryActivatePowerUp(String type, int duration) {
        int charges = getCharges(type);
        if (charges == 0) {
            shopMsg = "Buy " + capitalize(type) + " first in the shop.";
            shopMsgOk = false;
        } else if (powerUps.isActive(type)) {
            shopMsg = capitalize(type) + " is already active.";
            shopMsgOk = false;
        } else {
            addCharges(type, -1);
            powerUps.activate(type, duration);
            shopMsg = capitalize(type) + " activated!";
            shopMsgOk = true;
        }
        shopMsgTime = System.currentTimeMillis();
    }
 
    int getCharges(String type) {
        return switch (type) {
            case "shield" -> shieldCharges;
            case "magnet" -> magnetCharges;
            case "speed"  -> speedCharges;
            default -> 0;
        };
    }
 
    void addCharges(String type, int amount) {
        switch (type) {
            case "shield" -> shieldCharges = Math.max(0, shieldCharges + amount);
            case "magnet" -> magnetCharges = Math.max(0, magnetCharges + amount);
            case "speed"  -> speedCharges  = Math.max(0, speedCharges  + amount);
        }
    }
 
    String capitalize(String text) {
        return text.substring(0,1).toUpperCase() + text.substring(1);
    }
 
    //  Paint 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
 
        switch (state) {
            case MENU    -> drawMenu(g2);
            case PLAY    -> drawPlay(g2);
            case GAMEOVER-> drawGameOver(g2);
            case SHOP    -> drawShop(g2);
            case OPTIONS -> drawOptions(g2);
            case PAUSE   -> drawPause(g2);
        }
    }
 
    //  SHARED BACKGROUND
    void drawBackground(Graphics2D g2) {
        if (menuBgImage != null) {
            g2.drawImage(menuBgImage, 0, 0, W, H, null);
            return;
        }
        drawJungleBg(g2);
    }
 
    void drawJungleBg(Graphics2D g2) {
        // Sky gradient
        GradientPaint sky = new GradientPaint(0,0, new Color(91,184,232), 0, H*0.55f, new Color(135,206,235));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);
 
        // Ground
        GradientPaint ground = new GradientPaint(0, (int)(H*0.55), new Color(106,191,75), 0, H, new Color(74,158,48));
        g2.setPaint(ground);
        g2.fillRect(0, (int)(H*0.55), W, H);
 
        // Grass spikes
        drawGrassSpikes(g2, (int)(H*0.55));
 
        // Clouds
        for (int i = 0; i < cloudX.length; i++) {
            drawCloud(g2, cloudX[i], cloudY[i], cloudW[i]);
        }
 
        // Background trees
        drawTree(g2, 30,  (int)(H*0.55), 60, 120, 0.55f);
        drawTree(g2, 65,  (int)(H*0.55), 80, 160, 0.75f);
        drawTree(g2, 700, (int)(H*0.55), 55, 110, 0.5f);
        drawTree(g2, 730, (int)(H*0.55), 75, 145, 0.65f);
    }
 
    void drawGrassSpikes(Graphics2D g2, int groundY) {
        g2.setColor(new Color(90, 175, 60));
        int spikeW = 22, spikeH = 24;
        for (int sx = 0; sx < W; sx += spikeW) {
            int[] px = { sx, sx + spikeW/2, sx + spikeW };
            int[] py = { groundY, groundY - spikeH, groundY };
            g2.fillPolygon(px, py, 3);
        }
        // Darker row behind
        g2.setColor(new Color(70,155,45));
        for (int sx = 11; sx < W; sx += spikeW) {
            int[] px = { sx, sx + spikeW/2, sx + spikeW };
            int[] py = { groundY, groundY - spikeH + 6, groundY };
            g2.fillPolygon(px, py, 3);
        }
    }
 
    void drawCloud(Graphics2D g2, float cx, float cy, float cw) {
        g2.setColor(new Color(255,255,255,210));
        g2.fillOval((int)(cx),            (int)(cy),            (int)(cw*0.8), (int)(cw*0.45));
        g2.fillOval((int)(cx+cw*0.25),    (int)(cy-cw*0.25),   (int)(cw*0.55),(int)(cw*0.55));
        g2.fillOval((int)(cx+cw*0.5),     (int)(cy-cw*0.1),    (int)(cw*0.6), (int)(cw*0.42));
    }
 
    void drawTree(Graphics2D g2, int tx, int groundY, int tw, int th, float opacity) {
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        // Trunk
        g2.setColor(new Color(101,67,33));
        g2.fillRect(tx + tw/2 - 6, groundY - 40, 12, 40);
        // Canopy (stacked triangles)
        int[] lx = {tx, tx+tw/2, tx+tw};
        int[] ly = {groundY-40, groundY-40-th, groundY-40};
        g2.setColor(new Color(58,140,40));
        g2.fillPolygon(lx, ly, 3);
        int[] lx2 = {tx+8, tx+tw/2, tx+tw-8};
        int[] ly2 = {groundY-60, groundY-60-th*0.6f >groundY-40-th ? (int)(groundY-40-th-20):(int)(groundY-60-th*0.5), groundY-60};
        g2.setColor(new Color(74,170,50));
        g2.fillPolygon(lx2, new int[]{groundY-65, groundY-65-(int)(th*0.55), groundY-65}, 3);
        g2.setComposite(old);
    }
 
    //  MENU SCREEN
    void drawMenu(Graphics2D g2) {
        if (menuBgImage != null) {
            g2.drawImage(menuBgImage, 0, 0, W, H, null);
        } else {
            drawJungleBg(g2);
        }
        drawTitle(g2);
        drawSignPost(g2);
    }
 
    void drawVineWithMonkey(Graphics2D g2) {
        // Left vine
        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
 
        // Animate via vineAngle
        double angle = vineAngle;
 
        // Vine rope from top
        int vineTopX = 150, vineTopY = 0;
        int vineEndX = vineTopX + (int)(60 * Math.sin(angle));
        int vineEndY = 220;
        g2.setColor(new Color(58, 125, 18));
        g2.drawLine(vineTopX, vineTopY, vineEndX, vineEndY);
 
        // Leaf clusters on vine
        drawVineLeaf(g2, vineTopX + (int)(10*Math.sin(angle*0.3)), 60, angle);
        drawVineLeaf(g2, vineTopX + (int)(20*Math.sin(angle*0.5)), 130, angle + 0.5);
 
        // Monkey body center
        int mx2 = vineEndX;
        int my2 = vineEndY + 10;
 
        drawMonkey(g2, mx2, my2, angle);
    }
 
    void drawVineLeaf(Graphics2D g2, int x, int y, double ang) {
        g2.setColor(new Color(80, 160, 30));
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(ang * 0.5);
        g2.fillOval(-18, -8, 36, 16);
        g2.setTransform(old);
    }
 
    void drawMonkey(Graphics2D g2, int cx, int cy, double sway) {
        // Tail
        g2.setColor(new Color(160, 100, 30));
        g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tail = new Path2D.Double();
        tail.moveTo(cx + 30, cy + 40);
        tail.curveTo(cx+80, cy+60, cx+75, cy+100, cx+55, cy+95);
        g2.draw(tail);
 
        // Body
        g2.setColor(new Color(180, 120, 40));
        g2.fillOval(cx-35, cy+10, 70, 80);
 
        // Belly
        g2.setColor(new Color(230, 185, 110));
        g2.fillOval(cx-20, cy+22, 40, 55);
 
        // Left arm holding vine
        g2.setColor(new Color(180,120,40));
        g2.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - 30, cy + 25, cx - 50, cx > 150 ? cy - 40 : cy - 35);
 
        // Right arm holding banana
        g2.drawLine(cx + 30, cy + 25, cx + 55, cy + 55);
 
        // Banana in right hand
        drawHeldBanana(g2, cx + 58, cy + 60);
 
        // Head
        g2.setColor(new Color(180,120,40));
        g2.fillOval(cx-38, cy-60, 76, 70);
 
        // Ears
        g2.fillOval(cx-52, cy-42, 24, 22);
        g2.fillOval(cx+28, cy-42, 24, 22);
        g2.setColor(new Color(230,185,110));
        g2.fillOval(cx-47, cy-38, 15, 15);
        g2.fillOval(cx+32, cy-38, 15, 15);
 
        // Face plate
        g2.setColor(new Color(230,185,110));
        g2.fillOval(cx-24, cy-18, 48, 35);
 
        // Eyes
        g2.setColor(Color.WHITE);
        g2.fillOval(cx-26, cy-52, 18, 18);
        g2.fillOval(cx+8,  cy-52, 18, 18);
        g2.setColor(new Color(40,25,0));
        g2.fillOval(cx-21, cy-49, 10, 10);
        g2.fillOval(cx+13, cy-49, 10, 10);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx-19, cy-48, 4, 4);
        g2.fillOval(cx+15, cy-48, 4, 4);
 
        // Nostrils
        g2.setColor(new Color(140,85,20));
        g2.fillOval(cx-10, cy-12, 8, 6);
        g2.fillOval(cx+2,  cy-12, 8, 6);
 
        // Smile
        g2.setColor(new Color(100,55,10));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx-14, cy-6, 28, 18, 200, 140);
 
        // Right hand dot
        g2.setColor(new Color(160,100,30));
        g2.fillOval(cx-54, cy-45, 16, 16);
 
        // Legs
        g2.setColor(new Color(180,120,40));
        g2.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx-18, cy+85, cx-22, cy+130);
        g2.drawLine(cx+18, cy+85, cx+22, cy+130);
        // Feet
        g2.setColor(new Color(140,85,20));
        g2.fillOval(cx-32, cy+126, 22, 12);
        g2.fillOval(cx+10, cy+126, 22, 12);
    }
 
    void drawHeldBanana(Graphics2D g2, int bx, int by) {
        Graphics2D g3 = (Graphics2D) g2.create();
        g3.translate(bx, by);
        g3.rotate(-0.4);
        g3.setColor(new Color(255,224,0));
        Path2D ban = new Path2D.Float();
        ban.moveTo(-5,-18); ban.quadTo(22,-15,18,18);
        ban.quadTo(16,20,12,17); ban.quadTo(14,0,-8,-13);
        ban.closePath();
        g3.fill(ban);
        g3.setColor(new Color(180,140,0));
        g3.setStroke(new BasicStroke(2f));
        g3.draw(ban);
        g3.dispose();
    }
 
    void drawTitle(Graphics2D g2) {
        // Shadow
        g2.setFont(new Font("Arial Black", Font.BOLD, 50));
        g2.setColor(new Color(80,40,0,180));
        g2.drawString("BANANA CATCH", 34, 67);
        g2.drawString("FRENZY", 84, 117);
        // White stroke effect
        g2.setColor(Color.WHITE);
        g2.drawString("BANANA CATCH", 30, 63);
        g2.setColor(new Color(255,224,0));
        g2.drawString("FRENZY", 80, 113);
        // Outline
        FontMetrics fm = g2.getFontMetrics();
    }
 
    void drawSignPost(Graphics2D g2) {
        int beamX = 480;
        int beamY = 26;
        int beamW = 340;
        int beamH = 16;
        g2.setFont(new Font("Arial Black", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        int signWidth = Math.max(220, fm.stringWidth("OPTIONS") + 60);
        int baseX = beamX + (beamW - signWidth) / 2;
        int leftAnchorX = baseX + 16;
        int rightAnchorX = baseX + signWidth - 16;
 
        g2.setColor(new Color(130,90,40));
        g2.fillRoundRect(beamX, beamY, beamW, beamH, 10, 10);
        g2.setColor(new Color(90,50,10));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(beamX, beamY, beamW, beamH, 10, 10);
        g2.setColor(new Color(80,45,15));
        g2.fillOval(leftAnchorX - 7, beamY - 6, 14, 14);
        g2.fillOval(rightAnchorX - 7, beamY - 6, 14, 14);
 
        // Signs
        rPlay.setBounds(baseX, 100, signWidth, 58);
        rShop.setBounds(baseX, 172, signWidth, 58);
        rOptions.setBounds(baseX, 244, signWidth, 58);
        rExit.setBounds(baseX, 316, signWidth, 58);
        drawRopesAndBeam(g2, leftAnchorX, rightAnchorX, beamY, rExit.y + rExit.height + 20);
 
        drawSign(g2, rPlay,    "PLAY",    false, rPlay.contains(mx,my));
        drawSign(g2, rShop,    "SHOP",    false, rShop.contains(mx,my));
        drawSign(g2, rOptions, "OPTIONS", false, rOptions.contains(mx,my));
        drawSign(g2, rExit,    "EXIT",    true,  rExit.contains(mx,my));
    }
 
    void drawSign(Graphics2D g2, Rectangle r, String text, boolean isExit, boolean hover) {
        // 3D shadow
        g2.setColor(new Color(60,30,5));
        g2.fillRoundRect(r.x+4, r.y+5, r.width, r.height, 10, 10);
 
        // Plank body
        Color c1 = isExit ? new Color(178,70,26)  : new Color(196,137,42);
        Color c2 = isExit ? new Color(130,48,16)  : new Color(152,100,28);
        Color c3 = isExit ? new Color(178,70,26)  : new Color(196,137,42);
        if (hover) { c1 = c1.brighter(); c2 = c2.brighter(); c3 = c3.brighter(); }
 
        GradientPaint gp = new GradientPaint(r.x, r.y, c1, r.x, r.y+r.height/2, c2);
        g2.setPaint(gp);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        GradientPaint gp2 = new GradientPaint(r.x, r.y+r.height/2, c2, r.x, r.y+r.height, c3);
        g2.setPaint(gp2);
        g2.fillRoundRect(r.x, r.y+r.height/2, r.width, r.height/2+5, 10, 10);
 
        // Border
        g2.setColor(new Color(80,40,5));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
 
        // Highlight top
        g2.setColor(new Color(255,255,255,50));
        g2.fillRoundRect(r.x+2, r.y+2, r.width-4, r.height/2-2, 8, 8);
 
        // Rope holes at the board corners
        drawRopeHole(g2, r.x + 16, r.y + 6);
        drawRopeHole(g2, r.x + r.width - 16, r.y + 6);
 
        // Nail dots
        drawNail(g2, r.x+14, r.y+r.height/2);
        drawNail(g2, r.x+r.width-14, r.y+r.height/2);
 
        // Text
        g2.setFont(new Font("Arial Black", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
 
        g2.setColor(new Color(60,30,5));
        g2.drawString(text, tx+2, ty+2);
        g2.setColor(Color.WHITE);
        g2.drawString(text, tx, ty);
    }
 
    void drawNail(Graphics2D g2, int nx, int ny) {
        g2.setColor(new Color(255,220,80));
        g2.fillOval(nx-6, ny-6, 12, 12);
        g2.setColor(new Color(80,40,5));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(nx-6, ny-6, 12, 12);
        g2.setColor(new Color(255,240,160));
        g2.fillOval(nx-3, ny-3, 5, 5);
    }
 
    void drawRopeHole(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(90,60,35));
        g2.fillOval(x-7, y-7, 14, 14);
        g2.setColor(new Color(230,190,120));
        g2.fillOval(x-5, y-5, 10, 10);
        g2.setColor(new Color(120,70,35));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(x-7, y-7, 14, 14);
    }
 
    void drawRopesAndBeam(Graphics2D g2, int leftAnchorX, int rightAnchorX, int beamY, int ropeBottomY) {
        int ropeTopY = beamY + 8;
 
        g2.setColor(new Color(120,80,40));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(leftAnchorX, ropeTopY, leftAnchorX, ropeBottomY);
        g2.drawLine(rightAnchorX, ropeTopY, rightAnchorX, ropeBottomY);
 
        g2.setColor(new Color(90,50,20));
        g2.fillOval(leftAnchorX - 7, ropeTopY - 7, 14, 14);
        g2.fillOval(rightAnchorX - 7, ropeTopY - 7, 14, 14);
    }
 
    //  PLAY SCREEN
    void drawPlay(Graphics2D g2) {
        drawJungleBg(g2);
        for (Banana b : bananas) b.draw(g2);
 
        // Shield bubble
        if (powerUps.isActive("shield")) {
            g2.setColor(new Color(0,180,255,40));
            g2.fillOval(player.x - 20, player.y - 20, player.w+40, player.h+40);
            g2.setColor(new Color(0,200,255,140));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(player.x-20, player.y-20, player.w+40, player.h+40);
        }
 
        player.draw(g2);
        drawHUD(g2);
        drawPowerUpBars(g2);
 
        if (System.currentTimeMillis() - shopMsgTime < 2000 && !shopMsg.isEmpty()) {
            g2.setFont(fontSm);
            g2.setColor(new Color(255,255,255,220));
            int msgWidth = g2.getFontMetrics().stringWidth(shopMsg);
            g2.drawString(shopMsg, (W - msgWidth) / 2, H -5);
        }
    }
 
    void drawHUD(Graphics2D g2) {
        drawHudBadge(g2, 10, 10, "Score: " + player.score);
        drawHudBadge(g2, W/2 - 50, 10, "Coins: " + coins);
        drawHudBadge(g2, W - 290, 10, "High: " + highScore);
        drawHudBadge(g2, W - 120, 10, "Lives: " + player.lives);
 
        // ESC hint
        // Set the shared style once
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(255, 255, 255, 160)); // Subtle translucent white

        // Draw the first line
        g2.drawString("ESC = Pause", 10, H - 25); 
        
        g2.drawString("Powerups: 1=Shield 2=Magnet 3=Speed", 10, H - 10);
    }
 
    void drawHudBadge(Graphics2D g2, int bx, int by, String text) {
        g2.setFont(fontSm);
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(text) + 24, bh = 34;
        g2.setColor(new Color(0,0,0,120));
        g2.fillRoundRect(bx, by, bw, bh, 18, 18);
        g2.setColor(new Color(255,224,0));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(bx, by, bw, bh, 18, 18);
        g2.setColor(Color.WHITE);
        g2.drawString(text, bx+12, by+22);
    }
 
    void drawPowerUpBars(Graphics2D g2) {
        int py = 55;
        if (powerUps.isActive("shield")) { drawBar(g2, "SHIELD", powerUps.getTime("shield"), 600, new Color(0,180,255), py); py+=28; }
        if (powerUps.isActive("magnet")) { drawBar(g2, "MAGNET", powerUps.getTime("magnet"), 600, new Color(255,80,200), py);  py+=28; }
        if (powerUps.isActive("speed"))  { drawBar(g2, "SPEED",  powerUps.getTime("speed"),  600, new Color(255,200,0), py); }
    }
 
    void drawBar(Graphics2D g2, String label, int time, int max, Color col, int py) {
        int bx = W-145, bw = 130, bh = 18;
        g2.setColor(new Color(0,0,0,100));
        g2.fillRoundRect(bx, py, bw, bh, 8, 8);
        g2.setColor(col);
        g2.fillRoundRect(bx, py, (int)(bw*(time/(double)max)), bh, 8, 8);
        g2.setFont(new Font("Arial Black", Font.BOLD, 11));
        g2.setColor(Color.WHITE);
        g2.drawString(label, bx+4, py+13);
    }
 
    //  PAUSE SCREEN
    void drawPause(Graphics2D g2) {
        drawJungleBg(g2);
 
        // Dim overlay
        g2.setColor(new Color(0,0,0,150));
        g2.fillRect(0,0,W,H);
 
        int px = 260, py = 180, pw = 280, ph = 290;
        drawWoodPanel(g2, px, py, pw, ph);
 
        g2.setFont(new Font("Arial Black", Font.BOLD, 44));
        FontMetrics fm = g2.getFontMetrics();
        String title = "PAUSED";
        int tx = px + (pw - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(80,40,0));
        g2.drawString(title, tx+3, py+70+3);
        g2.setColor(new Color(255,224,0));
        g2.drawString(title, tx, py+70);
 
        g2.setFont(fontSm);
        g2.setColor(Color.WHITE);
        String msg = "Press ESC to resume";
        g2.drawString(msg, px + (pw - g2.getFontMetrics().stringWidth(msg)) / 2, py + 110);
 
        int buttonW = pw - 80;
        int buttonX = px + (pw - buttonW) / 2;
        rPauseContinue.setBounds(buttonX, py + 130, buttonW, 52);
        rPauseMenu.setBounds(buttonX, py + 204, buttonW, 52);
        drawGreenButton(g2, rPauseContinue, "Continue", rPauseContinue.contains(mx,my));
        drawGreenButton(g2, rPauseMenu,     "Menu",     rPauseMenu.contains(mx,my));
    }

    //  GAME OVER SCREEN
    void drawGameOver(Graphics2D g2) {
        drawBackground(g2);
 
        // Dim overlay
        g2.setColor(new Color(0,0,0,140));
        g2.fillRect(0,0,W,H);
 
        // Wood panel
        int px = 180, py = 140, pw = 440, ph = 350;
        drawWoodPanel(g2, px, py, pw, ph);
 
        // "GAME OVER!" title
        g2.setFont(new Font("Arial Black", Font.BOLD, 52));
        FontMetrics fm = g2.getFontMetrics();
        String go = "GAME OVER!";
        int tx = px + (pw - fm.stringWidth(go)) / 2;
        g2.setColor(new Color(80,40,0));
        g2.drawString(go, tx+3, py+80+3);
        g2.setColor(new Color(255,224,0));
        g2.drawString(go, tx, py+80);
 
        // Score
        g2.setFont(fontMed);
        fm = g2.getFontMetrics();
        String sc = "Score: " + player.score;
        g2.setColor(Color.WHITE);
        g2.drawString(sc, px + (pw - fm.stringWidth(sc))/2, py+140);
 
        String hs = "High Score: " + highScore;
        g2.setFont(fontSm);
        fm = g2.getFontMetrics();
        g2.drawString(hs, px + (pw - fm.stringWidth(hs))/2, py+175);
 
        // Buttons
        int buttonW = pw - 220;
        int buttonX = px + (pw - buttonW) / 2;
        rGoPlayAgain.setBounds(buttonX, py + 200, buttonW, 52);
        rGoMenu.setBounds(buttonX, py + 268, buttonW, 52);
        drawGreenButton(g2, rGoPlayAgain, "Play Again", rGoPlayAgain.contains(mx,my));
        drawGreenButton(g2, rGoMenu,      "Menu",       rGoMenu.contains(mx,my));
    }
 
    //  SHOP SCREEN
    void drawShop(Graphics2D g2) {
        drawBackground(g2);
        g2.setColor(new Color(0,0,0,140));
        g2.fillRect(0,0,W,H);
 
        int px = 170, py = 100, pw = 460, ph = 360;
        drawWoodPanel(g2, px, py, pw, ph);
 
        // Title
        g2.setFont(new Font("Arial Black", Font.BOLD, 38));
        FontMetrics fm = g2.getFontMetrics();
        String title = "SHOP";
        g2.setColor(new Color(80,40,0));
        g2.drawString(title, px+(pw-fm.stringWidth(title))/2+2, py+55+2);
        g2.setColor(new Color(255,224,0));
        g2.drawString(title, px+(pw-fm.stringWidth(title))/2, py+55);
 
        // Coins display
        g2.setFont(fontSm);
        g2.setColor(Color.WHITE);
        g2.drawString("Your coins: " + coins, px+20, py+90);
 
        // Items
        drawShopItem(g2, px+20, py+110, pw-40, "Shield (10s)", "30 coins", rBuyShield, rBuyShield.contains(mx,my), "shield");
        drawShopItem(g2, px+20, py+168, pw-40, "Magnet (10s)", "40 coins", rBuyMagnet, rBuyMagnet.contains(mx,my), "magnet");
        drawShopItem(g2, px+20, py+226, pw-40, "Speed Boost", "25 coins", rBuySpeed, rBuySpeed.contains(mx,my), "speed");
 
        // Message
        if (System.currentTimeMillis() - shopMsgTime < 2000 && !shopMsg.isEmpty()) {
            g2.setFont(fontSm);
            g2.setColor(shopMsgOk ? new Color(100,220,60) : new Color(255,80,80));
            fm = g2.getFontMetrics();
            g2.drawString(shopMsg, px + (pw - fm.stringWidth(shopMsg)) / 2, py + -5);
        }
 
        drawGreenButton(g2, rShopClose, "Close", rShopClose.contains(mx,my));
    }
 
    void drawShopItem(Graphics2D g2, int ix, int iy, int iw, String name, String cost, Rectangle buyRect, boolean hover, String type) {
        g2.setColor(new Color(0,0,0,60));
        g2.fillRoundRect(ix, iy, iw, 48, 10, 10);
        g2.setFont(fontSm);
        g2.setColor(Color.WHITE);
        g2.drawString(name, ix+12, iy+30);
 
        int charges = getCharges(type);
        String buttonText;
        if (charges == 0) {
            buttonText = cost;
        } else if (powerUps.isActive(type)) {
            buttonText = "Active";
        } else {
            buttonText = "Activate (" + charges + ")";
        }
        buyRect.setSize(140, 40);
        buyRect.setLocation(ix + iw - buyRect.width - 10, iy + 4);
        drawGreenButton(g2, buyRect, buttonText, hover);
    }
 
    //  OPTIONS SCREEN
    void drawOptions(Graphics2D g2) {
        drawBackground(g2);
        g2.setColor(new Color(0,0,0,140));
        g2.fillRect(0,0,W,H);
 
        int px = 170, py = 120, pw = 460, ph = 320;
        drawWoodPanel(g2, px, py, pw, ph);
 
        g2.setFont(new Font("Arial Black", Font.BOLD, 38));
        FontMetrics fm = g2.getFontMetrics();
        String title = "OPTIONS";
        g2.setColor(new Color(80,40,0));
        g2.drawString(title, px+(pw-fm.stringWidth(title))/2+2, py+55+2);
        g2.setColor(new Color(255,224,0));
        g2.drawString(title, px+(pw-fm.stringWidth(title))/2, py+55);
 
        // Difficulty
        g2.setFont(fontMed);
        g2.setColor(Color.WHITE);
        g2.drawString("Difficulty:", px+20, py+110);
 
        String[] diffs = {"Easy","Medium","Hard"};
        Rectangle[] dRects = { rOptEasy, rOptMedium, rOptHard };
        Color[] dcols  = {new Color(60,180,60), new Color(200,160,0), new Color(200,60,60)};
        for (int i = 0; i < 3; i++) {
            Rectangle dr = dRects[i];
            boolean sel = (i == diffChoice);
            boolean hov = dr.contains(mx,my);
            g2.setColor(sel ? dcols[i] : new Color(80,50,10));
            g2.fillRoundRect(dr.x, dr.y, dr.width, dr.height, 10,10);
            if (sel || hov) {
                g2.setColor(sel ? dcols[i].brighter() : new Color(120,80,20));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(dr.x, dr.y, dr.width, dr.height, 10,10);
            }
            g2.setFont(fontSm);
            g2.setColor(Color.WHITE);
            fm = g2.getFontMetrics();
            g2.drawString(diffs[i], dr.x+(dr.width-fm.stringWidth(diffs[i]))/2, dr.y+25);
        }
 
        // Current difficulty label
        g2.setFont(fontSm);
        g2.setColor(new Color(255,224,0));
        g2.drawString("Selected: " + diffs[diffChoice], px+20, py+195);
 
        // Controls info
        g2.setColor(new Color(220,220,220));
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        g2.drawString("Controls:  Left/Right Arrow Keys to move   ESC = Menu", px+20, py+230);
 
        drawGreenButton(g2, rOptClose, "Save & Close", rOptClose.contains(mx,my));
    }
 
    // 
    //  SHARED HELPERS
    // 
    void drawWoodPanel(Graphics2D g2, int px, int py, int pw, int ph) {
        // Shadow
        g2.setColor(new Color(0,0,0,100));
        g2.fillRoundRect(px+6, py+8, pw, ph, 22, 22);
 
        // Body gradient
        GradientPaint gp = new GradientPaint(px,py, new Color(196,137,42), px,py+ph, new Color(130,85,20));
        g2.setPaint(gp);
        g2.fillRoundRect(px, py, pw, ph, 22, 22);
 
        // Wood grain lines
        g2.setColor(new Color(0,0,0,25));
        g2.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < ph; i += 18) {
            g2.drawLine(px+10, py+i, px+pw-10, py+i);
        }
 
        // Top highlight
        g2.setColor(new Color(255,255,255,35));
        g2.fillRoundRect(px+3, py+3, pw-6, ph/2, 18,18);
 
        // Border
        g2.setColor(new Color(80,45,8));
        g2.setStroke(new BasicStroke(4f));
        g2.drawRoundRect(px, py, pw, ph, 22, 22);
 
        // Inner border highlight
        g2.setColor(new Color(255,220,100,60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(px+4, py+4, pw-8, ph-8, 18, 18);
    }
 
    void drawGreenButton(Graphics2D g2, Rectangle r, String text, boolean hover) {
        // Shadow
        g2.setColor(new Color(30,80,10));
        g2.fillRoundRect(r.x+2, r.y+5, r.width, r.height, 30, 30);
 
        // Body
        Color top = hover ? new Color(110,210,60) : new Color(90,185,50);
        Color bot = hover ? new Color(60,140,25)  : new Color(45,120,20);
        GradientPaint gp = new GradientPaint(r.x, r.y, top, r.x, r.y+r.height, bot);
        g2.setPaint(gp);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 30, 30);
 
        // Border
        g2.setColor(new Color(30,90,10));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 30, 30);
 
        // Highlight
        g2.setColor(new Color(255,255,255,60));
        g2.fillRoundRect(r.x+4, r.y+4, r.width-8, r.height/2-4, 24, 24);
 
        // Text
        g2.setFont(fontSm);
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2 - 1;
        g2.setColor(new Color(20,60,5));
        g2.drawString(text, tx+1, ty+2);
        g2.setColor(Color.WHITE);
        g2.drawString(text, tx, ty);
    }
}
