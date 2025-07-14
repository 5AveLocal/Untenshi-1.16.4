package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.cmds.errorLog;
import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.signalsign.*;
import static me.fiveave.untenshi.speedsign.*;

class signalcmd implements CommandExecutor, TabCompleter {

    private static void resetSuccessCount(Block blk) {
        BlockData bd = blk.getBlockData();
        Material bm = blk.getType();
        if (blk.getState() instanceof CommandBlock) {
            CommandBlock cmdblk = (CommandBlock) blk.getState();
            String cmd = cmdblk.getCommand();
            blk.setType(Material.AIR, false);
            blk.setType(bm);
            blk.setBlockData(bd);
            cmdblk.setCommand(cmd);
            cmdblk.update();
        }
    }

    static boolean isIlClear(ItemMeta mat, World world, boolean lossy) {
        boolean isclear = true;
        if (mat instanceof BookMeta) {
            BookMeta bk = (BookMeta) mat;
            int pgcount = bk.getPageCount();
            // Get pages from books
            for (int pgno = 1; pgno <= pgcount; pgno++) {
                String str = bk.getPage(pgno);
                String[] trysplitstr = str.split(" ", 3);
                Location loc1 = getFullLoc(world, str);
                // try statement
                if (trysplitstr[0].equals("try")) {
                    Location fullloc2 = getFullLoc(world, trysplitstr[2]);
                    Chest refchest2 = getChestFromLoc(fullloc2);
                    isclear = tryItemIsIlClear(refchest2, world, lossy);
                    // No reading other locations after try statement
                    break;
                }
                for (MinecartGroup mg2 : vehicle.keySet()) {
                    if (vehicle.get(mg2) != null) {
                        utsvehicle lv2 = vehicle.get(mg2);
                        // Get rsposlist (if not lossy)
                        if (!lossy) {
                            Location[] rssign2locs = lv2.getRsposlist();
                            if (rssign2locs != null) {
                                signalOrderPtnResult result2 = getSignalOrderPtnResult(lv2);
                                // Check for each location
                                for (int j = 0; j < rssign2locs.length; j++) {
                                    Location location = rssign2locs[j];
                                    // Maximum is result.halfptnlen - 1, cannot exceed (else index not exist and value will be null)
                                    int minno = Math.min(result2.halfptnlen - 1, Math.max(0, j - lv2.getRsoccupiedpos()));
                                    // Resettable sign signal of lv2 is supposed to be 0 km/h by resettable sign
                                    if (result2.ptnsisp[minno] == 0 && loc1.equals(location)) {
                                        isclear = false;
                                        break;
                                    }
                                }
                            }
                        }
                        // Get ilposoccupied
                        Location[] il2posoccupied = lv2.getIlposoccupied();
                        if (il2posoccupied != null) {
                            for (Location loc2 : il2posoccupied) {
                                if (loc1.equals(loc2)) {
                                    isclear = false;
                                    break;
                                }
                            }
                        }
                        // Get ilposlist
                        Location[] il2poslist = lv2.getIlposoccupied();
                        if (il2poslist != null) {
                            for (Location loc2 : il2poslist) {
                                if (loc1.equals(loc2)) {
                                    isclear = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return isclear;
    }

    private static boolean tryItemIsIlClear(Chest refchest, World world, boolean lossy) {
        boolean isclear = false;
        if (refchest != null) {
            for (int itemno = 0; itemno < 27; itemno++) {
                ItemMeta mat = null;
                try {
                    mat = Objects.requireNonNull(refchest.getBlockInventory().getItem(itemno)).getItemMeta();
                } catch (Exception ignored) {
                }
                isclear = isIlClear(mat, world, lossy);
                // If one of them is not clear then result is not clear
                if (!isclear) {
                    break;
                }
            }
        }
        return isclear;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (sender instanceof BlockCommandSender) {
                int signalspeed = args[3].equals("sign") ? parseInt(args[5]) : 0;
                // Input position combined
                String inputpos = args[0] + " " + args[1] + " " + args[2];
                // Main content starts here
                if (limitSpeedIncorrect(sender, signalspeed)) return true;
                if (args[3].equals("warn") || args[3].equals("sign") || args[3].equals("getilclear") || args[3].equals("getilclearlossy")) {
                    World world = ((BlockCommandSender) sender).getBlock().getWorld();
                    String signalmsg;
                    switch (args[3]) {
                        // Signal speed limit warn
                        case "warn":
                            Sign warn = getSignFromLoc(getFullLoc(world, inputpos));
                            if (warn != null && warn.getLine(1).equals("signalsign")) {
                                // lastsisign and lastsisp are for detecting signal change
                                String warnsi = warn.getLine(2).split(" ")[1];
                                String warnsp = warn.getLine(2).split(" ")[2];
                                signalmsg = signalName(warnsi);
                                if (signalmsg.isEmpty()) {
                                    generalMsg(sender, ChatColor.RESET, ChatColor.RED + getLang("signal_typewrong"));
                                }
                                String showspeed = parseInt(warnsp) >= maxspeed ? getLang("speedlimit_del") : warnsp + " km/h";
                                generalMsg(sender, ChatColor.YELLOW, getLang("signal_warn") + " " + signalmsg + ChatColor.GRAY + " " + showspeed);
                            }
                            break;
                        // Set line 4 of sign at (line 3 of this sign) to turn signal
                        case "sign":
                            if (isSignalType(args[4])) {
                                Sign sign = getSignFromLoc(getFullLoc(world, inputpos));
                                if (sign != null) {
                                    updateSignals(sign, "set " + args[4] + " " + args[5]);
                                    generalMsg(sender, ChatColor.RESET, getLang("signal_signchange") + " (" + args[4] + " " + args[5] + ")");
                                }
                                break;
                            }
                        case "getilclear":
                        case "getilclearlossy":
                            Chest refchest = getChestFromLoc(getFullLoc(world, inputpos));
                            boolean lossy = args[3].equals("getilclearlossy");
                            boolean isclear = tryItemIsIlClear(refchest, world, lossy);
                            Block blk = ((BlockCommandSender) sender).getBlock();
                            if (isclear) {
                                Bukkit.dispatchCommand(sender, String.format("data modify block %s %s %s SuccessCount set value 15", blk.getX(), blk.getY(), blk.getZ()));
                                int delay = 4;
                                if (args.length == 5) {
                                    int newdelay = parseInt(args[4]);
                                    delay = newdelay == -1 ? -1 : Math.max(delay, newdelay);
                                }
                                if (delay != -1) {
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> resetSuccessCount(blk), delay);
                                }
                                generalMsg(sender, ChatColor.RESET, getLang("secclear"));
                            } else {
                                Bukkit.getScheduler().runTaskLater(plugin, () -> resetSuccessCount(blk), 1);
                                generalMsg(sender, ChatColor.RESET, getLang("secnotclear"));
                            }
                            break;
                        default:
                            generalMsg(sender, ChatColor.RESET, getLang("argwrong"));
                            break;
                    }
                }
            } else {
                generalMsg(sender, ChatColor.RESET, getLang("cmdblkonlycmd"));
            }
        } catch (Exception e) {
            errorLog(e, "signalcmd.onCommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> ta = new ArrayList<>();
        List<String> result = new ArrayList<>();
        if (sender instanceof Player) {
            Player p = (Player) sender;
            Block block = p.getTargetBlock(Collections.singleton(Material.AIR), 5);
            if (block.getType() != Material.COMMAND_BLOCK && block.getType() != Material.CHAIN_COMMAND_BLOCK && block.getType() != Material.REPEATING_COMMAND_BLOCK) {
                switch (args.length) {
                    case 1:
                        ta.addAll(Arrays.asList(block.getX() + " ", block.getX() + " " + block.getY(), block.getX() + " " + block.getY() + " " + block.getZ()));
                        break;
                    case 2:
                        ta.addAll(Arrays.asList(block.getY() + " ", block.getY() + " " + block.getZ()));
                        break;
                    case 3:
                        ta.add(block.getZ() + " ");
                        break;
                }
            }
        }
        if (args.length == 4) {
            ta.addAll(Arrays.asList("warn", "sign", "getilclear", "getilclearlossy"));
        } else if (args.length == 5) {
            switch (args[3]) {
                case "sign":
                    ta.addAll(Arrays.asList("r", "yy", "y", "yg", "g", "atc"));
                    break;
                case "getilclear":
                case "getilclearlossy":
                    ta.addAll(Arrays.asList("-1", "4"));
                    break;
                default:
                    ta.add("");
                    break;
            }
        } else {
            ta.add("");
        }
        ta.forEach(a -> {
            if (a.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                result.add(a);
            }
        });
        return result;
    }
}