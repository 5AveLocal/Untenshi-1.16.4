package me.fiveave.untenshi;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import java.util.HashMap;

import static me.fiveave.untenshi.speedsign.getTcCoastersSign;

public class signcache {

    private static final HashMap<Location, Sign> locsignmap = new HashMap<>();

    static void updateSignalsIfChanged(Location loc, String str) {
        if (locsignmap.containsKey(loc)) {
            Sign sign = locsignmap.get(loc);
            if (sign != null && sign.getLine(1).equals("signalsign") && !sign.getLine(2).equals(str)) {
                updateSignals(loc, sign, str);
            }
        } else {
            Sign sign = getSignFromLoc(loc);
            updateSignals(loc, sign, str);
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
                locsignmap.put(loc, sign);
            }
        }
    }

    static boolean isLocOfSign(Location loc) {
        if (locsignmap.containsKey(loc)) {
            return true;
        } else {
            BlockState bl = loc.getBlock().getState();
            return bl instanceof Sign;
        }
    }

    static Sign getSignFromLoc(Location loc) {
        Sign sign;
        if (locsignmap.containsKey(loc)) {
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
        locsignmap.put(loc, sign);
        return sign;
    }
}
