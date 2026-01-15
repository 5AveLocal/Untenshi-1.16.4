package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.atosign.getLocFromString;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.motion.minSpeedLimit;

class speedsign extends SignAction {

    static Location getFullLoc(String loctext, Location signloc) {
        double[] newloc = new double[3];
        getLocFromString(loctext, signloc, newloc);
        return new Location(signloc.getWorld(), newloc[0], newloc[1], newloc[2]);
    }

    static boolean isLocOfSign(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return bl instanceof Sign;
    }

    static Sign getSignFromLoc(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return bl instanceof Sign ? (Sign) bl : null;
    }

    static Chest getChestFromLoc(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return bl instanceof Chest ? (Chest) bl : null;
    }

    static Location getActualRefPos(Location loc, World w) {
        int[] blkoffset = new int[]{0, 0, 0};
        if (isLocOfSign(loc)) {
            Sign sign = (Sign) w.getBlockAt(loc).getState();
            if (sign instanceof WallSign) {
                blkoffset[1] = 1;
                WallSign ws = (WallSign) sign;
                switch (String.valueOf(ws.getFacing())) {
                    case "NORTH":
                        blkoffset[2] = 1;
                        break;
                    case "SOUTH":
                        blkoffset[2] = -1;
                        break;
                    case "WEST":
                        blkoffset[0] = 1;
                        break;
                    case "EAST":
                        blkoffset[0] = -1;
                        break;
                }
            } else {
                blkoffset[1] = 2;
            }
            do {
                int railsuccess = 0;
                for (Material mat : new Material[]{Material.RAIL, Material.ACTIVATOR_RAIL, Material.DETECTOR_RAIL, Material.POWERED_RAIL}) {
                    if (sign.getWorld().getBlockAt(loc.getBlockX() + blkoffset[0], loc.getBlockY() + blkoffset[1], loc.getBlockZ() + blkoffset[2]).getType().equals(mat)) {
                        railsuccess++;
                    }
                }
                if (railsuccess > 0) break;
                blkoffset[1]++;
                // Anti over height limit / finding rail failed
                if (loc.getY() + blkoffset[1] > 320) {
                    blkoffset[1] = 1;
                    break;
                }
            } while (true);
        }
        return new Location(w, loc.getX() + blkoffset[0] + 0.5, loc.getY() + blkoffset[1] + CART_Y_POS_DIFF, loc.getZ() + blkoffset[2] + 0.5);
    }

    static void signImproper(Location loc, utsdriver ld) {
        String s = UTS_HEAD + ChatColor.RED + getLang("signimproper") + " (" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ")";
        if (ld != null && ld.getP() != null) {
            ld.getP().sendMessage(s);
        }
        Bukkit.getConsoleSender().sendMessage(s);
    }

    static boolean limitSpeedIncorrect(CommandSender p, int speedlimit) {
        if (speedlimit < 0 || Math.floorMod(speedlimit, 5) != 0 || speedlimit > MAX_SPEED) {
            if (p != null) {
                generalMsg(p, ChatColor.RED, getLang("argwrong"));
            }
            return true;
        }
        return false;
    }

    static void speedSignWarn(utsvehicle lv, Location eventloc, String signloc) {
        Sign warn = getSignFromLoc(getFullLoc(signloc, eventloc));
        if (warn != null) {
            lv.setLastspsign(warn.getLocation());
            int warnsp = parseInt(warn.getLine(2));
            lv.setLastspsp(warnsp);
            if (warnsp < MAX_SPEED) {
                // ATC signal and speed limit min value
                if (lv.getSafetysystype().equals("atc") && warnsp != minSpeedLimit(lv)) {
                    warnsp = Math.min(Math.min(lv.getLastsisp(), lv.getLastspsp()), lv.getSignallimit());
                    generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("signal_warn") + " " + ChatColor.GOLD + "ATC" + ChatColor.GRAY + " " + warnsp + " km/h");
                } else {
                    generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("speedlimit_warn") + " " + warnsp + " km/h");
                }
            } else if (eventloc != null) {
                signImproper(eventloc, lv.getLd());
            }
        } else if (eventloc != null) {
            signImproper(eventloc, lv.getLd());
        }
    }

    static void speedSignSet(utsvehicle lv, Location eventloc, int speed) {
        if (limitSpeedIncorrect(null, speed)) {
            signImproper(eventloc, lv.getLd());
            return;
        }
        int oldspeedlimit = lv.getSpeedlimit();
        lv.setSpeedlimit(speed);
        // ATC signal and speed limit min value
        if (lv.getSafetysystype().equals("atc")) {
            speed = Math.min(lv.getSignallimit(), lv.getSpeedlimit());
            if (speed != Math.min(oldspeedlimit, lv.getSignallimit())) {
                String temp = speed >= MAX_SPEED ? getLang("speedlimit_del") : speed + " km/h";
                generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("signal_set") + " " + ChatColor.GOLD + "ATC" + ChatColor.GRAY + " " + temp);
            }
        } else {
            generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("speedlimit_set") + " " + (speed == MAX_SPEED ? ChatColor.GREEN + getLang("speedlimit_del") : speed + " km/h"));
        }
        if (speed != 0) {
            lv.setLastspsign(null);
            lv.setLastspsp(MAX_SPEED);
        }
    }

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("speedsign");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            MinecartGroup mg = cartevent.getGroup();
            String speedsign = cartevent.getLine(2);
            Location eventloc = cartevent.getLocation();
            utsvehicle lv = vehicle.get(mg);
            // Speed limit set
            if (lv != null) {
                if (!cartevent.getLine(2).equals("warn")) {
                    try {
                        speedSignSet(lv, eventloc, parseInt(speedsign));
                    } catch (NumberFormatException e) {
                        signImproper(eventloc, lv.getLd());
                    }
                }
                // Speed limit warn
                else {
                    try {
                        String signloc = cartevent.getLine(3);
                        speedSignWarn(lv, eventloc, signloc);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        signImproper(eventloc, lv.getLd());
                    }
                }
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noSignPerm(e)) return true;
        Player p = e.getPlayer();
        try {
            int intspeed;
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Speed limit sign");
            if (!e.getLine(2).equals("warn")) {
                intspeed = parseInt(e.getLine(2));
                if (limitSpeedIncorrect(p, intspeed)) e.setCancelled(true);
                opt.setDescription("set speed limit for train");
            } else {
                getLocFromString(e.getLine(3), e.getLocation(), new double[3]);
                opt.setDescription("set speed limit warning for train");
            }
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            generalMsg(p, ChatColor.RED, getLang("signimproper"));
            e.setCancelled(true);
        }
        return true;
    }
}