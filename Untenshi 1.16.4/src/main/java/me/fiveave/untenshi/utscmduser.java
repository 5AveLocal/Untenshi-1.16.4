package me.fiveave.untenshi;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import static me.fiveave.untenshi.main.cmduser;

class utscmduser {

    private final Player p;
    private Location signloc;
    private String signtype;
    private String signdir;
    private boolean signplacing;

    utscmduser(Player p) {
        this.p = p;
    }

    static void initCmdUser(Player p) {
        utscmduser cu = new utscmduser(p);
        cmduser.putIfAbsent(p, cu);
    }

    public Player getP() {
        return p;
    }

    public Location getSignloc() {
        return signloc;
    }

    public void setSignloc(Location signloc) {
        this.signloc = signloc;
    }

    public String getSigntype() {
        return signtype;
    }

    public void setSigntype(String signtype) {
        this.signtype = signtype;
    }

    public String getSigndir() {
        return signdir;
    }

    public void setSigndir(String signdir) {
        this.signdir = signdir;
    }

    public boolean isSignplacing() {
        return signplacing;
    }

    public void setSignplacing(boolean signplacing) {
        this.signplacing = signplacing;
    }
}
