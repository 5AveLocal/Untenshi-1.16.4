package me.fiveave.untenshi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.cmds.noPerm;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.speedsign.getSignFromLoc;
import static me.fiveave.untenshi.speedsign.isLocOfSign;
import static me.fiveave.untenshi.utscmduser.initCmdUser;

class signtool implements CommandExecutor, TabCompleter, Listener {

    static void editPlaceSign(utscmduser cu, Block blk) {
        Location loc = blk.getLocation();
        if (isLocOfSign(loc)) {
            Sign sign = getSignFromLoc(loc);
            String signdir = cu.getSigndir();
            Location signloc = cu.getSignloc();
            if (sign != null) {
                if (!signdir.equals("none")) {
                    sign.setLine(0, "[+train:" + signdir + "]");
                } else {
                    sign.setLine(0, "[+train]");
                }
                sign.setLine(1, cu.getSigntype());
                sign.setLine(2, "warn");
                sign.setLine(3, signloc.getBlockX() + " " + signloc.getBlockY() + " " + signloc.getBlockZ());
                sign.update();
                cu.setSignplacing(false);
                generalMsg(cu.getP(), ChatColor.GREEN, getLang("signsetsuccess"));
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                generalMsg(sender, ChatColor.RED, getLang("playeronlycmd"));
                return true;
            }
            if (!sender.hasPermission("uts.") && !sender.isOp()) {
                noPerm(sender);
                return true;
            }
                Player p = (Player) sender;
                initCmdUser(p);
                utscmduser cu = cmduser.get(p);
                if (args.length > 0) {
                    switch (args[0].toLowerCase()) {
                        case "getsign":
                            Block block = p.getTargetBlock(Collections.singleton(Material.AIR), 5);
                            Location loc = block.getLocation();
                            Sign sign = getSignFromLoc(block.getLocation());
                            if (sign != null) {
                                switch (sign.getLine(1)) {
                                    case "speedsign":
                                        try {
                                            Integer.parseInt(sign.getLine(2));
                                            cu.setSigntype("speedsign");
                                            cu.setSignloc(loc);
                                            generalMsg(p, ChatColor.GREEN, getLang("signselsuccess"));
                                            break;
                                        } catch (Exception ignored) {
                                        }
                                        break;
                                    case "signalsign":
                                        if (sign.getLine(2).split(" ")[0].equals("set")) {
                                            cu.setSigntype("signalsign");
                                            cu.setSignloc(loc);
                                            generalMsg(p, ChatColor.GREEN, getLang("signselsuccess"));
                                            break;
                                        }
                                    default:
                                        generalMsg(p, ChatColor.RED, getLang("signwrong"));
                                        break;
                                }
                            } else {
                                generalMsg(p, ChatColor.RED, getLang("signwrong"));
                            }
                            break;
                        case "setdir":
                            if (args.length > 1) {
                                switch (args[1].toLowerCase()) {
                                    case "east":
                                    case "west":
                                    case "north":
                                    case "south":
                                    case "northeast":
                                    case "northwest":
                                    case "southeast":
                                    case "southwest":
                                    case "left":
                                    case "right":
                                    case "upwards":
                                    case "downwards":
                                    case "forward":
                                    case "backwards":
                                    case "none":
                                    case "e":
                                    case "w":
                                    case "n":
                                    case "s":
                                    case "ne":
                                    case "nw":
                                    case "se":
                                    case "sw":
                                    case "l":
                                    case "r":
                                    case "u":
                                    case "d":
                                    case "f":
                                    case "b":
                                        cu.setSigndir(args[1].toLowerCase());
                                        generalMsg(p, ChatColor.GREEN, getLang("signdirset"));
                                        break;
                                    default:
                                        generalMsg(p, ChatColor.RED, getLang("signdirwrong"));
                                        break;
                                }
                            } else {
                                generalMsg(p, ChatColor.RED, getLang("signdirwrong"));
                            }
                            break;
                        case "setsign":
                            if (cu.getSignloc() != null && cu.getSigntype() != null && cu.getSigndir() != null) {
                                generalMsg(p, ChatColor.YELLOW, getLang("signset5s"));
                                cu.setSignplacing(true);
                                waitPlaceSignClock(p, 0);
                            } else {
                                generalMsg(p, ChatColor.RED, getLang("signreqseldir"));
                            }
                            break;
                        default:
                            generalMsg(sender, ChatColor.RED, getLang("argwrong"));
                            break;
                    }
                } else {
                    generalMsg(sender, ChatColor.RED, getLang("argwrong"));
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    void waitPlaceSignClock(Player p, int i) {
        utscmduser cu = cmduser.get(p);
        if (!cu.isSignplacing()) {
            return;
        }
        final int l = i + 1;
        if (i < 100) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> waitPlaceSignClock(p, l), 1);
        } else {
            generalMsg(p, ChatColor.RED, getLang("signsettimeout"));
            cu.setSignplacing(false);
        }
    }

    @EventHandler
    void onPlaceSign(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        utscmduser cu = cmduser.get(p);
        if (cu != null && cu.isSignplacing()) {
            Block blk = e.getBlockPlaced();
            BlockData bd = blk.getBlockData();
            e.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                blk.setType(blk.getType());
                blk.setBlockData(bd);
                editPlaceSign(cu, blk);
            }, 1);
        }
    }

    @EventHandler
    void onClickSign(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        utscmduser cu = cmduser.get(p);
        if (cu != null && cu.isSignplacing()) {
            Block blk = e.getClickedBlock();
            if (blk != null && isLocOfSign(blk.getLocation())) {
                e.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> editPlaceSign(cu, blk), 1);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> ta = new ArrayList<>();
        List<String> result = new ArrayList<>();
        int arglength = args.length;
        if (arglength == 1) {
            ta.addAll(Arrays.asList("getsign", "setdir", "setsign"));
            ta.forEach(a -> {
                if (a.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(a);
                }
            });
            return result;
        } else if (arglength == 2) {
            if (args[0].equalsIgnoreCase("setdir")) {
                ta.addAll(Arrays.asList("east", "west", "north", "south", "northeast", "northwest", "southeast", "southwest", "left", "right", "upwards", "downwards", "forward", "backwards", "none", "e", "w", "n", "s", "ne", "nw", "se", "sw", "l", "r", "u", "d", "f", "b"));
            } else {
                ta.add("");
            }
            ta.forEach(a -> {
                if (a.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(a);
                }
            });
            return result;
            // Stop spamming player names
        } else if (arglength > 2) {
            result.add("");
            return result;
        }
        return null;
    }
}