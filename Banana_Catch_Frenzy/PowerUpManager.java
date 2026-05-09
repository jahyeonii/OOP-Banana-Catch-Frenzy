import java.util.HashMap;
 
public class PowerUpManager {
    private HashMap<String, Integer> timers = new HashMap<>();
 
    public void activate(String type, int duration) {
        timers.put(type, duration);
    }
 
    public boolean isActive(String type) {
        return timers.getOrDefault(type, 0) > 0;
    }
 
    public int getTime(String type) {
        return timers.getOrDefault(type, 0);
    }
 
    public void update() {
        timers.replaceAll((k, v) -> Math.max(0, v - 1));
    }
 
    public void reset() {
        timers.clear();
    }
}
 
