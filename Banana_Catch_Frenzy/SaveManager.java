import java.io.*;
 
public class SaveManager {
    private static final String FILE = "bcf_save.txt";
 
    public static void save(int coins, int difficulty, int highScore) {
        try (PrintWriter pw = new PrintWriter(FILE)) {
            pw.println(coins);
            pw.println(difficulty);
            pw.println(highScore);
        } catch (Exception ignored) {}
    }
 
    public static int[] load() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String coinsLine = br.readLine();
            String diffLine = br.readLine();
            String scoreLine = br.readLine();
            int coins = coinsLine != null ? Integer.parseInt(coinsLine.trim()) : 100;
            int difficulty = diffLine != null ? Integer.parseInt(diffLine.trim()) : 1;
            int highScore = scoreLine != null ? Integer.parseInt(scoreLine.trim()) : 0;
            return new int[]{coins, difficulty, highScore};
        } catch (Exception e) {
            return new int[]{100, 1, 0};
        }
    }
}
 