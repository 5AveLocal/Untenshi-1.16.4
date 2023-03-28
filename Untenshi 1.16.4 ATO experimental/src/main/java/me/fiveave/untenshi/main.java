package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import static me.fiveave.untenshi.cmds.helpnotitle;
import static me.fiveave.untenshi.motion.freemodenoato;
import static me.fiveave.untenshi.signalsign.resetSignals;

public final class main extends JavaPlugin implements Listener {
    public static HashMap<Player, Integer> mascon = new HashMap<>();
    public static HashMap<Player, Integer> speedlimit = new HashMap<>();
    public static HashMap<Player, Integer> signallimit = new HashMap<>();
    public static HashMap<Player, Integer> points = new HashMap<>();
    public static HashMap<Player, Integer> deductdelay = new HashMap<>();
    public static HashMap<Player, Integer> atsforced = new HashMap<>();
    public static HashMap<Player, Integer> lastsisp = new HashMap<>();
    public static HashMap<Player, Integer> lastspsp = new HashMap<>();
    public static HashMap<Player, Integer> dooropen = new HashMap<>();
    public static HashMap<Player, Integer[]> stopoutput = new HashMap<>();
    public static HashMap<Player, Integer[]> atodest = new HashMap<>();
    public static HashMap<Player, Integer> atostoptime = new HashMap<>();
    public static HashMap<Player, Double> current = new HashMap<>();
    public static HashMap<Player, Double> speed = new HashMap<>();
    public static HashMap<Player, Double> atospeed = new HashMap<>();
    public static HashMap<Player, Double[]> stoppos = new HashMap<>();
    public static HashMap<Player, Location> lastsisign = new HashMap<>();
    public static HashMap<Player, Location> lastspsign = new HashMap<>();
    public static HashMap<Player, Location[][]> lastresetablesign = new HashMap<>();
    public static HashMap<Player, String> traintype = new HashMap<>();
    public static HashMap<Player, String> signaltype = new HashMap<>();
    public static HashMap<Player, String> signalorderptn = new HashMap<>();
    public static HashMap<Player, Boolean> playing = new HashMap<>();
    public static HashMap<Player, Boolean> freemode = new HashMap<>();
    public static HashMap<Player, Boolean> reqstopping = new HashMap<>();
    public static HashMap<Player, Boolean> overrun = new HashMap<>();
    public static HashMap<Player, Boolean> fixstoppos = new HashMap<>();
    public static HashMap<Player, Boolean> staaccel = new HashMap<>();
    public static HashMap<Player, Boolean> staeb = new HashMap<>();
    public static HashMap<Player, Boolean> atsbraking = new HashMap<>();
    public static HashMap<Player, Boolean> atsping = new HashMap<>();
    public static HashMap<Player, Boolean> atspnear = new HashMap<>();
    public static HashMap<Player, Boolean> doordiropen = new HashMap<>();
    public static HashMap<Player, Boolean> doorconfirm = new HashMap<>();
    public static HashMap<Player, Boolean> frozen = new HashMap<>();
    public static HashMap<Player, Boolean> allowatousage = new HashMap<>();
    public static HashMap<Player, Boolean> atopisdirect = new HashMap<>();
    public static HashMap<Player, Boolean> atoforcebrake = new HashMap<>();
    public static HashMap<Player, MinecartGroup> train = new HashMap<>();
    public static HashMap<Player, ItemStack[]> inv = new HashMap<>();
    public static main plugin;
    static abstractfile config;
    static abstractfile langdata;
    static abstractfile traindata;
    static abstractfile playerdata;
    static abstractfile signalorder;
    static final int ticksin1s = 10;
    static final int interval = 20 / ticksin1s;

    static String getlang(String path) {
        langdata.reloadConfig();
        return Objects.requireNonNull(langdata.dataconfig.getString(path));
    }

    stoppos v1 = new stoppos();
    speedsign v2 = new speedsign();
    signalsign v3 = new signalsign();
    atosign v4 = new atosign();
    utstrain v5 = new utstrain();

