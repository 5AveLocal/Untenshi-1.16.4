package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.atosign.getLocFromString;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.motion.minSpeedLimit;
import static me.fiveave.untenshi.signcache.*;

class speedsign extends SignAction {

    static void speedSignWarn(utsvehicle lv, Location eventloc, String signlocstr) {
        Location signloc = getFullLoc(signlocstr, eventloc);
        Sign warn = getSignFromLoc(signloc);
        if (warn != null) {
            lv.setLastspsign(signloc);
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