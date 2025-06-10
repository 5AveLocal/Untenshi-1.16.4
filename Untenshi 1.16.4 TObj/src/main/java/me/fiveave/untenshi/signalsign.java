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
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.signalcmd.isIlClear;
import static me.fiveave.untenshi.speedsign.*;
import static me.fiveave.untenshi.utsvehicle.initVehicle;

class signalsign extends SignAction {

    static void updateSignals(Sign sign, String str) {
        if (sign.getLine(1).equals("signalsign")) {
            sign.setLine(2, str);
            sign.update();
        }
    }

    static void resetSignals(World world, Location[] locs) {
        try {
            // Get resetable signs
            for (Location loc : locs) {
                BlockState bs = world.getBlockAt(loc).getState();
                if (bs instanceof Sign) {
                    Sign resettable = (Sign) world.getBlockAt(loc).getState();
                    // Copy signal and speed from line 4 to line 3
                    updateSignals(resettable, "set " + resettable.getLine(3).split(" ")[1] + " " + resettable.getLine(3).split(" ")[2]);
                }
            }
        } catch (Exception ignored) {
        }
    }

    static String signalName(String warnsi) {
        String retsi = "";
        switch (warnsi) {
            case "g":
                retsi = ChatColor.GREEN + getLang("signal_" + warnsi);
                break;
            case "yg":
            case "y":
                retsi = ChatColor.YELLOW + getLang("signal_" + warnsi);
                break;
            case "yy":
            case "r":
                retsi = ChatColor.RED + getLang("signal_" + warnsi);
                break;
            case "atc":
                retsi = ChatColor.GOLD + "ATC";
                break;
        }
        return retsi;
    }

    // Simplify
    static boolean isSignalType(String s) {
        List<String> list = Arrays.asList("g", "yg", "y", "yy", "r", "atc");
        for (String str : list) {
            if (Objects.equals(s, str)) {
                return true;
            }
        }
        return false;
    }

    static void iLListandOccupiedRemoveShift(utsvehicle lv, Location targetloc, boolean resetrs) {
        ilListRemoveShift(lv, targetloc);
        ilOccupiedRemoveShift(lv, targetloc);
        if (resetrs) {
            rsListRemoveShift(lv);
        }
    }

    static void ilListRemoveShift(utsvehicle lv, Location targetloc) {
        if (lv.getIlposlist() != null) {
            Location[] oldpos = lv.getIlposlist();
            // Interlocking list
            for (int i1 = 0; i1 < oldpos.length; i1++) {
                if (targetloc.equals(oldpos[i1])) {
                    Location[] newpos = new Location[oldpos.length - (1 + i1)];
                    System.arraycopy(oldpos, 1 + i1, newpos, 0, newpos.length);
                    lv.setIlposlist(newpos);
                    break;
                }
            }
        }
    }

    static void ilOccupiedRemoveShift(utsvehicle lv, Location targetloc) {
        if (lv.getIlposoccupied() != null) {
            // Occupied list
            Location[] oldoccupied = lv.getIlposoccupied();
            for (int i2 = 0; i2 < oldoccupied.length; i2++) {
                if (targetloc.equals(oldoccupied[i2])) {
                    Location[] newoccupied = new Location[oldoccupied.length - (1 + i2)];
                    System.arraycopy(oldoccupied, 1 + i2, newoccupied, 0, newoccupied.length);
                    lv.setIlposoccupied(newoccupied);
                    break;
                }
            }
        }
    }

    static void rsListRemoveShift(utsvehicle lv) {
        // Resettable sign list
        if (lv.getRsposlist() != null) {
            lv.setRsposlist(null);
        }
    }


