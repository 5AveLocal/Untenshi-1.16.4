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

import static me.fiveave.untenshi.atosign.getLocFromString;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;

class stoppos extends SignAction {

    static void curveRailPosFix(utsvehicle lv, double[] loc) {
        BlockData bs = lv.getSavedworld().getBlockAt((int) (loc[0] - 0.5), (int) loc[1], (int) (loc[2] - 0.5)).getBlockData();
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

    static void stopPosDefault(utsvehicle lv, Location signloc, String l3, String l4) {
        if (lv != null) {
            double[] loc = new double[3];
            getLocFromString(l3, signloc, loc);
            curveRailPosFix(lv, loc);
            // Minecart always has y-offset
            loc[1] += CART_Y_POS_DIFF;
            lv.setStoppos(new Location(lv.getSavedworld(), loc[0], loc[1], loc[2]));
            if (!l4.isEmpty()) {
                double[] loc2 = new double[3];
                getLocFromString(l4, signloc, loc2);
                lv.setStopoutput(new Location(lv.getSavedworld(), loc2[0], loc2[1], loc2[2]));
            }
            lv.setReqstopping(true);
            generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("stoppos_next"));
        }
    }

    static void stopPosAutoOpen(utsvehicle lv, String l4) {
        if (lv != null) {
            boolean autoopen = Boolean.parseBoolean(l4);
            boolean oldautoopen = lv.isStopautoopen();
            if (autoopen != oldautoopen) {
                lv.setStopautoopen(autoopen);
                generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("stoppos_autoopen_" + autoopen));
            }
        }
    }

    static void stopPosMargin(utsvehicle lv, String l4) {
        if (lv != null) {
            double margin = Double.parseDouble(l4);
            double oldmargin = lv.getStopmargin();
            if (margin != oldmargin) {
                lv.setStopmargin(margin);
                generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("stoppos_margin") + " " + ChatColor.GRAY + (int) Math.round(margin * 100) + " cm");
            }
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
            String l3 = cartevent.getLine(2);
            String l4 = cartevent.getLine(3);
            utsvehicle lv = vehicle.get(mg);
            switch (l3) {
                case "autoopen":
                    stopPosAutoOpen(lv, l4);
                    break;
                case "margin":
                    stopPosMargin(lv, l4);
                    break;
                default:
                    stopPosDefault(lv, cartevent.getLocation(), l3, l4);
                    break;
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noSignPerm(e)) return true;
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Stop positioner");
            switch (e.getLine(2)) {
                case "autoopen":
                    opt.setDescription("set if doors will open automatically after train stop at a stop");
                    break;
                case "margin":
                    opt.setDescription("set stop position margin for train");
                    Double.parseDouble(e.getLine(3));
                    break;
                default:
                    opt.setDescription("set stop position for train");
                    getLocFromString(e.getLine(2), e.getLocation(), new double[3]);
                    if (!e.getLine(3).isEmpty()) {
                        getLocFromString(e.getLine(3), e.getLocation(), new double[3]);
                    }
                    break;
            }
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            generalMsg(e.getPlayer(), ChatColor.RED, getLang("signimproper"));
            e.setCancelled(true);
        }
        return true;
    }
}