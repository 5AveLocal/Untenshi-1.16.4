package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.fiveave.untenshi.cmds.*;
import static me.fiveave.untenshi.main.*;

class debugcmd implements CommandExecutor, TabCompleter {

    private static void debugSet(CommandSender sender, String[] args, utsvehicle lv) {
        switch (args[2].toLowerCase()) {
            case "speed":
                lv.setSpeed(Double.parseDouble(args[3]));
                break;
            case "mascon":
                lv.setMascon(Integer.parseInt(args[3]));
                break;
            case "brake":
                lv.setBrake(Integer.parseInt(args[3]));
                break;
            case "speedlimit":
                lv.setSpeedlimit(Integer.parseInt(args[3]));
                break;
            case "signallimit":
                lv.setSignallimit(Integer.parseInt(args[3]));
                break;
            case "atospeed":
                lv.setAtospeed(Double.parseDouble(args[3]));
                break;
            case "atodest":
                Location loc1 = new Location(lv.getSavedworld(), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                lv.setAtodest(loc1);
                break;
            case "stoppos":
                Location loc5 = new Location(lv.getSavedworld(), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                lv.setStoppos(loc5);
                break;
            case "stopoutput":
                Location loc4 = new Location(lv.getSavedworld(), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                lv.setStopoutput(loc4);
                break;
            case "lastspsp":
                lv.setLastspsp(Integer.parseInt(args[3]));
                break;
            case "lastspsign":
                Location loc2 = new Location(lv.getSavedworld(), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                lv.setLastspsign(loc2);
                break;
            case "lastsisp":
                lv.setLastsisp(Integer.parseInt(args[3]));
                break;
            case "lastsisign":
                Location loc3 = new Location(lv.getSavedworld(), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                lv.setLastsisign(loc3);
                break;
            case "rsposlist":
            case "ilposlist":
            case "ilposoccupied":
            case "ilpriority":
            case "ilenterqueuetime":
            case "ld":
                generalMsg(sender, ChatColor.YELLOW, "Option is not available yet.");
                break;
            case "atsforced":
                lv.setAtsforced(Integer.parseInt(args[3]));
                break;
            case "atsping":
                lv.setAtsping(Integer.parseInt(args[3]));
                break;
            case "atspnear":
                lv.setAtspnear(Boolean.parseBoolean(args[3]));
                break;
            case "accel":
                lv.setAccel(Double.parseDouble(args[3]));
                break;
            case "decel":
                lv.setDecel(Double.parseDouble(args[3]));
                break;
            case "ebdecel":
                lv.setEbdecel(Double.parseDouble(args[3]));
                break;
            case "speeddrop":
                lv.setSpeeddrop(Double.parseDouble(args[3]));
                break;
            case "current":
                lv.setCurrent(Double.parseDouble(args[3]));
                break;
            case "bcpressure":
                lv.setBcpressure(Double.parseDouble(args[3]));
                break;
            case "currentpertick":
                lv.setCurrentpertick(Double.parseDouble(args[3]));
                break;
            case "bcppertick":
                lv.setBcppertick(Double.parseDouble(args[3]));
                break;
            case "ebbcppertick":
                lv.setEbbcppertick(Double.parseDouble(args[3]));
                break;
            case "dooropenspeed":
                lv.setDooropenspeed(Double.parseDouble(args[3]));
                break;
            case "doorclosespeed":
                lv.setDoorclosespeed(Double.parseDouble(args[3]));
                break;
            case "reqstopping":
                lv.setReqstopping(Boolean.parseBoolean(args[3]));
                break;
            case "overrun":
                lv.setOverrun(Boolean.parseBoolean(args[3]));
                break;
            case "fixstoppos":
                lv.setFixstoppos(Boolean.parseBoolean(args[3]));
                break;
            case "dooropen":
                lv.setDooropen(Integer.parseInt(args[3]));
                break;
            case "doordiropen":
                lv.setDoordiropen(Boolean.parseBoolean(args[3]));
                break;
            case "doorconfirm":
                lv.setDoorconfirm(Boolean.parseBoolean(args[3]));
                break;
            case "atopisdirect":
                lv.setAtopisdirect(Boolean.parseBoolean(args[3]));
                break;
            case "atoforcebrake":
                lv.setAtoforcebrake(Boolean.parseBoolean(args[3]));
                break;
            case "atoautodep":
                lv.setAtoautodep(Boolean.parseBoolean(args[3]));
                break;
            default:
                generalMsg(sender, ChatColor.RED, getLang("argwrong"));
                break;
        }
    }

    private static void debugGet(CommandSender sender, String[] args, utsvehicle lv) {
        String retstr = "";
        switch (args[2].toLowerCase()) {
            case "speed":
                retstr = String.valueOf(lv.getSpeed());
                break;
            case "mascon":
                retstr = String.valueOf(lv.getMascon());
                break;
            case "brake":
                retstr = String.valueOf(lv.getBrake());
                break;
            case "speedlimit":
                retstr = String.valueOf(lv.getSpeedlimit());
                break;
            case "signallimit":
                retstr = String.valueOf(lv.getSignallimit());
                break;
            case "atospeed":
                retstr = String.valueOf(lv.getAtospeed());
                break;
            case "atodest":
                retstr = locToString(lv.getAtodest());
                break;
            case "stoppos":
                retstr = locToString(lv.getStoppos());
                break;
            case "stopoutput":
                retstr = locToString(lv.getStopoutput());
                break;
            case "lastspsp":
                retstr = String.valueOf(lv.getLastspsp());
                break;
            case "lastspsign":
                retstr = locToString(lv.getLastspsign());
                break;
            case "lastsisp":
                retstr = String.valueOf(lv.getLastsisp());
                break;
            case "lastsisign":
                retstr = locToString(lv.getLastsisign());
                break;
            case "rsposlist":
                retstr = locListToString(lv.getRsposlist());
                break;
            case "ilposlist":
                retstr = locListToString(lv.getIlposlist());
                break;
            case "ilposoccupied":
                retstr = locListToString(lv.getIlposoccupied());
                break;
            case "ilpriority":
                retstr = String.valueOf(lv.getIlpriority());
                break;
            case "ilenterqueuetime":
                retstr = String.valueOf(lv.getIlenterqueuetime());
                break;
            case "atsforced":
                retstr = String.valueOf(lv.getAtsforced());
                break;
            case "atsping":
                retstr = String.valueOf(lv.getAtsping());
                break;
            case "atspnear":
                retstr = String.valueOf(lv.isAtspnear());
                break;
            case "ld":
                retstr = lv.getLd().getP().getName();
                break;
            case "accel":
                retstr = String.valueOf(lv.getAccel());
                break;
            case "decel":
                retstr = String.valueOf(lv.getDecel());
                break;
            case "ebdecel":
                retstr = String.valueOf(lv.getEbdecel());
                break;
            case "speeddrop":
                retstr = String.valueOf(lv.getSpeeddrop());
                break;
            case "current":
                retstr = String.valueOf(lv.getCurrent());
                break;
            case "bcpressure":
                retstr = String.valueOf(lv.getBcpressure());
                break;
            case "currentpertick":
                retstr = String.valueOf(lv.getCurrentpertick());
                break;
            case "bcppertick":
                retstr = String.valueOf(lv.getBcppertick());
                break;
            case "ebbcppertick":
                retstr = String.valueOf(lv.getEbbcppertick());
                break;
            case "dooropenspeed":
                retstr = String.valueOf(lv.getDooropenspeed());
                break;
            case "doorclosespeed":
                retstr = String.valueOf(lv.getDoorclosespeed());
                break;
            case "reqstopping":
                retstr = String.valueOf(lv.isReqstopping());
                break;
            case "overrun":
                retstr = String.valueOf(lv.isOverrun());
                break;
            case "fixstoppos":
                retstr = String.valueOf(lv.isFixstoppos());
                break;
            case "dooropen":
                retstr = String.valueOf(lv.getDooropen());
                break;
            case "doordiropen":
                retstr = String.valueOf(lv.isDoordiropen());
                break;
            case "doorconfirm":
                retstr = String.valueOf(lv.isDoorconfirm());
                break;
            case "atopisdirect":
                retstr = String.valueOf(lv.isAtopisdirect());
                break;
            case "atoforcebrake":
                retstr = String.valueOf(lv.isAtoforcebrake());
                break;
            case "atoautodep":
                retstr = String.valueOf(lv.isAtoautodep());
                break;
            default:
                generalMsg(sender, ChatColor.RED, getLang("argwrong"));
                break;
        }
        generalMsg(sender, ChatColor.GRAY, args[0] + ": " + ChatColor.YELLOW + args[2].toLowerCase() + ChatColor.WHITE + " = " + ChatColor.GREEN + retstr);
    }

    public static String locToString(Location loc) {
        String retstr = "null";
        try {
            for (int i = 0; i < 3; i++) {
                retstr = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
            }
        } catch (Exception ignored) {

        }
        return retstr;
    }

    public static String locListToString(Location[] locs) {
        StringBuilder retstr = new StringBuilder();
        int i = 0;
        try {
            while (i < locs.length) {
                retstr.append(String.format("\n%s%d%s:%s %d %d %d", ChatColor.AQUA, i, ChatColor.WHITE, ChatColor.GREEN, locs[i].getBlockX(), locs[i].getBlockY(), locs[i].getBlockZ()));
                i++;
            }
        } catch (Exception e) {
            retstr.append("null");
        }
        return retstr.toString();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length >= 3) {
                if (sender instanceof Player && checkPerm((Player) sender, "uts.debug")) {
                    noPerm(sender);
                    return true;
                }
                utsvehicle lv = null;
                try {
                    lv = vehicle.get(TrainProperties.get(args[0]).getHolder());
                } catch (Exception ignored) {
                }
                if (lv != null) {
                    switch (args[1].toLowerCase()) {
                        case "get":
                            debugGet(sender, args, lv);
                            break;
                        case "set":
                            if (args.length >= 4) {
                                try {
                                    debugSet(sender, args, lv);
                                } catch (Exception e) {
                                    generalMsg(sender, ChatColor.RED, getLang("argwrong"));
                                }
                            } else {
                                generalMsg(sender, ChatColor.RED, getLang("argwrong"));
                            }
                    }
                }
            } else {
                generalMsg(sender, ChatColor.RED, getLang("argwrong"));
            }
        } catch (Exception e) {
            errorLog(e);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> ta = new ArrayList<>();
        List<String> result = new ArrayList<>();
        List<String> vehiclelist = new ArrayList<>();
        vehicle.keySet().forEach(mg -> {
            if (!mg.isUnloaded()) {
                vehiclelist.add(mg.getProperties().getTrainName());
            }
        });
        switch (args.length) {
            case 1:
                ta.addAll(vehiclelist);
                break;
            case 2:
                ta.addAll(Arrays.asList("get", "set"));
                break;
            case 3:
                ta.addAll(Arrays.asList("speed", "mascon", "brake", "speedlimit", "signallimit", "atospeed", "atodest", "stoppos", "stopoutput", "lastspsp", "lastspsign", "lastsisp", "lastsisign", "rsposlist", "ilposlist", "ilposoccupied", "ilpriority", "ilenterqueuetime", "atsforced", "atsping", "atspnear", "ld", "accel", "decel", "ebdecel", "speeddrop", "current", "bcpressure", "currentpertick", "bcppertick", "ebbcppertick", "dooropenspeed", "doorclosespeed", "reqstopping", "overrun", "fixstoppos", "dooropen", "doordiropen", "doorconfirm", "atopisdirect", "atoforcebrake", "atoautodep"));
                break;
        }
        ta.forEach(a -> {
            if (a.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                result.add(a);
            }
        });
        return result;
    }
}