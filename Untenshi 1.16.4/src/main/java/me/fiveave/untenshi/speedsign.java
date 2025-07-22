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
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;

class speedsign extends SignAction {

    static Location getFullLoc(World world, String loctext) {
        return new Location(world, getLoc(loctext, 0), getLoc(loctext, 1), getLoc(loctext, 2));
    }

    static boolean isLocOfSign(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return bl instanceof Sign;
    }

    static Sign getSignFromLoc(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return isLocOfSign(loc) ? (Sign) bl : null;
    }

    static boolean isLocOfChest(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return bl instanceof Chest;
    }

    static Chest getChestFromLoc(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return isLocOfChest(loc) ? (Chest) bl : null;
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
        return new Location(w, loc.getX() + blkoffset[0] + 0.5, loc.getY() + blkoffset[1] + cartyposdiff, loc.getZ() + blkoffset[2] + 0.5);
    }

    static void signImproper(Location loc, utsdriver ld) {
        String s = utshead + ChatColor.RED + getLang("signimproper") + " (" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ")";
        if (ld != null && ld.getP() != null) {
            ld.getP().sendMessage(s);
        }
        Bukkit.getConsoleSender().sendMessage(s);
    }

    static int getLoc(String str, int i) {
        return parseInt(str.split(" ")[i]);
    }

    static boolean limitSpeedIncorrect(CommandSender p, int speedlimit) {
        if (speedlimit < 0 || Math.floorMod(speedlimit, 5) != 0 || speedlimit > maxspeed) {
            if (p != null) {
                generalMsg(p, ChatColor.RED, getLang("argwrong"));
            }
            return true;
        }
        return false;
    }

    static void speedSignWarn(utsvehicle lv, Location eventloc, String signloc) {
        Sign warn = getSignFromLoc(getFullLoc(lv.getSavedworld(), signloc));
        if (warn != null) {
            lv.setLastspsign(warn.getLocation());
            int warnsp = parseInt(warn.getLine(2));
            lv.setLastspsp(warnsp);
            if (warnsp < maxspeed) {
                // ATC signal and speed limit min value
                if (lv.getSafetysystype().equals("atc")) {
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
                        int intspeed = parseInt(speedsign);
                        if (limitSpeedIncorrect(null, intspeed)) {
                            signImproper(eventloc, lv.getLd());
                            return;
                        }
                        lv.setSpeedlimit(intspeed);
                        // ATC signal and speed limit min value
                        if (lv.getSafetysystype().equals("atc")) {
                            intspeed = Math.min(lv.getSignallimit(), lv.getSpeedlimit());
                            String temp = intspeed >= maxspeed ? getLang("speedlimit_del") : intspeed + " km/h";
                            generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("signal_set") + " " + ChatColor.GOLD + "ATC" + ChatColor.GRAY + " " + temp);
                        } else {
                            generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("speedlimit_set") + " " + (intspeed == maxspeed ? ChatColor.GREEN + getLang("speedlimit_del") : intspeed + " km/h"));
                        }
                        if (parseInt(speedsign) != 0) {
                            lv.setLastspsign(null);
                            lv.setLastspsp(maxspeed);
                        }
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
                String[] temp = e.getLine(3).split(" ");
                parseInt(temp[0]);
                parseInt(temp[1]);
                parseInt(temp[2]);
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