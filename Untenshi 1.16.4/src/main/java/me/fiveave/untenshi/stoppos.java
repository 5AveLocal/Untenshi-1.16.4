package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;

class stoppos extends SignAction {

    static void curveRailPosFix(utsvehicle lv, double[] loc) {
        BlockData bs = lv.getSavedworld().getBlockAt((int) loc[0], (int) loc[1], (int) loc[2]).getBlockData();
        if (bs instanceof Rail) {
            switch (((Rail) bs).getShape()) {
                case NORTH_EAST:
                    loc[0] += 0.25;
                    loc[2] -= 0.25;
                    break;
                case NORTH_WEST:
                    loc[0] -= 0.25;
                    loc[2] -= 0.25;
                    break;
                case SOUTH_EAST:
                    loc[0] += 0.25;
                    loc[2] += 0.25;
                    break;
                case SOUTH_WEST:
                    loc[0] -= 0.25;
                    loc[2] += 0.25;
                    break;
            }
        }
    }

    static void stopPosDefault(utsvehicle lv, String[] l3split, String l4) {
        if (lv != null) {
            String[] l4split = l4.split(" ");
            double[] loc = new double[3];
            int[] loc2 = new int[3];
            for (int a = 0; a <= 2; a++) {
                loc[a] = Double.parseDouble(l3split[a]);
                if (!l4.isEmpty()) {
                    loc2[a] = parseInt(l4split[a]);
                }
            }
            curveRailPosFix(lv, loc);
            loc[0] += 0.5;
            loc[1] += CART_Y_POS_DIFF;
            loc[2] += 0.5;
            lv.setStoppos(new Location(lv.getSavedworld(), loc[0], loc[1], loc[2]));
            if (!l4.isEmpty()) {
                lv.setStopoutput(loc2);
            }
            lv.setReqstopping(true);
            generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("stoppos_next"));
        }
    }

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("stoppos");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            MinecartGroup mg = cartevent.getGroup();
            String[] l3split = cartevent.getLine(2).split(" ");
            String l4 = cartevent.getLine(3);
            utsvehicle lv = vehicle.get(mg);
            stopPosDefault(lv, l3split, l4);
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noSignPerm(e)) return true;
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
            generalMsg(e.getPlayer(), ChatColor.RED, getLang("signimproper"));
            e.setCancelled(true);
        }
        return true;
    }
}