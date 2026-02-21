package me.fiveave.untenshi;

import org.bukkit.entity.Player;

import static me.fiveave.untenshi.cmds.getPConfig;
import static me.fiveave.untenshi.main.driver;

class utsdriver {

    private final Player p;
    private int points;
    private boolean playing;
    private boolean freemode;
    private boolean frozen;
    private boolean allowatousage;
    private utsvehicle lv;

    utsdriver(Player p, Boolean freemode, Boolean allowatousage) {
        this.p = p;
        this.setPlaying(false);
        this.setFrozen(false);
        this.setAllowatousage(allowatousage);
        this.setFreemode(freemode);
        this.setPoints(30);
        this.setLv(null);
    }

    static void initDriver(Player p) {
        utsdriver ld = driver.get(p);
        if (ld == null) {
            ld = new utsdriver(p, getPConfig().getBoolean("players." + p.getUniqueId() + ".freemode"), getPConfig().getBoolean("players." + p.getUniqueId() + ".allowatousage"));
            driver.put(p, ld);
        }
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isFreemode() {
        return freemode;
    }

    public void setFreemode(boolean freemode) {
        this.freemode = freemode;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean isAllowatousage() {
        return allowatousage;
    }

    public void setAllowatousage(boolean allowatousage) {
        this.allowatousage = allowatousage;
    }

    public Player getP() {
        return p;
    }

    public utsvehicle getLv() {
        return lv;
    }

    public void setLv(utsvehicle lv) {
        this.lv = lv;
    }
}
