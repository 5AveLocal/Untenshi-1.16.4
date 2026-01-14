package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;

import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.events.doorControls;
import static me.fiveave.untenshi.events.toB8;
import static me.fiveave.untenshi.motion.noFreemodeOrATO;
import static me.fiveave.untenshi.signalsign.resetSignals;

public final class main extends JavaPlugin implements Listener {

    // ALL_CAPS for never changing final variables
    static final int TICKS_IN_1_S = 20;
    static final double ONE_TICK_IN_S = 0.05;
    static final int TICK_DELAY = 1;
    static final int MAX_SPEED = 360;
    static final double CART_Y_POS_DIFF = 0.0625;
    static final HashMap<MinecartGroup, utsvehicle> vehicle = new HashMap<>();
    static final HashMap<Player, utsdriver> driver = new HashMap<>();
    static final HashMap<Player, utscmduser> cmduser = new HashMap<>();
    static final HashMap<Location, Sign> loctosign = new HashMap<>();
    static final HashSet<Location> loctoother = new HashSet<>();
    static final String PURE_UTS_TITLE = ChatColor.YELLOW + "[========== " + ChatColor.GREEN + "Untenshi " + ChatColor.YELLOW + "==========]\n";
    static final String UTS_HEAD = "[" + ChatColor.GREEN + "Untenshi" + ChatColor.WHITE + "] ";
    public static main plugin;
    static abstractfile config;
    static abstractfile langdata;
    static abstractfile traindata;
    static abstractfile playerdata;
    static abstractfile signalorder;
    final stoppos sign1 = new stoppos();
    final speedsign sign2 = new speedsign();
    final signalsign sign3 = new signalsign();
    final atosign sign4 = new atosign();
    final utstrain sign5 = new utstrain();

    static String getLang(String path) {
        langdata.reloadConfig();
        String result;
        try {
            result = langdata.dataconfig.getString(path);
        } catch (Exception e) {
            result = " (lang not set for " + path + ") ";
        }
        if (result == null || result.equals("null")) {
            result = " (lang not set for " + path + ") ";
        }
        return result;
    }

    static boolean noSignPerm(SignChangeActionEvent e) {
        if (!e.getPlayer().hasPermission("uts.sign")) {
            generalMsg(e.getPlayer(), ChatColor.RED, getLang("noperm"));
            e.setCancelled(true);
            return true;
        }
        return false;
    }

    static void pointCounter(utsdriver ld, ChatColor color, String s, int pts, String str) {
        if (ld != null) {
            ChatColor color2 = pts > 0 ? ChatColor.GREEN : ChatColor.RED;
            String ptsstr = !noFreemodeOrATO(ld) ? "" : String.valueOf(pts);
            generalMsg(ld.getP(), color, s + color2 + ptsstr + str);
            if (noFreemodeOrATO(ld)) {
                ld.setPoints(ld.getPoints() + pts);
            }
        }
    }

    static void restoreInitLd(utsdriver ld) {
        // Get train group, stop train and open doors
        if (ld.isPlaying()) {
            utsvehicle lv = ld.getLv();
            if (lv.getAtodest() == null || lv.getAtospeed() == -1) {
                try {
                    MinecartGroup mg = lv.getTrain();
                    TrainProperties tprop = mg.getProperties();
                    // Stop train (TC side)
                    tprop.setSpeedLimit(0);
                    mg.setForwardForce(0);
                    // Delete owners
                    tprop.clearOwners();
                } catch (Exception ignored) {
                }
                // Stop train (Untenshi side)
                lv.setSpeed(0);
                toB8(lv);
                // Open doors (to prevent driver and passengers from being trapped)
                doorControls(lv, true);
            }
            // Reset inventory
            ld.getP().getInventory().setContents(ld.getInv());
            ld.getP().updateInventory();
            lv.setLd(null);
            try {
                driver.put(ld.getP(), new utsdriver(ld.getP(), ld.isFreemode(), ld.isAllowatousage()));
            } catch (Exception ignored) {
            }
            ld.setPlaying(false);
            generalMsg(ld.getP(), ChatColor.YELLOW, getLang("activate") + " " + ChatColor.RED + getLang("activate_off"));
        }
    }

    static void restoreInitLv(utsvehicle lv) {
        // Get train group and stop train and open doors
        try {
            MinecartGroup mg = lv.getTrain();
            TrainProperties tprop = mg.getProperties();
            tprop.setSpeedLimit(0);
            mg.setForwardForce(0);
            tprop.setPlayersEnter(true);
            tprop.setPlayersExit(true);
            // Delete owners
            tprop.clearOwners();
        } catch (Exception ignored) {
        }
        // Reset signals (resettablesign)
        final Location[] locs = lv.getRsposlist();
        resetSignals(lv.getSavedworld(), locs);
        // Reset signals (ilposoccupied)
        final Location[] locs2 = lv.getIlposoccupied();
        resetSignals(lv.getSavedworld(), locs2);
        vehicle.put(lv.getTrain(), new utsvehicle(lv.getTrain()));
    }


    @Override
    public void onEnable() {
        // Plugin startup logic
        for (SignAction sa : new SignAction[]{sign1, sign2, sign3, sign4, sign5}) {
            SignAction.register(sa);
        }
        plugin = this;
        // If langdata not init twice will cause UTF-8 characters not formatted properly
        config = new abstractfile(this, "config.yml");
        Arrays.asList("en_US", "zh_TW", "JP", plugin.getConfig().getString("lang")).forEach(s -> langdata = new abstractfile(this, "lang_" + s + ".yml"));
        traindata = new abstractfile(this, "traindata.yml");
        playerdata = new abstractfile(this, "playerdata.yml");
        signalorder = new abstractfile(this, "signalorder.yml");
        this.saveDefaultConfig();
        PluginManager pm = this.getServer().getPluginManager();
        Objects.requireNonNull(this.getCommand("uts")).setExecutor(new cmds());
        Objects.requireNonNull(this.getCommand("uts")).setTabCompleter(new cmds());
        Objects.requireNonNull(this.getCommand("utssignal")).setExecutor(new signalcmd());
        Objects.requireNonNull(this.getCommand("utssignal")).setTabCompleter(new signalcmd());
        Objects.requireNonNull(this.getCommand("utslogger")).setExecutor(new driverlog());
        Objects.requireNonNull(this.getCommand("utslogger")).setTabCompleter(new driverlog());
        Objects.requireNonNull(this.getCommand("utsdebug")).setExecutor(new debugcmd());
        Objects.requireNonNull(this.getCommand("utsdebug")).setTabCompleter(new debugcmd());
        Objects.requireNonNull(this.getCommand("utssigntool")).setExecutor(new signtool());
        Objects.requireNonNull(this.getCommand("utssigntool")).setTabCompleter(new signtool());
        try {
            pm.registerEvents(new events(), this);
            pm.registerEvents(new signtool(), this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        driver.keySet().forEach(p -> restoreInitLd(driver.get(p)));
        vehicle.keySet().forEach(mg -> restoreInitLv(vehicle.get(mg)));
        for (SignAction sa : new SignAction[]{sign1, sign2, sign3, sign4, sign5}) {
            SignAction.unregister(sa);
        }
    }

    static void errorLog(Exception e) {
        Bukkit.getLogger().log(Level.SEVERE, ChatColor.stripColor(UTS_HEAD) + "An error occurred!", e);
    }
}