    static signalOrderPtnResult getSignalOrderPtnResult(utsvehicle lv) {
        // Get signal order (ptnlen: length of string, halfptnlen: actual number of signal orders)
        List<String> ptn = signalorder.dataconfig.getStringList("signal." + lv.getSignalorderptn());
        int ptnlen = ptn.size();
        int halfptnlen = ptnlen / 2;
        // ptnsisi: Signal itself, ptnsisp: Signal speed
        String[] ptnsisi = new String[ptnlen];
        int[] ptnsisp = new int[ptnlen];
        // Differentiate ptnsisi and ptnsisp from ptn
        for (int i = 0; i < ptnlen; i++) {
            if (Math.floorMod(i, 2) == 0) {
                ptnsisi[i / 2] = ptn.get(i);
            } else {
                ptnsisp[(i - 1) / 2] = parseInt(ptn.get(i));
            }
        }
        return new signalOrderPtnResult(halfptnlen, ptnsisi, ptnsisp);
    }

    static void deleteOthersRs(utsvehicle lv, Location currentloc) {
        vehicle.keySet().forEach(mg2 -> {
            initVehicle(mg2);
            utsvehicle lv2 = vehicle.get(mg2);
            if (lv2.getRsposlist() != null && lv2 != lv) {
                Location[] oldloc = lv2.getRsposlist();
                Location[] newloc = oldloc;
                for (int i1 = 0; i1 < oldloc.length; i1++) {
                    if (oldloc[i1] != null && currentloc.equals(oldloc[i1])) {
                        newloc = new Location[i1];
                        System.arraycopy(oldloc, 0, newloc, 0, newloc.length);
                        break;
                    }
                }
                lv2.setRsposlist(newloc);
            }
        });
    }

    private static void readIlBook(utsvehicle lv, ItemMeta mat, Location refchestloc) {
        if (mat instanceof BookMeta) {
            BookMeta bk = (BookMeta) mat;
            int pgcount = bk.getPageCount();
            // Test for all pages
            for (int pgno = 1; pgno <= pgcount; pgno++) {
                String str = bk.getPage(pgno);
                String[] trysplitstr = str.split(" ", 3);
                Location[] oldilpos = lv.getIlposlist();
                Location[] newilpos;
                // try statement
                if (trysplitstr[0].equals("try")) {
                    Location fullloc2 = getFullLoc(lv.getSavedworld(), trysplitstr[2]);
                    Chest refchest2 = getChestFromLoc(fullloc2);
                    tryIlChest(lv, refchest2, trysplitstr[1]);
                    // No reading other locations after try statement
                    break;
                }
                Location setloc = getFullLoc(lv.getSavedworld(), str);
                // Anti duplicating causing interlock pattern to be set twice, thus bugging out
                if (lv.getLastilchest() != null && lv.getLastilchest().equals(refchestloc)) {
                    break;
                }
                // Null or not? If null just put new
                if (oldilpos == null || oldilpos.length == 0) {
                    newilpos = new Location[1];
                    newilpos[0] = setloc;
                }
                // If not add new ones in if not duplicated
                else if (!setloc.equals(oldilpos[oldilpos.length - 1])) {
                    int oldilposlen = oldilpos.length;
                    newilpos = new Location[oldilposlen + 1];
                    // Array copy and set new positions
                    System.arraycopy(oldilpos, 0, newilpos, 0, oldilposlen);
                    newilpos[oldilposlen] = getFullLoc(lv.getSavedworld(), str);
                }
                // If duplicated just copy old to new
                else {
                    newilpos = oldilpos;
                }
                // Set ilposlist
                lv.setIlposlist(newilpos);
            }
        }
    }

