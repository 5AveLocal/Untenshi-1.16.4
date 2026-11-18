package me.fiveave.untenshi;

import com.bergerkiller.bukkit.coasters.tracks.TrackNode;
import com.bergerkiller.bukkit.coasters.tracks.TrackNodeSign;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static me.fiveave.untenshi.atosign.getLocFromString;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.motion.getNearestTrackNode;

public class signcache {

    private static final HashMap<Location, Sign> locsignmap = new HashMap<>();
    private static final ArrayList<Location> locnonsignlist = new ArrayList<>();
    private static final HashMap<Location, Location> realsignlocmap = new HashMap<>();
    private static final HashMap<Location, Location> realreflocmap = new HashMap<>();
    private static final HashMap<Location, Location> actualrefposmap = new HashMap<>();

    static Location getRealSignLoc(Location loc) {
        // For Sign.getLocation() only, use getRealRefLoc() for others
        World w = loc.getWorld();
        Location retloc = realsignlocmap.get(loc);
        if (retloc != null) {
            return retloc;
        }
        if (tcc != null && tcc.isEnabled() && w != null && !isLocOfSign(loc)) {
            // From cartevent to real location conversion
            TrackNode tn = getNearestTrackNode(w, loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5);
            retloc = tn == null ? loc : tn.getPosition().toLocation(w);
            realsignlocmap.put(loc, retloc);
            return retloc;
        }
        retloc = loc.clone().add(0.5, 0, 0.5);
        realsignlocmap.put(loc, retloc);
        return retloc;
    }

    static Location getRealRefLoc(Location loc) {
        // For non-Sign.getLocation(), use getRealSignLoc() for Sign.getLocation()
        World w = loc.getWorld();
        Location retloc = realreflocmap.get(loc);
        if (retloc != null) {
            return retloc;
        }
        if (tcc != null && tcc.isEnabled() && w != null && !isLocOfSign(loc)) {
            // From cartevent to real location conversion
            TrackNode tn = getNearestTrackNode(w, loc.getX(), loc.getY(), loc.getZ());
            retloc = tn == null ? loc : tn.getPosition().toLocation(w);
            realreflocmap.put(loc, retloc);
            return retloc;
        }
        // Same as result
        realreflocmap.put(loc, loc);
        return loc;
    }

    static void updateSignalsIfChanged(Location loc, String str) {
        if (locsignmap.containsKey(loc)) {
            Sign sign = locsignmap.get(loc);
            if (sign != null && sign.getLine(1).equals("signalsign") && !sign.getLine(2).equals(str)) {
                updateSignals(loc, sign, str);
            }
        } else {
            Sign sign = getSignFromLoc(loc);
            if (sign != null) {
                updateSignals(loc, sign, str);
            }
        }
    }

    private static void updateSignals(Location loc, Sign sign, String str) {
        if (sign.getLine(1).equals("signalsign")) {
            sign.setLine(2, str);
            sign.update();
            BlockState bl = loc.getBlock().getState();
            if (bl instanceof Sign) {
                locsignmap.put(loc, (Sign) bl);
            } else {
                sign = getTcCoastersSign(loc);
                if (sign != null) {
                    locsignmap.put(loc, sign);
                } else {
                    locnonsignlist.add(loc);
                }
            }
        }
    }

    static boolean isLocOfSign(Location loc) {
        if (locnonsignlist.contains(loc)) {
            return false;
        } else if (locsignmap.containsKey(loc)) {
            return true;
        } else {
            BlockState bl = loc.getBlock().getState();
            return bl instanceof Sign;
        }
    }

    static Sign getSignFromLoc(Location loc) {
        Sign sign;
        if (locnonsignlist.contains(loc)) {
            return null;
        } else if (locsignmap.containsKey(loc)) {
            return locsignmap.get(loc);
        } else {
            BlockState bl = loc.getBlock().getState();
            if (bl instanceof Sign) {
                sign = (Sign) bl;
                locsignmap.put(loc, sign);
                return sign;
            }
        }
        // If cannot get sign directly from location
        sign = getTcCoastersSign(loc);
        if (sign != null) {
            locsignmap.put(loc, sign);
        } else {
            locnonsignlist.add(loc);
        }
        return sign;
    }

