package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.ChatColor;

import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;

class utstrain extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("utstrain");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.hasRailedMember() && cartevent.isPowered()) {
            cartevent.setLevers(vehicle.get(cartevent.getGroup()) != null);
            if (cartevent.isAction(SignActionType.GROUP_LEAVE)) {
                cartevent.setLevers(false);
            }
        } else {
            cartevent.setLevers(false);
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noSignPerm(e)) return true;
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Untenshi Train Detector");
            opt.setDescription("detect if the train is a Untenshi train");
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            generalMsg(e.getPlayer(), ChatColor.RED, getLang("signimproper"));
            e.setCancelled(true);
        }
        return true;
    }
}