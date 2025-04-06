package RavenMC.ravenRPG.Managers;

// PlayerRaven class to store raven data
public class PlayerRaven {
    private String type;
    private int level;
    private int xp;
    private int color;

    public PlayerRaven(String type, int level, int xp, int color) {
        this.type = type;
        this.level = level;
        this.xp = xp;
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
