package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import static me.fiveave.untenshi.events.toEB;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.motion.*;

class ato {

    static void atosys(Player p, double accel, double decel, double ebdecel, double speeddrop, int[] speedsteps, MinecartGroup mg) {
        if (atodest.containsKey(p) && atospeed.containsKey(p) && !atsbraking.get(p) && !atsping.get(p) && atsforced.get(p).equals(0) && allowatousage.get(p)) {
            /*
             Get distances (distnow: smaller value of atodist and signaldist)
             reqatodist decelfr must be higher than others to prevent ATS-P or ATC run
             suitableaccel is for N to accel when difference is at least 5, or already accelerating
            */
            double lowerSpeed = atospeed.get(p);
            Location currentLoc = mg.head().getEntity().getLocation();
            // 0.0625 from result of getting mg.head() y-location
            Location targetLoc = new Location(mg.getWorld(), atodest.get(p)[0] + 0.5, atodest.get(p)[1] + 0.0625, atodest.get(p)[2] + 0.5);
            double reqatodist = getreqdist(p, ticksin1s * globaldecel(decel, speed.get(p), 7, speedsteps), atospeed.get(p), targetLoc, currentLoc);
            double signaldist;
            double signaldistdiff;
            double speeddist;
            double speeddistdiff;
            double atodist = distFormula(atodest.get(p)[0] + 0.5, p.getLocation().getX(), atodest.get(p)[2] + 0.5, p.getLocation().getZ());
            double atodistdiff = atodist - reqatodist;
            double reqsidist;
            double reqspdist;
            double distnow = atodist;
            int currentlimit = minSpeedLimit(p);
            int finalmascon = 0;
            boolean suitableaccel = ((currentlimit - speed.get(p) > 5 && mascon.get(p) == 0) || mascon.get(p) > 0) && !overrun.get(p);
            // Find either ATO, signal or speed limit distance, figure out which has the greatest priority (distnow - reqdist is the smallest value)
            if (lastsisign.containsKey(p) && lastsisp.containsKey(p)) {
                reqsidist = getreqdist(p, ticksin1s * globaldecel(decel, speed.get(p), 6, speedsteps), lastsisp.get(p), lastsisign.get(p), currentLoc);
                signaldist = distFormula(lastsisign.get(p).getX() + 0.5, p.getLocation().getX(), lastsisign.get(p).getZ() + 0.5, p.getLocation().getZ());
                signaldistdiff = signaldist - reqsidist;
            } else {
                signaldist = Double.MAX_VALUE;
                signaldistdiff = Double.MAX_VALUE;
            }
            if (lastspsign.containsKey(p) && lastspsp.containsKey(p)) {
                reqspdist = getreqdist(p, ticksin1s * globaldecel(decel, speed.get(p), 6, speedsteps), lastspsp.get(p), lastspsign.get(p), currentLoc);
                speeddist = distFormula(lastspsign.get(p).getX() + 0.5, p.getLocation().getX(), lastspsign.get(p).getZ() + 0.5, p.getLocation().getZ());
                speeddistdiff = speeddist - reqspdist;
            } else {
                speeddist = Double.MAX_VALUE;
                speeddistdiff = Double.MAX_VALUE;
            }
            double priority = (atodistdiff < signaldistdiff) ? (Math.min(atodistdiff, speeddistdiff)) : (Math.min(signaldistdiff, speeddistdiff));
            if (lastsisign.containsKey(p) && lastsisp.containsKey(p) && priority == signaldistdiff) {
                lowerSpeed = lastsisp.get(p);
                targetLoc = lastsisign.get(p);
                distnow = signaldist;
            }
            if (lastspsign.containsKey(p) && lastspsp.containsKey(p) && priority == speeddistdiff) {
                lowerSpeed = lastspsp.get(p);
                targetLoc = lastspsign.get(p);
                distnow = speeddist;
            }
            // Get brake distance (reqdist)
            double[] reqdist = new double[10];
            getallreqdist(p, decel, ebdecel, speeddrop, speedsteps, lowerSpeed, reqdist, targetLoc, currentLoc);
            // If no signal give it one
            lastsisp.putIfAbsent(p, 360);
            // Actual controlling part
            // tempdist is for anti-ATS-run, stop at 5 m before 0 km/h signal
            double tempdist = lastsisp.get(p).equals(0) ? (distnow < 0 ? 0 : distnow - 5) : distnow;
            // Require accel? distnow - reqdist[5] > speed1s: accel end not yet reached (no need to prepare for braking yet)
            if (distnow - reqdist[5] > speed1s(p) && suitableaccel) {
                finalmascon = 5;
            }
            // Require braking? (additional thinking time to prevent braking too hard)
            if (tempdist < reqdist[6] + speed1s(p) * getThinkingTime(p, 6)) {
                atoforcebrake.put(p, true);
            }
            // Cancel braking?
            if (tempdist > reqdist[1] + 2 * speed1s(p) * getThinkingTime(p, 1)) {
                atoforcebrake.put(p, false);
            }
            // Direct pattern or forced?
            if (atoforcebrake.get(p) || atopisdirect.get(p)) {
                if (speed.get(p) > lowerSpeed) {
                    // tempdist is for anti-ATS-run, stop at 5 m before 0 km/h signal
                    for (int b = 9; b >= 0; b--) {
                        if (tempdist >= reqdist[b]) {
                            finalmascon = -b;
                        }
                        // If even emergency brake cannot brake in time
                        else if (tempdist < reqdist[9]) {
                            finalmascon = -9;
                        }
                    }
                } else if (suitableaccel) {
                    finalmascon = 5;
                }
            }
            // Speeding prevention for all (accelswitch to prevent accel then decel)
            if (speed.get(p) + accelswitch(accel, 5, speed.get(p), speedsteps) * ticksin1s > currentlimit && finalmascon > 0) {
                finalmascon = 0;
            }
            // Slightly speeding auto braking (not related to ATS-P or ATC)
            if (speed.get(p) > currentlimit) {
                // Redefine reqdist (here for braking distance to speed limit)
                getallreqdist(p, decel, ebdecel, speeddrop, speedsteps, currentlimit, reqdist, currentLoc, mg.tail().getEntity().getLocation());
                int finalbrake = -8;
                for (int a = 8; a >= 1; a--) {
                    // If braking distance is greater than distance in 1 s and if the brake is greater, then use the value
                    if (reqdist[a] <= speed.get(p) / 3.6) {
                        finalbrake = -a;
                    }
                }
                if (finalmascon > finalbrake) {
                    finalmascon = finalbrake;
                }
            }
            // Final value
            mascon.put(p, finalmascon);
            // EB when overrun
            if (overrun.get(p) && atodist > 1) {
                toEB(p);
            }
        }
    }

    private static void getallreqdist(Player p, double decel, double ebdecel, double speeddrop, int[] speedsteps, double lowerSpeed, double[] reqdist, Location endpt, Location startpt) {
        reqdist[9] = getreqdist(p, ticksin1s * globaldecel(decel, speed.get(p), ebdecel, speedsteps), lowerSpeed, endpt, startpt);
        // Get speed drop distance
        reqdist[0] = getreqdist(p, speeddrop, lowerSpeed, endpt, startpt);
        for (int a = 1; a <= 8; a++) {
            // Plus reaction time (0.1s is basic time)
            reqdist[a] = getreqdist(p, ticksin1s * globaldecel(decel, speed.get(p), a + 1, speedsteps), lowerSpeed, endpt, startpt) + speed1s(p) * getThinkingTime(p, a);
        }
    }

    private static double getThinkingTime(Player p, int a) {
        return Math.max(1.0 / ticksin1s, 0.1 * Math.min(a, a + (current.get(p) * 9 / 480)));
    }

    static double speed1s(Player p) {
        return speed.get(p) / 3.6;
    }
}