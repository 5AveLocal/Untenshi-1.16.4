package me.fiveave.untenshi;

import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.List;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.signalsign.signimproper;

class speedsign extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("speedsign");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            String speedsign = cartevent.getLine(2);
            for (MinecartMember cart : cartevent.getMembers()) {
                // For each passenger on cart
                CommonEntity cart2 = cart.getEntity();
                List cartpassengers = cart2.getPassengers();
                for (Object cartobj : cartpassengers) {
                    Player p = (Player) cartobj;
                    if (playing.containsKey(p)) {
                        signaltype.put(p, "ats");
                        if (playing.get(p)) {
                            // Speed limit set
                            if (!cartevent.getLine(2).equals("warn")) {
                                int intspeed = parseInt(speedsign);
                                if (intspeed <= 360 && intspeed >= 0 && Math.floorMod(intspeed, 5) == 0) {
                                    speedlimit.put(p, intspeed);
                                    p.sendMessage(utshead + ChatColor.YELLOW + getlang("speedlimitset") + (intspeed == 360 ? ChatColor.GREEN + getlang("nolimit") : speedlimit.get(p) + " km/h"));
                                    if (parseInt(speedsign) != 0) {
                                        lastspsign.remove(p);
                                        lastspsp.remove(p);
                                    }
//                                    atoforcebrake.put(p, false);
                                } else {
                                    signimproper(cartevent, p);
                                }
                            }
                            // Speed limit warn
                            else {
                                try {
                                    Sign warn = getSign(cartevent);
                                    if (warn != null) {
                                        lastspsign.put(p, warn.getLocation());
                                        int warnsp = parseInt(warn.getLine(2));
                                        lastspsp.put(p, warnsp);
                                        if (warnsp < 360) {
                                            p.sendMessage(utshead + ChatColor.YELLOW + getlang("speedlimitwarn") + warnsp + " km/h");
                                        } else {
                                            signimproper(cartevent, p);
                                        }
                                    } else {
                                        signimproper(cartevent, p);
                                    }
                                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                    signimproper(cartevent, p);
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noperm(e)) return true;
        try {
            int intspeed;
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Speed limit sign");
            if (!e.getLine(2).equals("warn")) {
                intspeed = parseInt(e.getLine(2));
                if (!(intspeed > 0) || !(intspeed <= 360) || !(Math.floorMod(intspeed, 5) == 0)) {
                    e.getPlayer().sendMessage(ChatColor.RED + getlang("speedlimitdiv5"));
                    e.setCancelled(true);
                }
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
            e.getPlayer().sendMessage(ChatColor.RED + getlang("signimproper"));
            e.setCancelled(true);
            exception.printStackTrace();
        }
        return true;
    }

    static Sign getSign(SignActionEvent cartevent) {
        if (cartevent.getWorld().getBlockAt(getloc(cartevent, 0), getloc(cartevent, 1), getloc(cartevent, 2)).getState() instanceof Sign) {
            return (Sign) cartevent.getWorld().getBlockAt(getloc(cartevent, 0), getloc(cartevent, 1), getloc(cartevent, 2)).getState();
        } else {
            return null;
        }
    }

    static int getloc(SignActionEvent cartevent, int i) {
        return parseInt(cartevent.getLine(3).split(" ")[i]);
    }
}