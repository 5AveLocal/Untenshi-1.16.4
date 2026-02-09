package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;

import static me.fiveave.untenshi.atosign.getLocFromString;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.speedsign.getFullLoc;
import static me.fiveave.untenshi.speedsign.signImproper;

class drivermsg extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("drivermsg");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            MinecartGroup mg = cartevent.getGroup();
            String l3 = cartevent.getLine(2);
            String l4 = cartevent.getLine(3);
            utsvehicle lv = vehicle.get(mg);
            utsdriver ld = lv.getLd();
            // Only if train has driver
            if (ld != null) {
                Player p = ld.getP();
                Location eventloc = cartevent.getLocation();
                Location loc = getFullLoc(l4, eventloc);
                Block b = cartevent.getWorld().getBlockAt(loc);
                if (b.getState() instanceof CommandBlock) {
                    CommandBlock cb = (CommandBlock) b.getState();
                    String s = cb.getCommand();
                    switch (l3.toLowerCase()) {
                        case "chatcolor":
                            p.sendMessage(UTS_HEAD + ChatColor.translateAlternateColorCodes('&', s));
                            break;
                        case "json":
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " [{\"text\":\"" + UTS_HEAD + "\"}," + s + "]");
                            break;
                        default:
                            signImproper(eventloc, ld);
                            break;
                    }
                }
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noSignPerm(e)) return true;
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Driver messenger");
            opt.setDescription("send a message to the driver");
            getLocFromString(e.getLine(3), e.getLocation(), new double[3]);
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            generalMsg(e.getPlayer(), ChatColor.RED, getLang("signimproper"));
            e.setCancelled(true);
        }
        return true;
    }
}