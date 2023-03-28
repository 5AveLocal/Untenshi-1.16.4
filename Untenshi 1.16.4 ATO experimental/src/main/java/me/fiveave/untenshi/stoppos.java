package me.fiveave.untenshi;

import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.main.*;

class stoppos extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("stoppos");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            //noinspection rawtypes
            for (MinecartMember cart : cartevent.getMembers()) {
                // For each passenger on cart
                //noinspection rawtypes
                CommonEntity cart2 = cart.getEntity();
                //noinspection rawtypes
                List cartps = cart2.getPassengers();
                for (Object cartobject : cartps) {
                    Player p = (Player) cartobject;
                    if (playing.containsKey(p)) {
                        if (playing.get(p)) {
                            String[] sloc = cartevent.getLine(2).split(" ");
                            String[] sloc2 = cartevent.getLine(3).split(" ");
                            Double[] loc = new Double[3];
                            Integer[] loc2 = new Integer[3];
                            for (int a = 0; a <= 2; a++) {
                                loc[a] = Double.valueOf(sloc[a]);
                                if (!cartevent.getLine(3).equals("")) {
                                    loc2[a] = Integer.parseInt(sloc2[a]);
                                }
                            }
                            loc[0] += 0.5;
                            loc[2] += 0.5;
                            stoppos.put(p, loc);
                            if (!cartevent.getLine(3).isEmpty()) {
                                stopoutput.put(p, loc2);
                            }
                            reqstopping.put(p, true);
                            p.sendMessage(utshead + ChatColor.YELLOW + getlang("nextstop"));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noperm(e)) return true;
        String[] loc = e.getLine(2).split(" ");
        String[] loc2 = e.getLine(3).split(" ");
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Stop positioner");
            opt.setDescription("set stop position for train");
            parseInt(loc[0]);
            parseInt(loc[1]);
            parseInt(loc[2]);
            if (!e.getLine(3).isEmpty()) {
                parseInt(loc2[0]);
                parseInt(loc2[1]);
                parseInt(loc2[2]);
            }
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            e.getPlayer().sendMessage(ChatColor.RED + "Numbers are not valid!");
            e.setCancelled(true);
        }
        return true;
    }
}