    // Clear sign cache on reload
    static void clearCaches() {
        locsignmap.clear();
        locnonsignlist.clear();
        realsignlocmap.clear();
        realreflocmap.clear();
        actualrefposmap.clear();
    }

    // Get location from string
    static Location getFullLoc(String loctext, Location signloc) {
        double[] newloc = new double[3];
        getLocFromString(loctext, signloc, newloc);
        return new Location(signloc.getWorld(), newloc[0], newloc[1], newloc[2]);
    }

    // Get chest from sign
    static Chest getChestFromLoc(Location loc) {
        BlockState bl = loc.getBlock().getState();
        return bl instanceof Chest ? (Chest) bl : null;
    }

    // Get rail position from sign
    static Location getActualRefPos(Location loc) {
        Location retloc = actualrefposmap.get(loc);
        if (retloc != null) {
            return retloc;
        }
        int[] blkoffset = new int[]{0, 0, 0};
        Sign sign = getSignFromLoc(loc);
        if (sign != null) {
            if (sign instanceof WallSign) {
                blkoffset[1] = 1;
                WallSign ws = (WallSign) sign;
                switch (ws.getFacing()) {
                    case NORTH:
                        blkoffset[2] = 1;
                        break;
                    case SOUTH:
                        blkoffset[2] = -1;
                        break;
                    case WEST:
                        blkoffset[0] = 1;
                        break;
                    case EAST:
                        blkoffset[0] = -1;
                        break;
                }
            } else {
                blkoffset[1] = 2;
            }
            int tempyoffset = blkoffset[1];
            while (true) {
                List<Material> railmateriallist = Arrays.asList(Material.RAIL, Material.ACTIVATOR_RAIL, Material.DETECTOR_RAIL, Material.POWERED_RAIL);
                if (railmateriallist.contains(sign.getWorld().getBlockAt(loc.getBlockX() + blkoffset[0], loc.getBlockY() + tempyoffset, loc.getBlockZ() + blkoffset[2]).getType())) {
                    blkoffset[1] = tempyoffset;
                    break;
                }
                tempyoffset++;
                // Anti over height limit / finding rail failed
                if (loc.getY() + tempyoffset > 320) {
                    break;
                }
            }
        }
        retloc = new Location(loc.getWorld(), loc.getX() + blkoffset[0], loc.getY() + blkoffset[1] + CART_Y_POS_DIFF, loc.getZ() + blkoffset[2]);
        actualrefposmap.put(loc, retloc);
        return retloc;
    }

    static void signImproper(Location loc, utsdriver ld) {
        String s = UTS_HEAD + ChatColor.RED + getLang("signimproper") + " (" + loc.getX() + " " + loc.getY() + " " + loc.getZ() + ")";
        if (ld != null && ld.getP() != null) {
            ld.getP().sendMessage(s);
        }
        Bukkit.getConsoleSender().sendMessage(s);
    }

    static boolean limitSpeedIncorrect(CommandSender p, int speedlimit) {
        if (speedlimit < 0 || Math.floorMod(speedlimit, 5) != 0 || speedlimit > MAX_SPEED) {
            if (p != null) {
                generalMsg(p, ChatColor.RED, getLang("argwrong"));
            }
            return true;
        }
        return false;
    }

    // Get sign from node from location
    static Sign getTcCoastersSign(Location signloc) {
        Sign sign = null;
        // Using Sign.getLocation() will truncate values
        if (tcc != null && tcc.isEnabled()) {
            TrackNode tn = getNearestTrackNode(signloc.getWorld(), signloc.getX(), signloc.getY(), signloc.getZ());
            if (tn != null) {
                TrackNodeSign[] tnsigns = tn.getSigns();
                if (tnsigns.length > 0) {
                    sign = tnsigns[0].getTrackedSign().sign;
                }
            }
        }
        return sign;
    }
}
