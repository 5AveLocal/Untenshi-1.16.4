package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.atosign.*;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.events.*;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.motion.*;
import static me.fiveave.untenshi.signalsign.signalSignInterlock;
import static me.fiveave.untenshi.signalsign.signalSignWarn;
import static me.fiveave.untenshi.speedsign.*;
import static me.fiveave.untenshi.stoppos.stopPosDefault;

class ato {

    static void atoSys(utsvehicle lv, MinecartGroup mg) {
        double speeddrop = lv.getSpeeddrop();
        if (lv.getAtodest() != null && lv.getAtospeed() != -1 && lv.getAtsping() == 0 && lv.getAtsforced() == 0 && (lv.getLd() == null || lv.getLd().isAllowatousage())) {
            /*
             Get distances (distnow: smaller value of atodist and signaldist)
             reqatodist rate must be higher than others to prevent ATS-P or ATC run
            */
            double lowerSpeed = lv.getAtospeed();
            double decel = lv.getDecel();
            HeadAndTailResult result = getHeadAndTailResult(mg);
            Location actualAtoRefPos = getActualRefPos(lv.getAtodest(), mg.getWorld());
            double slopeaccelnow = getSlopeAccel(result.headLoc, result.tailLoc);
            double slopeaccelsel = getSlopeAccel(actualAtoRefPos, result.tailLoc);
            double slopeaccelsi = 0;
            double slopeaccelsp = 0;
            double reqatodist = getSingleReqdist(lv, lv.getSpeed(), lv.getAtospeed(), speeddrop, 6, slopeaccelsel, 0) + getThinkingDistance(lv, lv.getSpeed(), lv.getAtospeed(), decel, 6, slopeaccelsel, 0);
            double signaldist = Double.MAX_VALUE;
            double signaldistdiff = Double.MAX_VALUE;
            double speeddist = Double.MAX_VALUE;
            double speeddistdiff = Double.MAX_VALUE;
            double atodist = distFormula(actualAtoRefPos, result.headLoc);
            double atodistdiff = atodist - reqatodist;
            double reqsidist;
            double reqspdist;
            double distnow = atodist;
            int currentlimit = minSpeedLimit(lv);
            int finalmascon = 0;
            int finalbrake = 0;
            // Find either ATO, signal or speed limit distance, figure out which has the greatest priority (distnow - reqdist is the smallest value)
            if (lv.getLastsisign() != null && lv.getLastsisp() != MAX_SPEED) {
                Location actualSiRefPos = getActualRefPos(lv.getLastsisign(), mg.getWorld());
                slopeaccelsi = getSlopeAccel(actualSiRefPos, result.tailLoc);
                reqsidist = getSingleReqdist(lv, lv.getSpeed(), lv.getLastsisp(), speeddrop, 6, slopeaccelsi, 0)
                        + getThinkingDistance(lv, lv.getSpeed(), lv.getLastsisp(), decel, 6, slopeaccelsi, 0);
                signaldist = distFormula(actualSiRefPos, result.headLoc);
                signaldistdiff = signaldist - reqsidist;
            }
            if (lv.getLastspsign() != null && lv.getLastspsp() != MAX_SPEED) {
                Location actualSpRefPos = getActualRefPos(lv.getLastspsign(), mg.getWorld());
                slopeaccelsp = getSlopeAccel(actualSpRefPos, result.tailLoc);
                reqspdist = getSingleReqdist(lv, lv.getSpeed(), lv.getLastspsp(), speeddrop, 6, slopeaccelsp, 0)
                        + getThinkingDistance(lv, lv.getSpeed(), lv.getLastspsp(), decel, 6, slopeaccelsp, 0);
                speeddist = distFormula(actualSpRefPos, result.headLoc);
                speeddistdiff = speeddist - reqspdist;
            }
            double priority = (atodistdiff < signaldistdiff) ? (Math.min(atodistdiff, speeddistdiff)) : (Math.min(signaldistdiff, speeddistdiff));
            if (lv.getLastsisign() != null && lv.getLastsisp() != MAX_SPEED && priority == signaldistdiff) {
                lowerSpeed = lv.getLastsisp();
                distnow = signaldist;
                slopeaccelsel = slopeaccelsi;
            }
            if (lv.getLastspsign() != null && lv.getLastspsp() != MAX_SPEED && priority == speeddistdiff) {
                lowerSpeed = lv.getLastspsp();
                distnow = speeddist;
                slopeaccelsel = slopeaccelsp;
            }
            // Get brake distance (reqdist)
            double[] reqdist = new double[10];
            // Potential speed after acceleration (acceleration after P5 to N)
            double potentialspeed = getSpeedAfterPotentialAccel(lv, lv.getSpeed(), slopeaccelnow);
            // To prevent redundant setting of mascon to N when approaching any signal
            boolean nextredlight = lv.getLastsisp() == 0 && priority == signaldistdiff;
            // tempdist is for anti-ATS-run, stop at 1 m before 0 km/h signal
            double tempdist = nextredlight ? (distnow - 1 < 0 ? 0 : distnow - 1) : distnow;
            // Speed with slope acceleration considered
            double safeslopeaccelsel = Math.max(slopeaccelsel, 0); // Non-negative slope acceleration
            double saspeed = lv.getSpeed() + safeslopeaccelsel; // Speed with slope acceleration
            // Actual controlling part, extra tick to prevent huge shock on stopping
            getAllReqdist(lv, lv.getSpeed(), lowerSpeed, speeddrop, reqdist, slopeaccelsel, ONE_TICK_IN_S);
            // Require accel? (no need to prepare for braking for next object and ATO target destination + additional thinking distance)
            boolean allowaccel = (lv.getMascon() > 0 || currentlimit - lv.getSpeed() > 5 && (lowerSpeed - lv.getSpeed() > 5 || tempdist > speed1s(lv)) && lv.getBrake() == 0) // 5 km/h under speed limit / target speed or already accelerating
                    && potentialspeed <= currentlimit // Will not go over speed limit
                    && (potentialspeed <= lowerSpeed || tempdist > speed1s(lv)) // Will not go over target speed
                    && !lv.isOverrun() // Not overrunning
                    && lv.getDooropen() == 0 && lv.isDoorconfirm(); // Doors closed
            boolean notnearreqdist = tempdist > reqdist[6] + getThinkingDistance(lv, saspeed, lowerSpeed, decel, 6, slopeaccelsel, 3);
            if (notnearreqdist && allowaccel) {
                finalmascon = 5;
            }
            // Require braking? (with additional thinking time, if thinking distance is less than 1 m then consider as 1 m (prevent hard braking at low speeds))
            if (tempdist < reqdist[6] + Math.max(1, 2 * ONE_TICK_IN_S * getThinkingDistance(lv, lv.getSpeed(), lowerSpeed, decel, 6, slopeaccelsel, ONE_TICK_IN_S))) {
                lv.setAtoforcebrake(true);
            }
            // Direct pattern or forced?
            if (lv.isAtoforcebrake() || lv.isAtopisdirect()) {
                // If even emergency brake cannot brake in time
                finalbrake = 9;
                // For more accurate result (prevent EB misuse especially at low speeds when smaller brakes can do)
                double minreqdist = Double.MAX_VALUE;
                for (int b = 9; b >= 0; b--) {
                    if (tempdist >= reqdist[b] || reqdist[b] < minreqdist) {
                        finalbrake = b;
                        minreqdist = reqdist[b];
                    }
                }
            }
            // Cancel braking? (with additional thinking time)
            if (tempdist > reqdist[6] + getThinkingDistance(lv, saspeed + safeslopeaccelsel, lowerSpeed, decel, 6, slopeaccelsel, 3) && !lv.isOverrun()) {
                lv.setAtoforcebrake(false);
            }
            // 0 km/h signal waiting procedure (1 m (+ 1 m before signal) distance with signal)
            if (nextredlight && lv.getSpeed() == 0 && signaldist < 2) {
                if (finalbrake < 8) {
                    finalbrake = 8;
                }
                finalmascon = 0;
            }
            // Potentially over speed limit / next speed limit in 1 s
            if (lv.getSpeed() + slopeaccelnow > (distnow < speed1s(lv) ? lowerSpeed : currentlimit)) {
                lv.setAtoforceslopebrake(true);
            }
            // Slope braking (not related to ATS-P or ATC)
            if (lv.isAtoforceslopebrake()) {
                // Redefine reqdist (here for braking distance to speed limit)
                getAllReqdist(lv, lv.getSpeed() + slopeaccelnow, currentlimit, speeddrop, reqdist, slopeaccelnow, 0);
                int thisfinalbrake = 8;
                for (int a = 8; a >= 1; a--) {
                    double ssavgdecel = avgRangeDecel(decel, lv.getSpeed() + slopeaccelnow, currentlimit, a + 1, lv.getSpeedsteps());
                    // If braking distance is greater than distance in 1 s and if the brake is greater, then use the value
                    if (ssavgdecel > slopeaccelnow) {
                        thisfinalbrake = a;
                    }
                }
                if (finalbrake < thisfinalbrake) {
                    finalbrake = thisfinalbrake;
                }
            }
            // 3 s needed for release
            if (lv.getSpeed() + 3 * slopeaccelnow < currentlimit) {
                lv.setAtoforceslopebrake(false);
            }
            // Final value
            lv.setMascon(finalmascon);
            lv.setBrake(finalbrake);
            // EB when overrun
            if (lv.isOverrun() && atodist > 1) {
                toEB(lv);
            }
        } else if (lv.getAtodest() != null && lv.getAtospeed() != -1 && lv.getLd() != null && !lv.getLd().isAllowatousage()) {
            lv.setAtodest(null);
            lv.setAtospeed(-1);
            generalMsg(lv.getLd().getP(), ChatColor.GOLD, getLang("ato_patterncancel"));
        }
    }