    private static void tryIlChest(utsvehicle lv, Chest refchest, String tagprefix) {
        boolean found = false;
        // Test for all items in chest
        for (int itemno = 0; itemno < 27; itemno++) {
            ItemMeta mat;
            try {
                mat = Objects.requireNonNull(refchest.getBlockInventory().getItem(itemno)).getItemMeta();
                found = isIlClear(mat, lv.getSavedworld(), false);
                if (found) {
                    String tagname = tagprefix + itemno;
                    // Check for duplicated tags and time (prevent two trains occupying same path)
                    for (MinecartGroup mg2 : vehicle.keySet()) {
                        if (mg2.getProperties().getTags().contains(tagname)) {
                            found = false;
                        }
                    }
                    // Tag not used then assign
                    if (found) {
                        readIlBook(lv, mat, refchest.getLocation());
                        // Add tag for point switches
                        lv.getTrain().getProperties().addTags(tagname);
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // Try again 1 tick later if cannot find
        if (!found) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> tryIlChest(lv, refchest, tagprefix), 1);
        }
    }

    static void signalSignInterlock(utsvehicle lv, String[] l3, String l4) {
        Location fullloc = getFullLoc(lv.getSavedworld(), l4);
        Chest refchest = getChestFromLoc(fullloc);
        if (l3.length == 3 && l3[2].equals("del")) {
            iLListandOccupiedRemoveShift(lv, fullloc, true);
        } else if ((l3.length == 2 || l3.length == 3) && refchest != null) {
            for (int itemno = 0; itemno < 27; itemno++) {
                try {
                    ItemMeta mat = Objects.requireNonNull(refchest.getBlockInventory().getItem(itemno)).getItemMeta();
                    readIlBook(lv, mat, fullloc);
                } catch (Exception ignored) {
                }
            }
            if (l3.length == 3) {
                lv.setIlpriority(parseInt(l3[2]));
            } else {
                lv.setIlpriority(0);
            }
            // Set ilenterqueuetime, signalorder and lastilchest
            lv.setSignalorderptn(l3[1]);
            lv.setIlenterqueuetime(System.currentTimeMillis());
            lv.setLastilchest(fullloc);
        }
    }

    static boolean signalSignWarn(utsvehicle lv, Location eventloc, String l4) {
        String signalmsg;
        if (lv.getAtsforced() != 2 && (lv.getSafetysystype().equals("ats-p") || lv.getSafetysystype().equals("atc"))) {
            Sign warn = getSignFromLoc(getFullLoc(lv.getSavedworld(), l4));
            if (warn != null && warn.getLine(1).equals("signalsign")) {
                // lastsisign and lastsisp are for detecting signal change
                lv.setLastsisign(warn.getLocation());
                String warnsi = warn.getLine(2).split(" ")[1];
                int warnsp = parseInt(warn.getLine(2).split(" ")[2]);
                lv.setLastsisp(warnsp);
                signalmsg = signalName(warnsi);
                if (signalmsg.isEmpty() && eventloc != null) {
                    signImproper(eventloc, lv.getLd());
                    return true;
                }
                // ATC signal and speed limit min value
                if (lv.getSafetysystype().equals("atc")) {
                    warnsp = Math.min(Math.min(lv.getLastsisp(), lv.getLastspsp()), lv.getSpeedlimit());
                }
                String temp2 = warnsp >= maxspeed ? getLang("speedlimit_del") : warnsp + " km/h";
                generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("signal_warn") + " " + signalmsg + ChatColor.GRAY + " " + temp2);
            } else if (eventloc != null) {
                signImproper(eventloc, lv.getLd());
            }
        }
        return false;
    }

    boolean checkType(SignActionEvent e) {
        String[] l3 = e.getLine(2).toLowerCase().split(" ");
        return l3[0].equals("warn") || l3[0].equals("interlock") || (l3[0].equals("set") && isSignalType(l3[1]));
    }

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("signalsign");
    }

