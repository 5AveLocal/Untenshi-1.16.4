package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Arrays;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.cmds.errorLog;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.stoppos.curveRailPosFix;
import static me.fiveave.untenshi.utsvehicle.initVehicle;

class atosign extends SignAction {

    static void atoSignStopTime(utsvehicle lv, int val) {
        lv.setAtoautodep(val >= 0 && (lv.getLd() == null || lv.getLd().isAllowatousage()));
        lv.setAtostoptime(Math.abs(val));
        generalMsg(lv.getLd(), ChatColor.GOLD, getLang("ato_detectstoptime"));
    }

    static void atoSignDefault(utsvehicle lv, double val, double[] loc) {
        lv.setOverrun(false);
        // Direct or indirect pattern?
        lv.setAtopisdirect(val < 0);
        lv.setAtospeed(Math.abs(val));
        curveRailPosFix(lv, loc);
        lv.setAtodest(new Location(lv.getSavedworld(), loc[0], loc[1], loc[2]));
        generalMsg(lv.getLd(), ChatColor.GOLD, getLang("ato_detectpattern"));
    }

    static void atoSignDir(utsvehicle lv, MinecartGroup mg, MinecartMember<?> mm, String dir, SignActionEvent cartevent) {
        BlockFace bf = BlockFace.valueOf(dir);
        if (mm.getDirection().getOppositeFace().equals(bf)) {
            mg.reverse();
            lv.setDriverseat(mg.head());
            generalMsg(lv.getLd(), ChatColor.GOLD, getLang("dir_info") + " " + getLang("dir_" + mg.head().getDirection().toString().toLowerCase()));
            if (cartevent != null) {
                cartevent.setLevers(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> cartevent.setLevers(false), 4);
            }
        }
    }

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("atosign");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        try {
            if (cartevent.hasRailedMember() && cartevent.isPowered()) {
                // For each passenger on cart
                MinecartGroup mg = cartevent.getGroup();
                MinecartMember<?> mm = cartevent.getMember();
                utsvehicle lv = vehicle.get(mg);
                // Register train if not yet
                if (lv == null && cartevent.getLine(2).equals("reg")) {
                    initVehicle(mg);
                    utsvehicle lv2 = vehicle.get(mg);
                    lv2.setBrake(8);
                    lv2.setMascon(0);
                } else if (lv != null && lv.getTrain() != null) {
                    switch (cartevent.getLine(2)) {
                        case "reg":
                            // Do nothing, train is already registered
                            break;
                        case "stoptime":
                            if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON)) {
                                int val = parseInt(cartevent.getLine(3));
                                atoSignStopTime(lv, val);
                            }
                            break;
                        case "dir":
                            // Only activate if train is stopped
                            if (lv.getSpeed() == 0 && (lv.getLd() == null || lv.getLd().isAllowatousage())) {
                                String dir = cartevent.getLine(3).toUpperCase();
                                atoSignDir(lv, mg, mm, dir, cartevent);
                            }
                            break;
                        case "stopaction":
                            if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON)) {
                                double[] loc = new double[3];
                                String[] sloc = cartevent.getLine(3).split(" ");
                                for (int a = 0; a <= 2; a++) {
                                    loc[a] = Integer.parseInt(sloc[a]);
                                }
                                lv.setStopactionpos(new Location(cartevent.getWorld(), loc[0], loc[1], loc[2]));
                                generalMsg(lv.getLd(), ChatColor.GOLD, getLang("ato_detectstopaction"));
                            }
                            break;
                        default:
                            // Only activate if train is not overrun
                            if (!lv.isOverrun() && cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && (lv.getLd() == null || lv.getLd().isAllowatousage())) {
                                double[] loc = new double[3];
                                String[] sloc = cartevent.getLine(3).split(" ");
                                double val = Double.parseDouble(cartevent.getLine(2));
                                for (int a = 0; a <= 2; a++) {
                                    loc[a] = Integer.parseInt(sloc[a]);
                                }
                                atoSignDefault(lv, val, loc);
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            errorLog(e, "atosign.execute");
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noSignPerm(e)) return true;
        Player p = e.getPlayer();
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "ATO sign");
            switch (e.getLine(2)) {
                case "stoptime":
                    int stoptimeval = parseInt(e.getLine(3));
                    if (stoptimeval >= 1) {
                        opt.setDescription("set ATO station stopping time for train, train departs automatically after doors close");
                    } else if (Math.abs(stoptimeval) >= 1) {
                        opt.setDescription("set ATO station stopping time for train, train does not depart automatically after doors close");
                    } else {
                        generalMsg(p, ChatColor.RED, getLang("argwrong"));
                        e.setCancelled(true);
                    }
                    break;
                case "dir":
                    opt.setDescription("set direction for train");
                    boolean match = Arrays.asList("north", "south", "east", "west", "north_east", "north_west", "south_east", "south_west").contains(e.getLine(3).toLowerCase());
                    if (!match) {
                        generalMsg(p, ChatColor.RED, getLang("dir_notexist"));
                        e.setCancelled(true);
                    }
                    break;
                case "reg":
                    opt.setDescription("register vehicle as Untenshi vehicle");
                    break;
                case "stopaction":
                    opt.setDescription("register stop action for train");
                    break;
                default:
                    double val = parseInt(e.getLine(2));
                    opt.setDescription(val >= 0 ? "set ATO indirect pattern for train" : "set ATO direct pattern for train");
                    if (val > maxspeed) {
                        generalMsg(p, ChatColor.RED, getLang("argwrong"));
                        e.setCancelled(true);
                    }
                    for (String i : e.getLine(3).split(" ")) {
                        parseInt(i);
                    }
                    break;
            }
            return opt.handle(p);
        } catch (Exception exception) {
            generalMsg(p, ChatColor.RED, getLang("signimproper"));
            e.setCancelled(true);
        }
        return true;
    }
}