    // Reset values, open doors, reset ATO
    static void stopProcedure(utsvehicle lv) {
        lv.setReqstopping(false);
        lv.setFixstoppos(false);
        lv.setOverrun(false);
        // Stop penalties (If have)
        utsdriver ld = lv.getLd();
        if (noFreemodeOrATO(ld)) {
            // In station EB
            if (lv.isStaeb()) {
                pointCounter(ld, ChatColor.YELLOW, getLang("eb_stop") + " ", -5, "");
            }
            // In station accel
            if (lv.isStaaccel()) {
                pointCounter(ld, ChatColor.YELLOW, getLang("reaccel") + " ", -5, "");
            }
        }
        lv.setStaeb(false);
        lv.setStaaccel(false);
        if (lv.isStopautodooropen()) {
            doorControls(lv, true);
        }
        // Provide output
        if (lv.getStopoutput() != null) {
            Block b = lv.getSavedworld().getBlockAt(lv.getStopoutput()[0], lv.getStopoutput()[1], lv.getStopoutput()[2]);
            b.getChunk().load();
            b.setType(Material.REDSTONE_BLOCK);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                b.setType(Material.AIR);
                lv.setStopoutput(null);
            }, 4);
        }
        // ATO Stop Time Countdown, cancelled if door is closed, unless door does not open automatically
        atoDepartCountdown(lv);
        if (lv.getAtospeed() != -1) {
            toB8(lv);
        }
        lv.setAtodest(null);
        lv.setAtospeed(-1);
    }

    // ATO Stop Time Countdown
    static void atoDepartCountdown(utsvehicle lv) {
        if (lv.getAtostoptime() != -1) {
            if (lv.getAtostoptime() > 0 && (lv.isDoordiropen() || !lv.isStopautodooropen())) {
                lv.setAtostoptime(lv.getAtostoptime() - 1);
                Bukkit.getScheduler().runTaskLater(plugin, () -> atoDepartCountdown(lv), 20);
            } else {
                doorControls(lv, false);
                // Reset values in order to depart
                lv.setAtostoptime(-1);
                lv.setAtodest(null);
                lv.setAtospeed(-1);
                waitDepart(lv, null, null);
            }
        }
    }

    static void waitDepart(utsvehicle lv, Location actualSiRefPos, Location cartactualpos) {
        if (lv != null && lv.getTrain().isValid()) {
            boolean notindist = true;
            double[] reqdist = new double[10];
            getAllReqdist(lv, minSpeedLimit(lv), 0, lv.getSpeeddrop(), reqdist, 0, 0);
            if (lv.getLastsisign() != null) {
                // Assuming positions will not be changed during loop, prevent lag
                actualSiRefPos = actualSiRefPos == null ? getActualRefPos(lv.getLastsisign(), lv.getSavedworld()) : actualSiRefPos;
                cartactualpos = cartactualpos == null ? getDriverseatActualPos(lv) : cartactualpos;
                notindist = (distFormula(actualSiRefPos, cartactualpos)) > 5;
            }
            // Wait doors fully closed then depart (if have red light in 5 meters do not depart)
            if (lv.getDooropen() == 0 && lv.isDoorconfirm() && lv.getBrake() != 9 && (lv.getLastsisp() != 0 || notindist) && lv.isAtoautodep() && lv.getAtsforced() == 0) {
                lv.setBrake(0);
                lv.setMascon(5);
                lv.setAtoautodep(false);
            } else if (lv.isAtoautodep()) {
                // Return as final variables
                Location finalCartactualpos = cartactualpos;
                Location finalActualSiRefPos = actualSiRefPos;
                Bukkit.getScheduler().runTaskLater(plugin, () -> waitDepart(lv, finalActualSiRefPos, finalCartactualpos), TICK_DELAY);
            }
        }
    }

    static void stopActionClock(utsvehicle lv, ItemMeta mat, int timecount, int stattimecount, boolean lastdoordiropen, boolean lastdoorconfirm, boolean lastreqstopping) {
        if (lv.getTrain().isValid()) {
            boolean stopped = lv.getSpeed() == 0;
            boolean reqstopping = lv.isReqstopping();
            if (mat instanceof BookMeta) {
                BookMeta bk = (BookMeta) mat;
                int pgcount = bk.getPageCount();
                boolean doordiropen = lv.isDoordiropen();
                boolean doorconfirm = lv.isDoorconfirm();
                // Test for all pages
                for (int pgno = 1; pgno <= pgcount; pgno++) {
                    String str = bk.getPage(pgno);
                    if (!str.isEmpty()) {
                        String[] timesplit = str.split(":", 2);
                        String[] trysplitstr = timesplit[1].split(";");
                        // Counter reset if status change
                        if (lastdoordiropen != doordiropen || lastdoorconfirm != doorconfirm) {
                            stattimecount = 0;
                        }

                        String status = (doordiropen ? "open" : "clos") + (doorconfirm ? "ed" : "ing") + stattimecount;
                        // Run book (format: open/clos + ed/ing + timecount)
                        if (stopped && timesplit[0].equals(status) || timesplit[0].equals("init") && timecount == 0) {
                            for (String onesplitstr : trysplitstr) {
                                // Run action string
                                actionCmdRunner(lv, onesplitstr);
                            }
                        }
                    }
                }
                // Continue unless departed
                if (reqstopping || lastreqstopping || stopped) {
                    // Add to counter, run next tick
                    int finalStatTimeCount = stattimecount;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> stopActionClock(lv, mat, timecount + 1, finalStatTimeCount + 1, doordiropen, doorconfirm, reqstopping), 1);
                }
            }
        }
    }

    static void actionCmdRunner(utsvehicle lv, String runstr) {
        String[] splitstr = runstr.split(" ", 2);
        String actionstr = splitstr[1];
        String[] actionstrsplit = actionstr.split(" ");
        switch (splitstr[0]) {
            case "stoppos":
                // stoppos <x> <y> <z> (stop position) <x> <y> <z> (redstone output)
                String[] sl3split = {actionstrsplit[0], actionstrsplit[1], actionstrsplit[2]};
                String sl4 = "";
                if (actionstrsplit.length == 6) {
                    sl4 = actionstrsplit[3] + " " + actionstrsplit[4] + " " + actionstrsplit[5];
                }
                stopPosDefault(lv, sl3split, sl4);
                break;
            case "atosign":
                switch (actionstrsplit[0]) {
                    case "stoptime":
                        // atosign stoptime <time>
                        atoSignStopTime(lv, parseInt(actionstrsplit[1]));
                        break;
                    case "dir":
                        // atosign dir <direction>
                        atoSignDir(lv, lv.getTrain(), lv.getTrain().head(), actionstrsplit[1], null);
                        break;
                    default:
                        // atosign <target_speed> <x> <y> <z> (target location)
                        double[] loc = new double[3];
                        String[] sloc = {actionstrsplit[1], actionstrsplit[2], actionstrsplit[3]};
                        for (int a = 0; a <= 2; a++) {
                            loc[a] = parseInt(sloc[a]);
                        }
                        atoSignDefault(lv, parseInt(actionstrsplit[0]), loc);
                        break;
                }
                break;
            case "speedsign":
                if (!actionstrsplit[0].equals("warn")) {
                    // speedsign <speed>
                    speedSignSet(lv, null, parseInt(actionstrsplit[0]));
                } else {
                    // speedsign warn <x> <y> <z> (target location)
                    String warnloc = actionstrsplit[1] + " " + actionstrsplit[2] + " " + actionstrsplit[3];
                    speedSignWarn(lv, null, warnloc);
                }
                break;
            case "signalsign":
                switch (actionstrsplit[0]) {
                    case "warn":
                        // signalsign warn <x> <y> <z> (target location)
                        String warnloc2 = actionstrsplit[1] + " " + actionstrsplit[2] + " " + actionstrsplit[3];
                        signalSignWarn(lv, null, warnloc2);
                        break;
                    case "interlock":
                        // signalsign interlock <signalorder> <priority/del> <x> <y> <z>
                        String[] ill3 = {actionstrsplit[0], actionstrsplit[1], actionstrsplit[2]};
                        String illoc = actionstrsplit[3] + " " + actionstrsplit[4] + " " + actionstrsplit[5];
                        signalSignInterlock(lv, ill3, illoc);
                        break;
                }
                break;
            case "run":
                // run <cmd...>
                String newexecutestr = getActionCmdExecuteStr(lv, actionstrsplit, actionstr);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), newexecutestr);
                break;
            case "set":
                // set <x> <y> <z> <material>
                Block b = lv.getSavedworld().getBlockAt(parseInt(actionstrsplit[0]), parseInt(actionstrsplit[1]), parseInt(actionstrsplit[2]));
                b.getChunk().load();
                b.setType(Material.valueOf(actionstrsplit[3].toUpperCase()));
                break;
        }
    }

    private static String getActionCmdExecuteStr(utsvehicle lv, String[] actionstrsplit, String actionstr) {
        String newexecutestr;
        // If is TrainCarts command (support MinecartGroup only)
        if (actionstrsplit[0].contains("train")) {
            // Run for train, add select train parameter at end of command
            newexecutestr = actionstr + " --train " + lv.getTrain().getProperties().getTrainName();
        } else {
            newexecutestr = "execute at " + lv.getTrain().head().getEntity().getEntity().getUniqueId() + " run " + actionstr;
        }
        return newexecutestr;
    }
}