    @Override
    // Format: line 3: mode, signal, speed; line 4: coord (warn, sign)
    public void execute(SignActionEvent cartevent) {
        try {
            MinecartGroup mg = cartevent.getGroup();
            utsvehicle lv = vehicle.get(mg);
            Location eventloc = cartevent.getLocation();
            if (lv != null) {
                String[] l3 = cartevent.getLine(2).toLowerCase().split(" ");
                int signalspeed = l3[0].equals("set") ? parseInt(l3[2]) : 0;
                // Main content starts here
                if (limitSpeedIncorrect(null, signalspeed)) {
                    signImproper(eventloc, lv.getLd());
                    return;
                }
                if ((!(l3[0].equals("warn") || l3[0].equals("interlock")) && l3[1].equals("del")) || checkType(cartevent)) {
                    // Put signal speed limit
                    switch (l3[0]) {
                        // Set signal speed limit
                        case "set":
                            // Train enters, add location into resettablesign
                            if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
                                String signalmsg;
                                if (lv.getRsposlist() == null) {
                                    lv.setRsposlist(new Location[0]);
                                }
                                lv.setSignalorderptn(cartevent.getLine(3).split(" ")[0]);
                                // Prevent stepping on same signal causing ATS run
                                if (lv.getRsposlist().length == 0 || !lv.getRsposlist()[0].equals(cartevent.getLocation())) {
                                    // Except red light, signal must get reset first
                                    if (signalspeed != 0) {
                                        Location currentloc = cartevent.getLocation();
                                        // Suzhoushi: If in 3 trains middle train disappears, back train will receive ALL green lights ((rs only, il is issue-free) r, 0 (front), g, 360 (back), g, 360 (back), ...)
                                        // Check if that location exists in any other train, then delete that record
                                        deleteOthersRs(lv, currentloc);
                                    }
                                    // Set values and signal name
                                    lv.setSignallimit(signalspeed);
                                    lv.setSafetysystype(l3[1].equals("atc") ? "atc" : "ats-p");
                                    signalmsg = signalName(l3[1]);
                                    if (signalmsg.isEmpty()) {
                                        signImproper(eventloc, lv.getLd());
                                        break;
                                    }
                                    int shownspeed = signalspeed;
                                    // ATC signal and speed limit min value
                                    if (lv.getSafetysystype().equals("atc")) {
                                        shownspeed = Math.min(lv.getSignallimit(), lv.getSpeedlimit());
                                    }
                                    String temp = shownspeed >= maxspeed ? getLang("speedlimit_del") : shownspeed + " km/h";
                                    if (lv.getLd() != null) {
                                        generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("signal_set") + " " + signalmsg + ChatColor.GRAY + " " + temp);
                                    }
                                    // If red light need to wait signal change, if not then delete variable
                                    if (signalspeed != 0) {
                                        signalOrderPtnResult result = getSignalOrderPtnResult(lv);
                                        // Array copy (move passed signals to the back)
                                        Location[] oldloc = lv.getRsposlist();
                                        Location[] newloc;
                                        // Expand oldloc into newloc by 1 index
                                        newloc = new Location[oldloc.length + 1];
                                        // Length > 0 then copy from n to n + 1
                                        if (oldloc.length != 0) {
                                            System.arraycopy(oldloc, 0, newloc, 1, oldloc.length);
                                        }
                                        newloc[0] = cartevent.getLocation();
                                        // Remove variables
                                        lv.setLastsisign(null);
                                        lv.setLastsisp(maxspeed);
                                        lv.setRsposlist(newloc);
                                        // Set 0th (this) sign with new signal and speed
                                        // settable: Sign to be set
                                        Sign settable;
                                        try {
                                            settable = getSignFromLoc(newloc[0]);
                                            if (settable != null) {
                                                String defaultsi = settable.getLine(3).split(" ")[1];
                                                int defaultsp = parseInt(settable.getLine(3).split(" ")[2]);
                                                // Check if new speed to be set is larger than default, if yes choose default instead
                                                String str = result.ptnsisp[0] > defaultsp ? defaultsi + " " + defaultsp : result.ptnsisi[0] + " " + result.ptnsisp[0];
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> updateSignals(settable, "set " + str), 1);
                                            }
                                        } catch (Exception ignored) {
                                        }
                                        // Make blocked section longer by 1
                                        lv.setRsoccupiedpos(lv.getRsoccupiedpos() + 1);
                                    }
                                    // Prevent non-resettable ATS Run caused by red light but without receiving warning
                                    else if (lv.getLastsisign() == null) {
                                        lv.setLastsisign(cartevent.getLocation());
                                        lv.setLastsisp(signalspeed);
                                    }
                                }
                            }
                            // Train leaves, but in many ways (group leave, destroyed, etc), delete location from resettablesign
                            else if ((cartevent.isAction(SignActionType.GROUP_LEAVE, SignActionType.REDSTONE_OFF) || !cartevent.hasRailedMember() || !cartevent.isPowered()) && lv.getRsposlist() != null && lv.getRsposlist().length > 0) {
                                Location[] oldloc = lv.getRsposlist();
                                signalOrderPtnResult result = getSignalOrderPtnResult(lv);
                                // Make blocked section shorter by 1
                                lv.setRsoccupiedpos(Math.max(lv.getRsoccupiedpos() - 1, 0));
                                for (int i1 = 0; i1 < oldloc.length; i1++) {
                                    // settable: Sign to be set
                                    Sign settable;
                                    try {
                                        settable = getSignFromLoc(oldloc[i1]);
                                        if (settable != null) {
                                            String defaultsi = settable.getLine(3).split(" ")[1];
                                            int defaultsp = parseInt(settable.getLine(3).split(" ")[2]);
                                            // Maximum is result.halfptnlen - 1, cannot exceed (else index not exist and value will be null)
                                            int minno = Math.min(result.halfptnlen - 1, Math.max(0, i1 - lv.getRsoccupiedpos()));
                                            // Check if new speed to be set is larger than default, if yes choose default instead
                                            String str = result.ptnsisp[minno] > defaultsp ? defaultsi + " " + defaultsp : result.ptnsisi[minno] + " " + result.ptnsisp[minno];
                                            Bukkit.getScheduler().runTaskLater(plugin, () -> updateSignals(settable, "set " + str), 1);
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                                // Must put here or else points that are not signals also get cleared wrongly too early
                                // If location is in interlocking list, then remove location and shift list
                                iLListandOccupiedRemoveShift(lv, cartevent.getLocation(), false);
                            }
                            break;
                        // Signal speed limit warn
                        case "warn":
                            if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
                                String l4 = cartevent.getLine(3);
                                if (signalSignWarn(lv, eventloc, l4)) break;
                            }
                            break;
                        case "interlock":
                            if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
                                String l4 = cartevent.getLine(3);
                                signalSignInterlock(lv, l3, l4);
                            }
                            break;
                        default:
                            signImproper(eventloc, lv.getLd());
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noSignPerm(e)) return true;
        Player p = e.getPlayer();
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Signal sign");
            String[] s2 = e.getLine(2).split(" ");
            String[] s3 = e.getLine(3).split(" ");
            // Check signal name
            if (!checkType(e) && !s2[1].equals("del")) {
                p.sendMessage(ChatColor.RED + getLang("signal_typewrong"));
                p.sendMessage(ChatColor.RED + getLang("argwrong"));
                e.setCancelled(true);
            }
            // Check speed conditions
            if (s2[0].equals("set")) {
                int signalspeed = parseInt(s2[2]);
                if (limitSpeedIncorrect(p, signalspeed)) e.setCancelled(true);
            }
            // Check line 4 (coord) is int only
            switch (s2[0]) {
                case "warn":
                    for (String i : s3) {
                        parseInt(i);
                    }
                    opt.setDescription("set signal speed warning for train");
                    break;
                case "interlock":
                    if (s2.length == 3 && s2[2].equals("del")) {
                        opt.setDescription("delete last and previous interlock occupations for train");
                    } else if (s2.length == 3 && parseInt(s2[2]) > 0) {
                        opt.setDescription("set interlocking path for train with priority");
                    } else if (s2.length == 2) {
                        opt.setDescription("set interlocking path for train");
                    } else {
                        e.setCancelled(true);
                    }
                    for (String i : s3) {
                        parseInt(i);
                    }
                    break;
                case "set":
                    if (!isSignalType(s3[1].toLowerCase())) {
                        p.sendMessage(ChatColor.RED + getLang("signal_typewrong"));
                    }
                    int setspeed = parseInt(s3[2]);
                    if (limitSpeedIncorrect(p, setspeed)) e.setCancelled(true);
                    opt.setDescription("set signal speed limit for train");
                    break;
            }
            return opt.handle(p);
        } catch (Exception exception) {
            p.sendMessage(ChatColor.RED + getLang("signimproper"));
            e.setCancelled(true);
        }
        return true;
    }

    static class signalOrderPtnResult {
        public final int halfptnlen;
        public final String[] ptnsisi;
        public final int[] ptnsisp;

        public signalOrderPtnResult(int halfptnlen, String[] ptnsisi, int[] ptnsisp) {
            this.halfptnlen = halfptnlen;
            this.ptnsisi = ptnsisi;
            this.ptnsisp = ptnsisp;
        }
    }
}