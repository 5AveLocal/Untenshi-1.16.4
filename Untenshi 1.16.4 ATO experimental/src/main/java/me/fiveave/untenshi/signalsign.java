package me.fiveave.untenshi;

import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.speedsign.getSign;

class signalsign extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("signalsign");
    }

    @Override
    // Format: line 3: mode, signal, speed; line 4: coord (warn, sign)
    public void execute(SignActionEvent cartevent) {
        try {
            //noinspection rawtypes
            for (MinecartMember cart : cartevent.getMembers()) {
                // For each passenger on cart
                //noinspection rawtypes
                CommonEntity cart2 = cart.getEntity();
                //noinspection rawtypes
                List cartpassengers = cart2.getPassengers();
                for (Object cartobj : cartpassengers) {
                    Player p = (Player) cartobj;
                    if (playing.containsKey(p)) {
                        signaltype.putIfAbsent(p, "ats");
                        if (playing.get(p) && cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
                            int signalspeed = 0;
                            if (l1(cartevent).equals("set") || (l1(cartevent).equals("relate") && !l2(cartevent).equals("del"))) {
                                signalspeed = parseInt(l3(cartevent));
                            }
                            // Main content starts here
                            if ((!l1(cartevent).equals("warn") && l2(cartevent).equals("del")) || (signalspeed <= 360 && signalspeed >= 0 && Math.floorMod(signalspeed, 5) == 0 && (checktype(cartevent)))) {
                                String signalmsg = "";
                                // Put signal speed limit
                                switch (l1(cartevent)) {
                                    // Set signal speed limit
                                    case "set":
                                        // [y][x], y for vertical (number of signals passed), x for horizontal (same row needed to be set)
                                        lastresetablesign.putIfAbsent(p, new Location[1][1]);
                                        signalorderptn.put(p, cartevent.getLine(3).split(" ")[0]);
                                        // Prevent stepping on same signal causing ATS run
                                        if (lastresetablesign.get(p)[0][0] == null || !lastresetablesign.get(p)[0][0].equals(cartevent.getLocation())) {
                                            // Prevent non-resettable ATS Run caused by red light but without receiving warning
                                            if (signalspeed == 0) {
                                                lastsisign.putIfAbsent(p, cartevent.getLocation());
                                                lastsisp.putIfAbsent(p, signalspeed);
                                            }
                                            // Check if that location exists in any other train, then delete that record
                                            Location currentloc = cartevent.getLocation();
                                            for (Player p2 : plugin.getServer().getOnlinePlayers()) {
                                                playing.putIfAbsent(p2, false);
                                                if (playing.get(p2)) {
                                                    lastresetablesign.putIfAbsent(p2, new Location[1][1]);
                                                    Location[][] locs = lastresetablesign.get(p2);
                                                    for (int i1 = 0; i1 < locs.length; i1++) {
                                                        for (int i2 = 0; i2 < locs[i1].length; i2++) {
                                                            if (locs[i1][i2] == currentloc) {
                                                                locs[i1][i2] = null;
                                                                lastresetablesign.put(p, locs);
                                                                resetSignals(cartevent.getWorld(), locs[i1]);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            signallimit.put(p, signalspeed);
                                            signaltype.put(p, l2(cartevent).equals("atc") ? "atc" : "ats");
                                            signalmsg = signalName(l2(cartevent), signalmsg);
                                            if (signalmsg.equals("")) {
                                                signimproper(cartevent, p);
                                                break;
                                            }
                                            String temp = signalspeed >= 360 ? getlang("nolimit") : signalspeed + " km/h";
                                            p.sendMessage(utshead + ChatColor.YELLOW + getlang("signalset") + signalmsg + ChatColor.GRAY + " " + temp);
                                            // If red light need to wait signal change, if not then delete variable
                                            if (signalspeed != 0) {
                                                // Get signal order
                                                List<String> ptn = signalorder.dataconfig.getStringList("signal." + signalorderptn.get(p));
                                                int ptnlen = ptn.size();
                                                int halfptnlen = ptnlen / 2;
                                                String[] ptnsisi = new String[ptnlen];
                                                int[] ptnsisp = new int[ptnlen];
                                                for (int i = 0; i < ptnlen; i++) {
                                                    if (Math.floorMod(i, 2) == 0) {
                                                        ptnsisi[i / 2] = ptn.get(i);
                                                    } else {
                                                        ptnsisp[(i - 1) / 2] = parseInt(ptn.get(i));
                                                    }
                                                }
                                                // Get maximum value of y in each x
                                                Location[][] oldloc = lastresetablesign.get(p);
                                                int maxvaly = 0;
                                                for (Location[] locations : oldloc) {
                                                    if (locations.length > maxvaly) {
                                                        maxvaly = locations.length;
                                                    }
                                                }
                                                // Array copy
                                                Location[][] newloc = new Location[halfptnlen][maxvaly];
                                                for (int i1 = 0; i1 < oldloc.length; i1++) {
                                                    if (i1 + 1 < newloc.length) {
                                                        newloc[i1 + 1] = oldloc[i1];
                                                    }
                                                }
                                                newloc[0][0] = cartevent.getLocation();
                                                // Remove variables
                                                lastsisign.remove(p);
                                                lastsisp.remove(p);
                                                // Reset signals if too much (oldloc.length > newloc.length)
                                                if (oldloc.length > newloc.length) {
                                                    for (int i1 = newloc.length + 1; i1 < oldloc.length; i1++) {
                                                        // Get resetable signs
                                                        resetSignals(cartevent.getWorld(), oldloc[i1]);
                                                    }
                                                }
                                                // Set signs with new signal and speed
                                                for (int i1 = 0; i1 < halfptnlen; i1++) {
                                                    for (int i2 = 0; i2 < lastresetablesign.get(p)[lastresetablesign.get(p).length - 1].length; i2++) {
                                                        // setable: Sign to be set
                                                        Sign setable = null;
                                                        try {
                                                            setable = (Sign) cartevent.getWorld().getBlockAt(newloc[i1][i2]).getState();
                                                        } catch (Exception ignored) {
                                                        }
                                                        if (setable != null) {
                                                            String defaultsi = setable.getLine(3).split(" ")[1];
                                                            int defaultsp = parseInt(setable.getLine(3).split(" ")[2]);
                                                            // Check if new speed to be set is larger than default, if yes choose default instead
                                                            String str = ptnsisp[i1] > defaultsp ? defaultsi + " " + defaultsp : ptnsisi[i1] + " " + ptnsisp[i1];
                                                            Sign finalSetable = setable;
                                                            Bukkit.getScheduler().runTaskLater(plugin, () -> updateSignals(finalSetable, "set " + str), 1);

                                                        }
                                                    }
                                                }
                                                lastresetablesign.put(p, newloc);
                                            }
//                                            atoforcebrake.put(p, false);
                                        }
                                        break;
                                    // Signal speed limit warn
                                    case "warn":
                                        if ((!atsbraking.get(p) && signaltype.get(p).equals("ats")) || signaltype.get(p).equals("atc")) {
                                            Sign warn = getSign(cartevent);
                                            if (warn != null && warn.getLine(1).equals("signalsign")) {
                                                // lastsisign and lastsisp are for detecting signal change
                                                lastsisign.put(p, warn.getLocation());
                                                String warnsi = warn.getLine(2).split(" ")[1];
                                                String warnsp = warn.getLine(2).split(" ")[2];
                                                lastsisp.put(p, Integer.valueOf(warnsp));
                                                signalmsg = signalName(warnsi, signalmsg);
                                                if (signalmsg.equals("")) {
                                                    signimproper(cartevent, p);
                                                    break;
                                                }
                                                String temp2 = parseInt(warnsp) >= 360 ? getlang("nolimit") : warnsp + " km/h";
                                                p.sendMessage(utshead + ChatColor.YELLOW + getlang("signalwarn") + signalmsg + ChatColor.GRAY + " " + temp2);
                                            } else {
                                                signimproper(cartevent, p);
                                            }
                                        }
                                        break;
                                    case "relate":
                                        lastresetablesign.putIfAbsent(p, new Location[1][1]);
                                        Sign sign = getSign(cartevent);
                                        if (sign != null && sign.getLine(2).split(" ")[0].equals("set")) {
                                            int reqlen = lastresetablesign.get(p)[0].length;
                                            Location[][] oldloc = lastresetablesign.get(p);
                                            int maxvaly = 0;
                                            for (Location[] locations : oldloc) {
                                                if (locations.length > maxvaly) {
                                                    maxvaly = locations.length;
                                                }
                                            }
                                            Location[][] newloc = oldloc;
                                            // Get record that contains coordinate then delete all records including and before that
                                            int delfrom = -1;
                                            // If delete relations
                                            if (l2(cartevent).equals("del")) {
                                                for (int i1 = 0; i1 < oldloc.length; i1++) {
                                                    if (oldloc[i1][0] != null && (oldloc[i1][0].getBlockX() + " " + oldloc[i1][0].getBlockY() + " " + oldloc[i1][0].getBlockZ()).equals(cartevent.getLine(3))) {
                                                        delfrom = i1;
                                                    }
                                                }
                                                if (delfrom >= 0) {
                                                    for (int i1 = delfrom; i1 < oldloc.length; i1++) {
                                                        resetSignals(cartevent.getWorld(), oldloc[i1]);
                                                    }
                                                    newloc = new Location[delfrom][maxvaly];
                                                    for (int i1 = 0; i1 < oldloc.length; i1++) {
                                                        System.arraycopy(oldloc[i1], 0, newloc[i1], 0, delfrom);
                                                    }
                                                }
                                            } // Else add relations
                                            else {
                                                newloc = new Location[oldloc.length][maxvaly + 1];
                                                if (signalspeed < parseInt(sign.getLine(2).split(" ")[2])) {
                                                    updateSignals(sign, "set " + l2(cartevent) + " " + l3(cartevent));
                                                }
                                                for (int i1 = 0; i1 < oldloc.length; i1++) {
                                                    System.arraycopy(oldloc[i1], 0, newloc[i1], 0, oldloc[i1].length);
                                                }
                                                newloc[0][reqlen] = sign.getLocation();
                                            }
                                            lastresetablesign.put(p, newloc);
                                        } else {
                                            signimproper(cartevent, p);
                                        }
                                        break;
                                    default:
                                        signimproper(cartevent, p);
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void updateSignals(Sign sign, String cartevent) {
        sign.setLine(2, cartevent);
        sign.update();
    }

    static void resetSignals(World world, Location[] locs) {
        try {
            // Get resetable signs
            for (Location loc : locs) {
                Sign resetable = (Sign) world.getBlockAt(loc).getState();
                // Copy signal and speed from line 4 to line 3
                updateSignals(resetable, "set " + resetable.getLine(3).split(" ")[1] + " " + resetable.getLine(3).split(" ")[2]);
            }
        } catch (Exception ignored) {
        }
    }

    static void signimproper(SignActionEvent cartevent, Player p) {
        String s = utshead + ChatColor.RED + getlang("signimproper") + " (" + cartevent.getLocation().getBlockX() + " " + cartevent.getLocation().getBlockY() + " " + cartevent.getLocation().getBlockZ() + ")";
        p.sendMessage(s);
        System.out.println(s);
    }

    static String signalName(String warnsi, String sigmsg) {
        switch (warnsi) {
            case "g":
                sigmsg = ChatColor.GREEN + getlang("signal" + warnsi);
                break;
            case "yg":
            case "y":
                sigmsg = ChatColor.YELLOW + getlang("signal" + warnsi);
                break;
            case "yy":
            case "r":
                sigmsg = ChatColor.RED + getlang("signal" + warnsi);
                break;
            case "atc":
                sigmsg = ChatColor.GOLD + getlang("signal" + warnsi);
                break;
        }
        return sigmsg;
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        if (noperm(e)) return true;
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "Signal sign");
            // Check signal name
            if (!checktype(e) && !l2(e).equals("del")) {
                e.getPlayer().sendMessage(ChatColor.RED + getlang("signaltypewrong"));
                e.getPlayer().sendMessage(ChatColor.RED + getlang("signalargwrong1"));
                e.setCancelled(true);
            }
            // Check if speed can mod 5
            if (l1(e).equals("set") || (l1(e).equals("relate") && !l2(e).equals("del"))) {
                if (parseInt(l3(e)) > 360) {
                    e.getPlayer().sendMessage(ChatColor.RED + getlang("signalmax360"));
                    e.setCancelled(true);
                }
                if (parseInt(l3(e)) < 0) {
                    e.getPlayer().sendMessage(ChatColor.RED + getlang("signalmin0"));
                    e.setCancelled(true);
                }
                if (Math.floorMod(parseInt(l3(e)), 5) != 0) {
                    e.getPlayer().sendMessage(ChatColor.RED + getlang("signaldiv5"));
                    e.setCancelled(true);
                }

            }

            // Check line 4 (coord) is int only
            switch (l1(e)) {
                case "warn":
                    for (String i : e.getLine(3).split(" ")) {
                        parseInt(i);
                    }
                    opt.setDescription("set signal speed warning for train");
                    break;
                case "relate":
                    for (String i : e.getLine(3).split(" ")) {
                        parseInt(i);
                    }
                    if (l2(e).equals("del")) {
                        opt.setDescription("delete signal relations to train");
                    } else {
                        opt.setDescription("add signal relations to train");
                    }
                    break;
                case "set":
                    String[] line4 = e.getLine(3).split(" ");
                    if (!issignaltype(line4[1].toLowerCase())) {
                        e.getPlayer().sendMessage(ChatColor.RED + getlang("signaltypewrong"));
                    }
                    parseInt(line4[2]);
                    if (parseInt(line4[2]) > 360) {
                        e.getPlayer().sendMessage(ChatColor.RED + getlang("signalmax360"));
                        e.setCancelled(true);
                    }
                    if (parseInt(line4[2]) < 0) {
                        e.getPlayer().sendMessage(ChatColor.RED + getlang("signalmin0"));
                        e.setCancelled(true);
                    }
                    if (Math.floorMod(parseInt(line4[2]), 5) != 0) {
                        e.getPlayer().sendMessage(ChatColor.RED + getlang("signaldiv5"));
                        e.setCancelled(true);
                    }
                    opt.setDescription("set signal speed limit for train");
                    break;
            }
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            e.getPlayer().sendMessage(ChatColor.RED + getlang("signimproper"));
            exception.printStackTrace();
            e.setCancelled(true);
        }
        return true;
    }

    // Simplify
    static boolean issignaltype(String s) {
        List<String> list = Arrays.asList("g", "yg", "y", "yy", "r", "atc");
        for (String str : list) {
            if (Objects.equals(s, str)) {
                return true;
            }
        }
        return false;
    }

    boolean checktype(SignActionEvent e) {
        return l1(e).equals("warn") || ((l1(e).equals("set") || l1(e).equals("relate")) && issignaltype(l2(e)));
    }

    // l[n]: "n"th split text in line 3
    String l1(SignActionEvent e) {
        return e.getLine(2).toLowerCase().split(" ")[0];
    }

    static String l2(SignActionEvent e) {
        return e.getLine(2).toLowerCase().split(" ")[1];
    }

    String l3(SignActionEvent e) {
        return e.getLine(2).split(" ")[2];
    }
}