    @Override
    public void onEnable() {
        // Plugin startup logic
        SignAction.register(v1);
        SignAction.register(v2);
        SignAction.register(v3);
        SignAction.register(v4);
        SignAction.register(v5);
        plugin = this;
        // If langdata not init twice will cause UTF-8 characters not formatted properly
        config = new abstractfile(this, "config.yml");
        for (String s : Arrays.asList("en_US", "zh_TW", "JP", plugin.getConfig().getString("lang"))) {
            langdata = new abstractfile(this, "lang_" + s + ".yml");
        }
        traindata = new abstractfile(this, "traindata.yml");
        playerdata = new abstractfile(this, "playerdata.yml");
        signalorder = new abstractfile(this, "signalorder.yml");
        this.saveDefaultConfig();
        PluginManager pm = this.getServer().getPluginManager();
        Objects.requireNonNull(this.getCommand("uts")).setExecutor(new cmds());
        Objects.requireNonNull(this.getCommand("uts")).setTabCompleter(new cmds());
        Objects.requireNonNull(this.getCommand("utssignal")).setExecutor(new signalcmd());
        Objects.requireNonNull(this.getCommand("utssignal")).setTabCompleter(new signalcmd());
        pm.registerEvents(new events(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (Player p : this.getServer().getOnlinePlayers()) {
            // Prevent plugin shutdown affecting playing status
            restoreinit(p);
        }
        SignAction.unregister(v1);
        SignAction.unregister(v2);
        SignAction.unregister(v3);
        SignAction.unregister(v4);
        SignAction.unregister(v5);
    }

    static boolean noperm(SignChangeActionEvent e) {
        if (!e.getPlayer().hasPermission("uts.sign")) {
            e.getPlayer().sendMessage(ChatColor.RED + getlang("noperm"));
            e.setCancelled(true);
            return true;
        }
        return false;
    }

    static String pureutstitle = ChatColor.YELLOW + "[========== " + ChatColor.GREEN + "Untenshi " + ChatColor.YELLOW + "==========]\n";

    static String utshead = "[" + ChatColor.GREEN + "Untenshi" + ChatColor.WHITE + "] ";

    static void pointCounter(Player p, ChatColor color, String s, int pts, String str) {
        ChatColor color2 = pts > 0 ? ChatColor.GREEN : ChatColor.RED;
        String ptsstr = !freemodenoato(p) ? "" : String.valueOf(pts);
        p.sendMessage(utshead + color + s + color2 + ptsstr + str);
        if (freemodenoato(p)) {
            points.put(p, points.get(p) + pts);
        }
    }

    static void restoreinit(Player p) {
        // Get train group and stop train and open doors
        playing.putIfAbsent(p, false);
        if (playing.get(p)) {
            MinecartGroup mg = train.get(p);
            TrainProperties trainprop = mg.getProperties();
            trainprop.setSpeedLimit(0);
            mg.setForwardForce(0);
            trainprop.setPlayersEnter(true);
            trainprop.setPlayersExit(true);
            speed.put(p, 0.0);
            points.put(p, 30);
            overrun.put(p, false);
            reqstopping.put(p, false);
            fixstoppos.put(p, false);
            playing.put(p, false);
            train.remove(p);
            stoppos.remove(p);
            speedlimit.remove(p);
            signallimit.remove(p);
            atospeed.remove(p);
            atodest.remove(p);
            atostoptime.remove(p);
            atopisdirect.remove(p);
            atoforcebrake.remove(p);
            lastsisign.remove(p);
            lastspsign.remove(p);
            lastsisp.remove(p);
            lastspsp.remove(p);
            // Delete owners
            trainprop.clearOwners();
            // Clear Inventory
            for (int i = 0; i < 41; i++) {
                p.getInventory().setItem(i, new ItemStack(Material.AIR));
            }
            // Reset inventory
            p.getInventory().setContents(inv.get(p));
            p.updateInventory();
            helpnotitle(p, ChatColor.YELLOW, getlang("activate") + ChatColor.RED + getlang("disable"));
            // Reset signals
            try {
                Location[][] locs = lastresetablesign.get(p);
                for (Location[] locs1 : locs) {
                    resetSignals(p.getWorld(), locs1);
                }
            } catch (Exception ignored) {
            }
            lastresetablesign.remove(p);
            signalorderptn.remove(p);
        }
    }
}
