package me.fiveave.untenshi;

import com.bergerkiller.bukkit.common.math.Quaternion;
import com.bergerkiller.bukkit.tc.attachments.config.AttachmentModel;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.ato.*;
import static me.fiveave.untenshi.cmds.errorLog;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.events.toB8;
import static me.fiveave.untenshi.events.trainSound;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.signalsign.*;
import static me.fiveave.untenshi.speedsign.*;

class motion {

    static void recursiveClockLv(utsvehicle lv) {
        if (lv.getTrain() != null && !lv.getTrain().isEmpty()) {
            try {
                motionSystem(lv);
                Bukkit.getScheduler().runTaskLater(plugin, () -> recursiveClockLv(lv), TICK_DELAY);
            } catch (Exception e) {
                errorLog(e, "motion.recursiveClockLv");
                restoreInitLv(lv);
            }
        } else {
            restoreInitLv(lv);
        }
    }

    static void recursiveClockLd(utsdriver ld) {
        if (ld.isPlaying() && ld.getP().isInsideVehicle() && MinecartGroup.get(ld.getP().getVehicle()).equals(ld.getLv().getTrain())) {
            try {
                driverSystem(ld);
                Bukkit.getScheduler().runTaskLater(plugin, () -> recursiveClockLd(ld), TICK_DELAY);
            } catch (Exception e) {
                errorLog(e, "motion.recursiveClockLd");
                restoreInitLd(ld);
            }
        } else if (ld.isFrozen()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> recursiveClockLd(ld), TICK_DELAY);
        } else if (!ld.getP().isInsideVehicle() || !MinecartGroup.get(ld.getP().getVehicle()).equals(ld.getLv().getTrain())) { // Not in (same) vehicle
            restoreInitLd(ld);
        }
    }

    static void motionSystem(utsvehicle lv) {
        // From Config
        double oldspeed = lv.getSpeed();
        boolean stationstop = plugin.getConfig().getBoolean("stationsignstop");
        // Init train
        MinecartGroup mg = lv.getTrain();
        TrainProperties tprop = mg.getProperties();
        // Rounding
        DecimalFormat df3 = new DecimalFormat("#.###");
        DecimalFormat df0 = new DecimalFormat("#");
        df3.setRoundingMode(RoundingMode.CEILING);
        df0.setRoundingMode(RoundingMode.UP);
        // Electric current
        double ecnow = lv.getCurrent();
        // Set current for current mascon
        double ectarget = getCurrentFromNotch(lv.getMascon());
        double currentpertick = lv.getCurrentpertick();
        // Set real current
        if (ectarget < ecnow) {
            lv.setCurrent((ecnow - ectarget) > currentpertick ? ecnow - currentpertick : ectarget);
            trainSound(lv, "accel_off");
        } else if (ectarget > ecnow) {
            lv.setCurrent((ectarget - ecnow) > currentpertick ? ecnow + currentpertick : ectarget);
            trainSound(lv, "accel_on");
        }
        // Brake cylinder pressure
        double bcpnow = lv.getBcpressure();
        // Set pressure for current brake
        double bcptarget = getPressureFromBrake(lv.getBrake());
        double bcppertick = lv.getBcppertick();
        double ebbcppertick = lv.getEbbcppertick();
        // Set real pressure
        if (bcptarget < bcpnow) {
            lv.setBcpressure((bcpnow - bcptarget) > bcppertick ? bcpnow - bcppertick : bcptarget);
            trainSound(lv, "brake_release");
        } else if (bcptarget > bcpnow) {
            double selbcppertick = (lv.getBrake() == 9 ? ebbcppertick : bcppertick);
            lv.setBcpressure((bcptarget - bcpnow) > selbcppertick ? bcpnow + selbcppertick : bcptarget);
            trainSound(lv, "brake_apply");
        }
        // Slope speed adjust
        HeadAndTailResult result = getHeadAndTailResult(mg);
        double slopeaccel = getSlopeAccel(result.headLoc, result.tailLoc);
        // Accel and decel
        double accelnow = accelSwitch(lv, lv.getSpeed(), (int) (getNotchFromCurrent(ecnow)));
        double decelnow = decelSwitch(lv, lv.getSpeed(), slopeaccel);
        if (lv.getDooropen() == 0) {
            // If door is closed
            lv.setSpeed(lv.getSpeed() //
                    + accelnow * ONE_TICK_IN_S // Acceleration
                    - decelnow * ONE_TICK_IN_S); // Deceleration (speed drop included)
            // Prevent negative speed
            if (lv.getSpeed() < 0) {
                lv.setSpeed(0);
            }
            // Prevent speed over 360 km/h (TC Limit)
            if (lv.getSpeed() > 360) {
                lv.setSpeed(360);
            }
        } else {
            // If door is open
            lv.setSpeed(0);
            if (lv.getMascon() > 0) {
                lv.setMascon(0);
            }
        }
        // Cancel TC motion-related sign actions
        if (!stationstop) mg.getActions().clear();
        // Shock when stopping
        String shock = lv.getSpeed() == 0 && lv.getSpeed() < oldspeed ? " " + ChatColor.GRAY + String.format("%.2f km/h/s", decelnow) : "";
        // Combine properties and action bar
        double blockpertick = 0;
        try {
            blockpertick = Double.parseDouble(df3.format(lv.getSpeed() / 72));
        } catch (NumberFormatException ignored) {
        }
        tprop.setSpeedLimit(blockpertick);
        mg.setForwardForce(blockpertick);
        mg.setProperties(tprop);
        // Combine properties and action bar
        doorLogic(lv, tprop);
        // Get signal update when warn (if signal speed isn't same)
        catchSignalUpdate(lv);
        // Interlocking stuff
        interlocking(lv);
        // Types of speeding
        boolean isoverspeed0 = lv.getSpeed() > minSpeedLimit(lv);
        boolean isoverspeed3 = lv.getSpeed() > minSpeedLimit(lv) + 3 || lv.getSignallimit() == 0;
        // ATS-P or ATC
        safetySys(lv, mg, isoverspeed0, isoverspeed3);
        // ATO (Must be placed after actions)
        atoSys(lv, mg);
        // Stop action
        if (lv.getStopactionpos() != null) {
            Chest refchest = getChestFromLoc(lv.getStopactionpos());
            // Train stopping at station
            if (refchest != null) {
                // Only 1 book allowed
                ItemMeta mat = Objects.requireNonNull(refchest.getBlockInventory().getItem(0)).getItemMeta();
                stopActionClock(lv, refchest, mat, 0, 0, lv.isDoordiropen(), lv.isDoorconfirm(), true);
                lv.setStopactionpos(null);
            }
        }
        // Stop position
        stopPos(lv, shock);
    }

    static void driverSystem(utsdriver ld) {
        utsvehicle lv = ld.getLv();
        // Rounding
        DecimalFormat df0 = new DecimalFormat("#");
        df0.setRoundingMode(RoundingMode.HALF_EVEN);
        // Combine properties and action bar
        String doortxt = doorText(lv);
        // Display speed
        String speedcolor = "";
        if (lv.getAtsping() > 0 || lv.getAtsforced() == 2) {
            speedcolor += ChatColor.RED;
        } else if (lv.isAtspnear()) {
            speedcolor += ChatColor.GOLD;
        } else {
            speedcolor += ChatColor.WHITE;
        }
        String formattedSpeed = speedcolor + df0.format(lv.getSpeed());
        String displaySpeed = add3ZeroPadding(formattedSpeed);
        boolean showstoppos = false;
        int stopdistcm = 0;
        if (lv.getStoppos() != null) {
            StopPosResult spresult = getStopPosResult(lv);
            stopdistcm = spresult.stopdistcm;
            if (spresult.stopdist <= 5 && lv.getSpeed() != 0) showstoppos = true;
        }
        // Action bar
        String actionbarmsg = getCtrlText(lv) + ChatColor.WHITE + " | " + displaySpeed + ChatColor.GRAY + " km/h" // speed
                + ChatColor.WHITE + " | " + add3ZeroPadding(ChatColor.WHITE + String.valueOf(Math.round(lv.getCurrent()))) + ChatColor.GRAY + " A" // current
                + ChatColor.WHITE + " | " + add3ZeroPadding(ChatColor.WHITE + String.valueOf(Math.round(lv.getBcpressure()))) + ChatColor.GRAY + " kPa" // brake cylinder pressure
                + ChatColor.WHITE + " | " + add3ZeroPadding(ChatColor.WHITE + String.valueOf(ld.getPoints())) + ChatColor.GRAY + " pts" // points
                + ChatColor.WHITE + " | " + (showstoppos ? add3ZeroPadding(ChatColor.WHITE + String.valueOf(stopdistcm)) + ChatColor.GRAY + " cm" : doortxt); // stop position or door status
        ld.getP().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionbarmsg));
        // Catch point <= 0 and end game
        if (noFreemodeOrATO(ld) && ld.getPoints() <= 0) {
            ebUntilRestoreInit(ld, getLang("nopoints"));
        }
    }

    private static void interlocking(utsvehicle lv) {
        Location[] oldposlist = lv.getIlposlist();
        if (oldposlist == null || oldposlist.length == 0) {
            return;
        }
        // Check conditions to change signal
        int furthestoccupied = oldposlist.length - 1;
        boolean ispriority = true;
        // Prevent front train being blocked due to train at back due to lack of iloccupied
        // Ignore blocked status until first signal is found,
        // no worries for direct collision as if rs exists, train will be blocked;
        // if rs does not exist (by interlock del), there must be another location
        int searchstart = 0;
        for (int i = 0; i < oldposlist.length; i++) {
            if (isLocOfSign(oldposlist[i])) {
                searchstart = i;
                break;
            }
        }
        // mg2: other trains
        for (MinecartGroup mg2 : vehicle.keySet()) {
            if (!mg2.equals(lv.getTrain()) && vehicle.get(mg2) != null) {
                utsvehicle lv2 = vehicle.get(mg2);
                // Check resettable sign of other trains
                Location[] rssign2locs = lv2.getRsposlist();
                if (rssign2locs != null) {
                    signalOrderPtnResult result2 = getSignalOrderPtnResult(lv2);
                    // Check for each location
                    for (int i = oldposlist.length - 1; i >= 0; i--) {
                        for (int j = 0; j < rssign2locs.length; j++) {
                            Location location = rssign2locs[j];
                            // Maximum is result.halfptnlen - 1, cannot exceed (else index not exist and value will be null)
                            int minno = Math.min(result2.halfptnlen - 1, Math.max(0, j - lv2.getRsoccupiedpos()));
                            // Resettable sign signal of lv2 is supposed to be 0 km/h by resettable sign
                            if (oldposlist[i].equals(location) && i < furthestoccupied && result2.ptnsisp[minno] == 0) {
                                furthestoccupied = i;
                                break;
                            }
                        }
                    }
                }
                // Check occupied interlocking path of other trains
                Location[] il2posoccupied = lv2.getIlposoccupied();
                if (il2posoccupied != null) {
                    // Check for each location
                    for (int i = oldposlist.length - 1; i >= 0; i--) {
                        for (Location location : il2posoccupied) {
                            // Occupied by interlocking route
                            if (oldposlist[i].equals(location) && i < furthestoccupied) {
                                furthestoccupied = i;
                                break;
                            }
                        }
                    }
                }
                // Check for priority
                // Fighting with other's unoccupied interlocking route
                Location[] il2poslist = lv2.getIlposlist();
                if (il2poslist != null) {
                    // Find location for start of blocked section, -1 means none
                    int blocked = -1;
                    // Find blocked signal
                    for (int i = searchstart; i < oldposlist.length; i++) {
                        if (blocked != -1) {
                            break;
                        }
                        // Check for each location
                        for (int j = 0; j < il2poslist.length - 1; j++) {
                            // Occupied by interlocking list
                            if (oldposlist[i].equals(il2poslist[j])) {
                                blocked = i;
                                break;
                            }
                        }
                    }
                    if (blocked != -1) {
                        // Sign closer to this train
                        int firstsign = 0;
                        for (int i = 0; i <= blocked; i++) {
                            firstsign = i;
                            // If sign is found
                            if (isLocOfSign(oldposlist[i])) {
                                break;
                            }
                        }
                        // Sign closer to part being blocked
                        int lastsign = blocked;
                        for (int i = blocked; i >= 0; i--) {
                            lastsign = i;
                            // If sign is found
                            if (isLocOfSign(oldposlist[i])) {
                                break;
                            }
                        }
                        // firstsign == lastsign means the front sign of train is last before blocked section
                        // Check priority
                        if (firstsign == lastsign && lv.getIlenterqueuetime() > lv2.getIlenterqueuetime() && lv.getIlpriority() <= lv2.getIlpriority()) {
                            ispriority = false;
                            break;
                        }
                    }
                }
            }
        }
        // Is first priority? If yes then continue, if no then wait
        if (ispriority) {
            // Occupy and set signals when ok
            signalOrderPtnResult result = getSignalOrderPtnResult(lv);
            int orderno = 0;
            // Set signs with new signal and speed
            // Prevent repeated updating
            if (lv.getIlposoccupied() == null || lv.getIlposoccupied().length < furthestoccupied + 1) {
                for (int signno = furthestoccupied; signno >= 0; signno--) {
                    // settable: Sign to be set
                    Sign settable = getSignFromLoc(oldposlist[signno]);
                    if (settable != null) {
                        try {
                            String[] settablesplit = settable.getLine(2).split(" ");
                            String currentsi = settablesplit[1];
                            int currentsp = parseInt(settablesplit[2]);
                            // Check if new speed to be set is lower than current, if yes choose current instead
                            String sistr = result.ptnsisp[orderno] < currentsp ? currentsi : result.ptnsisi[orderno];
                            int sp = Math.max(result.ptnsisp[orderno], currentsp);
                            // Update only if changed
                            if (sp != currentsp) {
                                updateSignals(settable, "set " + sistr + " " + sp);
                            }
                            // Orderno must not exceed halfptnlen
                            if (orderno + 1 < result.halfptnlen) {
                                orderno++;
                            }
                            // If next signal is lower than this
                            if (result.ptnsisp[orderno] <= sp) {
                                // For next orderno, must be higher than this
                                for (int i = orderno; i < result.halfptnlen; i++) {
                                    if (result.ptnsisp[i] >= sp) {
                                        orderno = i;
                                        break;
                                    }
                                }
                            }
                        } catch (IndexOutOfBoundsException | NumberFormatException e) {
                            signImproper(settable.getLocation(), lv.getLd());
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            // Set as occupied
            Location[] newiloccupied = new Location[furthestoccupied];
            // Put in ilposoccupied
            System.arraycopy(oldposlist, 0, newiloccupied, 0, furthestoccupied);
            lv.setIlposoccupied(newiloccupied);
            // Delete other's resettable sign (check all locations to prevent bug)
            Arrays.stream(newiloccupied).forEach(eachloc -> deleteOthersRs(lv, eachloc));
        }
    }

    private static String getCtrlText(utsvehicle lv) {
        String ctrltext, braketext;
        int mascon = lv.getMascon();
        int brake = lv.getBrake();
        if (brake == 9) {
            braketext = ChatColor.DARK_RED + "EB";
        } else if (brake >= 1) {
            braketext = ChatColor.RED + "B" + brake;
        } else {
            braketext = ChatColor.WHITE + "N";
        }
        if (mascon == 0) {
            ctrltext = ChatColor.WHITE + "N";
        } else {
            ctrltext = ChatColor.GREEN + "P" + mascon;
        }
        ctrltext = !lv.isTwohandled() ? (brake == 0 ? ctrltext : braketext) : ctrltext + ChatColor.WHITE + " | " + braketext;
        return ctrltext;
    }

    private static void stopPos(utsvehicle lv, String shock) {
        if (lv.isReqstopping()) {
            // Get stop location
            StopPosResult spresult = getStopPosResult(lv);
            // Start Overrun (prevent escaping overrun over 144 km/h)
            if (!lv.isOverrun() && (spresult.stopdist - ONE_TICK_IN_S * speed1s(lv) < 0 || spresult.stopdist < 1)) {
                lv.setOverrun(true);
            }
            // Rewards and penalties
            // In station EB
            if (lv.getMascon() == -9 && !lv.isStaeb() && !plugin.getConfig().getBoolean("allowebstop")) {
                lv.setStaeb(true);
            }
            // In station accel
            if (lv.getMascon() >= 1 && !lv.isStaaccel() && !lv.isFixstoppos() && !plugin.getConfig().getBoolean("allowreaccel")) {
                lv.setStaaccel(true);
            }
            // Stop positions
            // <= 1 m
            if (spresult.stopdist <= 1.00 && lv.getSpeed() == 0) {
                // 25 cm
                if (spresult.stopdist <= 0.25) {
                    showStopPos(lv, "stoppos_perfect", spresult.stopdistcm, shock, 10);
                }
                // 50 cm
                else if (spresult.stopdist <= 0.50) {
                    showStopPos(lv, "stoppos_great", spresult.stopdistcm, shock, 5);
                }
                // 1 m
                else {
                    showStopPos(lv, "stoppos_good", spresult.stopdistcm, shock, 3);
                }
                stopProcedure(lv);
            }
            // < 50 m
            else if (spresult.stopdist < 50 && lv.getSpeed() == 0 && lv.isOverrun() && !lv.isFixstoppos()) {
                lv.setFixstoppos(true);
                lv.setStaaccel(false);
                if (noFreemodeOrATO(lv.getLd())) {
                    pointCounter(lv.getLd(), ChatColor.YELLOW, getLang("stoppos_over") + " ", Math.toIntExact(-Math.round(spresult.stopdist)), shock);
                } else {
                    generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("stoppos_over") + " " + ChatColor.RED + Math.round(spresult.stopdist) + " m" + shock);
                }
            }
            // Cho-heta-dane!
            else if (spresult.stopdist >= 50 && lv.isOverrun()) {
                if (noFreemodeOrATO(lv.getLd())) {
                    ebUntilRestoreInit(lv.getLd(), getLang("stoppos_seriousover"));
                } else {
                    lv.setReqstopping(false);
                    lv.setOverrun(false);
                    lv.setStopactionpos(null);
                }
            }
        }
    }

    static StopPosResult getStopPosResult(utsvehicle lv) {
        Location stoppos = lv.getStoppos();
        Location cartactualpos = getDriverseatActualPos(lv);
        // Add vector for stop position offset
        Vector cartoffsetvector = lv.getDriverseat().getOrientationForward().multiply(lv.getStopposoffset());
        double stopdist = distFormula(stoppos, cartactualpos.add(cartoffsetvector));
        int stopdistcm = (int) Math.round(stopdist * 100);
        return new StopPosResult(stopdist, stopdistcm);
    }

    private static void ebUntilRestoreInit(utsdriver ld, String s) {
        if (ld != null) {
            if (ld.getLv().getAtsforced() != 2) {
                generalMsg(ld.getP(), ChatColor.RED, s);
            }
            ld.getLv().setAtsforced(2);
            ld.getLv().setBrake(9);
            ld.getLv().setMascon(0);
            if (ld.getLv().getSpeed() == 0) {
                restoreInitLd(ld);
            }
        }
    }

    private static void doorLogic(utsvehicle lv, TrainProperties tprop) {
        // Door (enter and exit train, open = 1, close = 0)
        if (lv.isDoordiropen() && lv.getDooropen() < 1) {
            double openvalue = lv.getDooropen() + lv.getDooropenspeed() / TICKS_IN_1_S;
            lv.setDooropen(openvalue > 1 ? 1 : openvalue);
        } else if (!lv.isDoordiropen() && lv.getDooropen() > 0) {
            double closevalue = lv.getDooropen() - lv.getDoorclosespeed() / TICKS_IN_1_S;
            lv.setDooropen(closevalue < 0 ? 0 : closevalue);
        }
        if (!lv.isDoorconfirm() && (lv.getDooropen() == 0 || lv.getDooropen() == 1)) {
            lv.setDoorconfirm(true);
        }
        // Open / close at half
        boolean canenter = tprop.getPlayersEnter();
        boolean canexit = tprop.getPlayersExit();
        boolean opencondition = lv.getDooropen() > 0.5;
        if (opencondition != canenter || opencondition != canexit) {
            tprop.setPlayersEnter(opencondition);
            tprop.setPlayersExit(opencondition);
        }
    }

    private static String doorText(utsvehicle lv) {
        // Door text
        String door = lv.isDoordiropen() ? ChatColor.GREEN + "<|>" : ChatColor.RED + ">|<";
        int openpercent = (int) Math.round(lv.getDooropen() * 100);
        return lv.getAtostoptime() != -1 && lv.isDoordiropen() ? ChatColor.GOLD + "..." + lv.getAtostoptime() : door + " " + add3ZeroPadding(ChatColor.WHITE + String.valueOf(openpercent)) + "%";
    }

    private static void catchSignalUpdate(utsvehicle lv) {
        Location signalloc = lv.getLastsisign();
        if (signalloc != null && lv.getLastsisp() != MAX_SPEED) {
            Sign warnsign = (Sign) lv.getSavedworld().getBlockAt(signalloc).getState();
            String warnsi = warnsign.getLine(2).split(" ")[1];
            int warnsp = Integer.parseInt(warnsign.getLine(2).split(" ")[2]);
            // Detect difference (sign speed now != saved sign speed)
            if (warnsp != lv.getLastsisp()) {
                int oldsignallimit = lv.getLastsisp();
                lv.setLastsisp(warnsp);
                String signalmsg = signalName(warnsi);
                // If red light
                if (lv.getAtsforced() == 2 && lv.getSignallimit() == 0) {
                    // Update signal limit and resettable sign
                    lv.setSignallimit(warnsp);
                    shiftRs(lv, signalloc);
                }
                if (lv.getSafetysystype().equals("atc")) {
                    warnsp = Math.min(warnsp, lv.getSpeedlimit());
                }
                String speedlimittxt = warnsp >= MAX_SPEED ? getLang("speedlimit_del") : warnsp + " km/h";
                if (!lv.getSafetysystype().equals("atc") || warnsp != Math.min(oldsignallimit, lv.getSpeedlimit())) {
                    generalMsg(lv.getLd(), ChatColor.YELLOW, getLang("signal_change") + " " + signalmsg + ChatColor.GRAY + " " + speedlimittxt);
                }
            }
        }
    }

    private static void safetySys(utsvehicle lv, MinecartGroup mg, boolean isoverspeed0, boolean isoverspeed3) {
        double lowerSpeed = minSpeedLimit(lv);
        // 0.0625 from result of getting mg.head() y-location
        HeadAndTailResult result = getHeadAndTailResult(mg);
        double slopeaccel = 0;
        double slopeaccelsi = 0;
        double slopeaccelsp = 0;
        double signaldist = Double.MAX_VALUE;
        double signaldistdiff = Double.MAX_VALUE;
        double speeddist = Double.MAX_VALUE;
        double speeddistdiff = Double.MAX_VALUE;
        double reqsidist;
        double reqspdist;
        double distnow = Double.MAX_VALUE;
        // TC forced stop (e.g. wait distance)
        if (lv.getAtsping() == 0 && lv.getAtsforced() != -1 && (mg.isObstacleAhead(Math.max(mg.getProperties().getWaitDistance(), 0), true, false) || mg.isObstacleAhead(0.001, false, true))) {
            lv.setAtsping(2);
            lv.setAtsforced(-1);
            lv.setBrake(9);
            lv.setMascon(0);
            generalMsg(lv.getLd(), ChatColor.RED, getLang("tcblocking"));
        }
        if (lv.getAtsforced() == -1 && lv.getAtsping() > 0 && lv.getMascon() == -9) {
            lv.setSpeed(Math.max(lv.getSpeed() - lv.getEbdecel() * ONE_TICK_IN_S * 45 / 7, 0));
        }
        // If no obstacle need braking in 2s then release
        if (lv.getAtsforced() == -1 && !mg.isObstacleAhead(mg.getProperties().getWaitDistance() + getThinkingDistance(lv, lv.getSpeed(), lowerSpeed, 8, slopeaccel, 0) * 2, true, true)) {
            lv.setAtsforced(0);
        }
        // Find either signal or speed limit distance, figure out which has the greatest priority (distnow - reqdist is the smallest value)
        if (lv.getLastsisign() != null && lv.getLastsisp() != MAX_SPEED) {
            Location actualSiRefPos = getActualRefPos(lv.getLastsisign(), mg.getWorld());
            slopeaccelsi = getSlopeAccel(actualSiRefPos, result.tailLoc);
            reqsidist = getSingleReqdist(lv, lv.getSpeed(), lv.getLastsisp(), 6, slopeaccelsi, 0);
            signaldist = distFormula(actualSiRefPos, result.headLoc);
            signaldistdiff = signaldist - reqsidist;
        }
        if (lv.getLastspsign() != null && lv.getLastspsp() != MAX_SPEED) {
            Location actualSpRefPos = getActualRefPos(lv.getLastspsign(), mg.getWorld());
            slopeaccelsp = getSlopeAccel(actualSpRefPos, result.tailLoc);
            reqspdist = getSingleReqdist(lv, lv.getSpeed(), lv.getLastspsp(), 6, slopeaccelsp, 0);
            speeddist = distFormula(actualSpRefPos, result.headLoc);
            speeddistdiff = speeddist - reqspdist;
        }
        double priority = Math.min(signaldistdiff, speeddistdiff);
        if (lv.getLastsisign() != null && lv.getLastsisp() != MAX_SPEED && priority == signaldistdiff) {
            lowerSpeed = lv.getLastsisp();
            distnow = signaldist;
            slopeaccel = slopeaccelsi;
        }
        if (lv.getLastspsign() != null && lv.getLastspsp() != MAX_SPEED && priority == speeddistdiff) {
            lowerSpeed = lv.getLastspsp();
            distnow = speeddist;
            slopeaccel = slopeaccelsp;
        }
        // Get brake distance (reqdist)
        double[] reqdist = new double[10];
        getAllReqdist(lv, lv.getSpeed(), lowerSpeed, reqdist, slopeaccel, 0);
        // Actual controlling part
        // Check if next is red light
        boolean nextredlight = lv.getLastsisp() == 0 && priority == signaldistdiff;
        // tempdist is for anti-ATS-run, stop at 1 m before 0 km/h signal
        double tempdist = nextredlight ? Math.max(distnow - 1, 0) : distnow;
        // Find minimum brake needed
        int reqbrake = 9;
        for (int b = 8; b >= 0; b--) {
            if (tempdist >= reqdist[b]) {
                reqbrake = b;
            }
        }
        // Pattern run
        if (reqbrake >= 8 && lv.getSpeed() > lowerSpeed + 3 || isoverspeed3) {
            // Or SPAD (0 km/h signal) EB
            if ((reqbrake == 9 || lv.getSignallimit() == 0) && lv.getAtsping() != 2) {
                lv.setBrake(9);
                lv.setMascon(0);
                lv.setAtsping(2);
                pointCounter(lv.getLd(), ChatColor.RED, lv.getSafetysystype().toUpperCase() + " " + getLang("p_eb") + " ", -5, "");
            } else if (lv.getAtsping() == 0) {
                toB8(lv);
                lv.setAtsping(1);
                pointCounter(lv.getLd(), ChatColor.RED, lv.getSafetysystype().toUpperCase() + " " + getLang("p_b8") + " ", -5, "");
            }
        } else if (lv.getSpeed() <= lowerSpeed + 3 && !isoverspeed0 && lv.getAtsforced() != 2 && lv.getAtsforced() != -1) {
            lv.setAtsping(0);
        }
        // Pattern near
        boolean pnear = (tempdist < reqdist[8] + speed1s(lv) * 5 && lv.getSpeed() > lowerSpeed) || isoverspeed0;
        if (!lv.isAtspnear() && pnear) {
            generalMsg(lv.getLd(), ChatColor.GOLD, lv.getSafetysystype().toUpperCase() + " " + getLang("p_near"));
        }
        lv.setAtspnear(pnear);
    }

    static void getAllReqdist(utsvehicle lv, double upperSpeed, double lowerSpeed, double[] reqdist, double slopeaccel, double extra) {
        for (int a = 0; a <= 9; a++) {
            reqdist[a] = getSingleReqdist(lv, upperSpeed, lowerSpeed, a, slopeaccel, extra);
        }
    }

    static double getSingleReqdist(utsvehicle lv, double upperSpeed, double lowerSpeed, int a, double slopeaccel, double extra) {
        double decel = lv.getDecel();
        double ebdecel = lv.getEbdecel();
        double speeddrop = lv.getSpeeddrop();
        int[] speedsteps = lv.getSpeedsteps();
        switch (a) {
            case 9:
                double afterBrakeInitSpeed9 = getSpeedAfterBrakeInit(lv, upperSpeed, lowerSpeed, 9, slopeaccel);
                double brakeInitDistance9 = getThinkingDistance(lv, upperSpeed, lowerSpeed, 9, slopeaccel, extra);
                // rate = 7 because no multiplier for avgRangeDecel
                double afterInitDistance9 = getReqdist(afterBrakeInitSpeed9, lowerSpeed, avgRangeDecel(ebdecel, afterBrakeInitSpeed9, lowerSpeed, 7, speedsteps), slopeaccel, speeddrop);
                return brakeInitDistance9 + afterInitDistance9;
            case 0:
                // Get speed drop distance
                return getReqdist(upperSpeed, lowerSpeed, speeddrop, slopeaccel, speeddrop);
            default:
                double afterBrakeInitSpeed = getSpeedAfterBrakeInit(lv, upperSpeed, lowerSpeed, a, slopeaccel);
                double brakeInitDistance = getThinkingDistance(lv, upperSpeed, lowerSpeed, a, slopeaccel, extra);
                double afterInitDistance = getReqdist(afterBrakeInitSpeed, lowerSpeed, avgRangeDecel(decel, afterBrakeInitSpeed, lowerSpeed, a + 1, speedsteps), slopeaccel, speeddrop);
                return brakeInitDistance + afterInitDistance;
        }
    }

    static double getReqdist(double upperSpeed, double lowerSpeed, double decel, double slopeaccel, double speeddrop) {
        // Does not consider additional decel upslope, does not consider net deceleration smaller than speeddrop
        return Math.max((Math.pow(upperSpeed, 2) - Math.pow(lowerSpeed, 2)) / (7.2 * Math.max(decel - slopeaccel, speeddrop)), 0);
    }

    static double getSpeedAfterBrakeInit(utsvehicle lv, double upperSpeed, double lowerSpeed, int targetBrake, double slopeaccel) {
        // Prediction tested to be
        // down, flat: too LOW, up: too HIGH
        double speed = upperSpeed;
        double bcp = lv.getBcpressure();
        double bcptarget = getPressureFromBrake(targetBrake);
        if (upperSpeed > lowerSpeed && bcp < bcptarget) {
            AfterBrakeInitResult result = getAfterBrakeInitResult(lv, upperSpeed, slopeaccel, bcp, bcptarget);
            speed -= result.avgdecel * result.realt; // result
        }
        return speed;
    }

    static double getThinkingDistance(utsvehicle lv, double upperSpeed, double lowerSpeed, int targetBrake, double slopeaccel, double extra) {
        // Prediction tested to be
        // down, flat: too LONG, up: too SHORT
        // Prevent unable to accel just because near next target, but no need to brake or to neutral
        if (upperSpeed > lowerSpeed) {
            double bcp = lv.getBcpressure();
            double bcptarget = getPressureFromBrake(targetBrake);
            double sumdist = 0;
            if (bcp < bcptarget) {
                AfterBrakeInitResult result = getAfterBrakeInitResult(lv, upperSpeed, slopeaccel, bcp, bcptarget);
                sumdist = (upperSpeed * result.realt - result.avgdecel * Math.pow(result.realt, 2) / 2) / 3.6; // get distance from basic decel distance formula, v = u*t+1/2*a*t^2, and speed to SI units
            }
            // Extra tick for action delay + slope acceleration considered, prevent negative distance
            return Math.max(0, Math.max(0, sumdist) + (upperSpeed + slopeaccel) / 3.6 * extra);
        } else {
            // Thinking distance not required
            return 0;
        }
    }

    static double getSpeedAfterPotentialAccel(utsvehicle lv, double currentSpeed, double slopeaccel) {
        double current = Math.max(0, lv.getCurrent());
        // 1 tick delay for compensation for action delay
        double speed = currentSpeed + (accelSwitch(lv, currentSpeed, (int) (getNotchFromCurrent(current))) + slopeaccel) * ONE_TICK_IN_S;
        // Anti out-of-range causing GIGO
        // Use while loop as using formula is tedious
        while (current > 0) {
            speed += (accelSwitch(lv, speed, (int) (getNotchFromCurrent(current))) + slopeaccel) * ONE_TICK_IN_S;
            current -= lv.getCurrentpertick();
        }
        return speed;
    }

    static double getCurrentFromNotch(int a) {
        return a * 480.0 / 9;
    }

    static double getNotchFromCurrent(double current) {
        return current * 9 / 480;
    }

    static double getPressureFromBrake(int b) {
        return b * 480.0 / 9;
    }

    static double getBrakeFromPressure(double bcp) {
        return bcp * 9 / 480;
    }

    static double speed1s(utsvehicle lv) {
        return lv.getSpeed() / 3.6;
    }

    static double getSlopeAccel(Location endpt, Location beginpt) {
        double height = beginpt.getY() - endpt.getY();
        double length = distFormula(endpt.getX(), beginpt.getX(), endpt.getZ(), beginpt.getZ());
        return 35.30394 * height / (Math.hypot(height, length)); // 3.6 * gravity
    }

    static double distFormula(double x1, double x2, double z1, double z2) {
        return Math.hypot(x1 - x2, z1 - z2);
    }

    static double distFormula(Location loc1, Location loc2) {
        return Math.hypot(Math.hypot(loc1.getX() - loc2.getX(), loc1.getY() - loc2.getY()), loc1.getZ() - loc2.getZ());
    }

    static void showStopPos(utsvehicle lv, String stopposeval, int stopdistcm, String shock, int pts) {
        utsdriver ld = lv.getLd();
        // Need to fix stop pos? If no then add points
        if (!lv.isFixstoppos() && ld != null) {
            String s = " ";
            if (noFreemodeOrATO(lv.getLd())) {
                s = " " + ChatColor.GREEN + "+" + pts + " ";
                ld.setPoints(lv.getLd().getPoints() + pts);
            }
            generalMsg(ld.getP(), ChatColor.YELLOW, getLang(stopposeval) + s + ChatColor.GRAY + stopdistcm + " cm" + shock);
        }
    }

    static boolean noFreemodeOrATO(utsdriver ld) {
        return ld == null || !ld.isFreemode() && ld.getLv().getAtodest() == null;
    }

    static double accelSwitch(utsvehicle lv, double speed, int dcurrent) {
        double accel = lv.getAccel();
        int[] speedsteps = lv.getSpeedsteps();
        double retaccel = 0;
        if (dcurrent - 1 >= 0) {
            retaccel = accel * speedsteps[dcurrent] / speedsteps[5] * (1 - 0.5 * speed / speedsteps[5]);
            if (speed > speedsteps[dcurrent - 1]) {
                retaccel *= 1 - (speed - speedsteps[dcurrent - 1]) / (speedsteps[dcurrent] - speedsteps[dcurrent - 1]);
            }
        }
        if (retaccel < 0) {
            retaccel = 0;
        }
        return retaccel;
    }

    static double decelSwitch(utsvehicle lv, double speed, double slopeaccel) {
        double decel = lv.getDecel();
        double ebdecel = lv.getEbdecel();
        double speeddrop = lv.getSpeeddrop();
        double bcp = lv.getBcpressure();
        int[] speedsteps = lv.getSpeedsteps();
        double retdecel = 0;
        if (bcp == 0) {
            retdecel = speeddrop;
        } else if (bcp > 0 && bcp < 480) {
            retdecel = globalDecel(decel, speed, getBrakeFromPressure(bcp) + 1, speedsteps);
        } else if (bcp == 480) {
            if (lv.getAtsforced() != 2 && lv.getSignallimit() != 0) {
                retdecel = globalDecel(ebdecel, speed, 7, speedsteps);
            } else {
                // SPAD EB on 0 km/h signal
                lv.setAtsforced(2);
                lv.setSpeed(lv.getSpeed() - ebdecel * ONE_TICK_IN_S * 45 / 7);
            }
        }
        return retdecel - slopeaccel;
    }

    static double globalDecel(double decel, double speed, double rate, int[] speedsteps) {
        // (1 / 98) = (1 / 7 / 14)
        return (speed >= speedsteps[0]) ? (decel * rate * (15 - 4 * (speed - speedsteps[0]) / (speedsteps[5] - speedsteps[0])) / 98) : (decel * rate * 15 / 98);
    }

    static double avgRangeDecel(double decel, double upperspd, double lowerspd, double rate, int[] speedsteps) {
        double alpha = globalDecel(decel, upperspd, rate, speedsteps); // first term of geometric decel sequence, or current decel
        if (upperspd > lowerspd && upperspd > speedsteps[0]) {
            double k = decel * rate / (490 * (speedsteps[5] - speedsteps[0])) + 1; // result of da/dx / 20 + 1, or result of dividing decel of this tick to last tick
            double varlower = Math.max(lowerspd, speedsteps[0]); // lower end speed in variable range in decel graph
            double x = Math.max(0, Math.log(20 * (k - 1) * (upperspd - varlower) / alpha + 1) / Math.log(k)); // no. of ticks to lowerspeed / speedsteps[0]
            double sumdistvar = ((upperspd * x) - (alpha * (Math.pow(k, x) - 1 - x * (k - 1))) / (20 * Math.pow(k - 1, 2))) / 72; // braking distance in variable range
            double beta = globalDecel(decel, speedsteps[0], rate, speedsteps); // decel in static range
            double staticupper = Math.min(upperspd, speedsteps[0]); // upper end speed in static range in decel graph
            double sumdiststatic = Math.max(0, (Math.pow(staticupper, 2) - Math.pow(lowerspd, 2)) / (7.2 * beta)); // braking distance in static range
            // Prevent negative value
            return Math.max(0, (Math.pow(upperspd, 2) - Math.pow(lowerspd, 2)) / (7.2 * (sumdistvar + sumdiststatic)));
        } else {
            return alpha;
        }
    }

    static int minSpeedLimit(utsvehicle lv) {
        return Math.min(lv.getSpeedlimit(), lv.getSignallimit());
    }

    static AfterBrakeInitResult getAfterBrakeInitResult(utsvehicle lv, double upperSpeed, double slopeaccel, double bcp, double bcptarget) {
        boolean iseb = getBrakeFromPressure(bcptarget) == 9;
        double decel = iseb ? lv.getEbdecel() : lv.getDecel();
        double bcppertick = iseb ? lv.getEbbcppertick() : lv.getBcppertick();
        double timeleft = (bcptarget - bcp) / bcppertick * ONE_TICK_IN_S;
        double avgrate = bcp > 0 ? (bcptarget + bcp) * 9 / 960 + 1 : ((bcptarget + bcppertick) * 9 / 960 + 1) * (timeleft - 0.05) / timeleft; // average rate, separate cases for bcp < 0 or not
        double estlowerspeed = Math.max(0, upperSpeed - (decel * avgrate / 7 - slopeaccel) * timeleft); // estimated lower speed, rough result only for avgRangeDecel, prevent negative
        double avgdecel = avgRangeDecel(decel, Math.max(0, upperSpeed + slopeaccel), estlowerspeed, avgrate, lv.getSpeedsteps()) - slopeaccel; // gives better estimation than globalDecel, inaccuracy is negligible?
        // Time in s instead of tick to brake init end, but to prevent over-estimation and negative deceleration values
        double realt = avgdecel > 0 ? Math.min(timeleft, upperSpeed / avgdecel) : timeleft;
        return new AfterBrakeInitResult(avgdecel, realt);
    }

    static Location getCartActualRefPos(MinecartMember<?> mm, boolean flipdir) {
        double newx, newy, newz;
        AttachmentModel am = mm.getProperties().getModel();
        Location loc = mm.getEntity().getLocation();
        double cartlen = am.getCartLength();
        // Assuming carts with length within 1 m are minecarts
        if (cartlen <= 1) {
            return loc;
        } else {
            // 0.5 * cart length * direction flip multiplier
            double resultmul = 0.5 * cartlen * (flipdir ? -1 : 1);
            Quaternion ori = mm.getOrientation();
            // Roll is ignored as a cart is considered as a line, not a surface
            double yaw = ori.getYaw();
            double pitch = ori.getPitch();
            // -90 is west, 0 is south, 90 is east, 180 is north (degrees)
            newx = loc.getX() + resultmul * Math.cos(Math.toRadians(yaw - 90)) * Math.cos(Math.toRadians(pitch));
            newy = loc.getY() + resultmul * Math.sin(Math.toRadians(pitch));
            newz = loc.getZ() - resultmul * Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            // Note that wrong value is returned if not all carts are in the same orientation
            return new Location(mm.getWorld(), newx, newy, newz);
        }
    }

    static HeadAndTailResult getHeadAndTailResult(MinecartGroup mg) {
        // Might be any combination, so all scenarios have to be tested
        // Get longest length to get actual head and tail
        Location retHeadLoc = null;
        Location retTailLoc = null;
        double length = 0;
        for (boolean h : new boolean[]{false, true}) {
            for (boolean t : new boolean[]{false, true}) {
                Location testHeadLoc = getCartActualRefPos(mg.head(), h);
                Location testTailLoc = getCartActualRefPos(mg.tail(), t);
                double testlength = distFormula(testHeadLoc, testTailLoc);
                if (testlength > length) {
                    length = testlength;
                    retHeadLoc = testHeadLoc;
                    retTailLoc = testTailLoc;
                }
            }
        }
        return new HeadAndTailResult(retHeadLoc, retTailLoc);
    }

    static Location getDriverseatActualPos(utsvehicle lv) {
        // Driver seat may be flipped, therefore must test
        // Get longest length to get actual head and driver seat
        HeadAndTailResult result = getHeadAndTailResult(lv.getTrain());
        // Different reference positions for front and back train directions
        Location refloc = lv.getTrain().head().equals(lv.getDriverseat()) ? result.tailLoc : result.headLoc;
        Location retDriverseat = getCartActualRefPos(lv.getDriverseat(), false);
        double length = distFormula(retDriverseat, refloc);
        // Test for flipped case
        Location testDriverseat = getCartActualRefPos(lv.getDriverseat(), true);
        double testlength = distFormula(testDriverseat, refloc);
        if (testlength > length) {
            retDriverseat = testDriverseat;
        }
        return retDriverseat;
    }

    static String add3ZeroPadding(String str) {
        int len = str.length();
        String zero = ChatColor.DARK_GRAY + "0";
        StringBuilder newstr = new StringBuilder();
        // 2 * ChatColor also counts in string length
        for (int i = 0; i < 5 - len; i++) {
            newstr.append(zero);
        }
        newstr.append(str);
        return newstr.toString();
    }

    static class StopPosResult {
        public final double stopdist;
        public final int stopdistcm;

        public StopPosResult(double stopdist, int stopdistcm) {
            this.stopdist = stopdist;
            this.stopdistcm = stopdistcm;
        }
    }

    static class AfterBrakeInitResult {
        public final double avgdecel;
        public final double realt;

        public AfterBrakeInitResult(double avgdecel, double realt) {
            this.avgdecel = avgdecel;
            this.realt = realt;
        }
    }

    static class HeadAndTailResult {
        public final Location headLoc;
        public final Location tailLoc;

        public HeadAndTailResult(Location headLoc, Location tailLoc) {
            this.headLoc = headLoc;
            this.tailLoc = tailLoc;
        }
    }
}