package me.fiveave.untenshi;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Integer.parseInt;
import static me.fiveave.untenshi.main.getlang;
import static me.fiveave.untenshi.main.utshead;
import static me.fiveave.untenshi.signalsign.*;

class signalcmd implements CommandExecutor, TabCompleter {

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                int signalspeed = 0;
                if (args[3].equals("sign")) {
                    signalspeed = parseInt(args[5]);
                }
                // Anti returning wrong arguments
                String e;
                String cartevent = args[0] + " " + args[1] + " " + args[2];
                if (args.length == 4) {
                    e = args[3];
                } else if (args.length == 6) {
                    e = args[3] + " " + args[4] + " " + args[5];
                } else {
                    e = null;
                }
                // Main content starts here
                if (signalspeed > 360) {
                    sender.sendMessage(utshead + getlang("signalmax360"));
                    return true;
                }
                if (signalspeed < 0) {
                    sender.sendMessage(utshead + getlang("signalmin0"));
                    return true;
                }
                if (Math.floorMod(signalspeed, 5) != 0) {
                    sender.sendMessage(utshead + getlang("signaldiv5"));
                    return true;
                }
                assert e != null;
                if ((l1(e).equals("warn") || l1(e).equals("sign"))) {
                    String signalmsg = "";
                    switch (args[3]) {
                        // Signal speed limit warn
                        case "warn":
                            Sign warn = getSign(sender, cartevent);
                            if (warn.getLine(1).equals("signalsign")) {
                                // lastsisign and lastsisp are for detecting signal change
                                String warnsi = warn.getLine(2).split(" ")[1];
                                String warnsp = warn.getLine(2).split(" ")[2];
                                signalmsg = signalName(warnsi, signalmsg);
                                if (signalmsg.equals("")) {
                                    sender.sendMessage(utshead + ChatColor.RED + getlang("signaltypewrong"));
                                }
                                String temp2 = warnsp + " km/h";
                                if (parseInt(warnsp) >= 360) {
                                    temp2 = getlang("nolimit");
                                }
                                sender.sendMessage(utshead + ChatColor.YELLOW + getlang("signalwarn") + signalmsg + ChatColor.GRAY + " " + temp2);
                            }
                            break;
                        // Set line 4 of sign at (line 3 of this sign) to turn signal
                        case "sign":
                            if (issignaltype(args[4])) {
                                Sign sign = getSign(sender, cartevent);
                                updateSignals(sign, "set " + args[4] + " " + args[5]);
                                sender.sendMessage(utshead + getlang("signalsignchange") + " (" + args[4] + " " + args[5] + ")");
                                break;
                            }
                        default:
                            sender.sendMessage(utshead + getlang("signalargwrong2"));
                            break;
                    }
                }
            } else {
                sender.sendMessage(utshead + getlang("cmdblkonlycmd"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    // Simplify

    // l[n]: "n"th split text in line 3
    String l1(String e) {
        return e.toLowerCase().split(" ")[0];
    }

    int getloc(String loctext, int i) {
        return parseInt(loctext.split(" ")[i]);
    }

    Sign getSign(CommandSender sender, String loctext) {
        World w = sender instanceof Player ? ((Player) sender).getWorld() : (sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getWorld() : null);
        assert w != null;
        BlockState bl = w.getBlockAt(getloc(loctext, 0), getloc(loctext, 1), getloc(loctext, 2)).getState();
        return bl instanceof Sign ? (Sign) bl : null;
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
            ta.addAll(Arrays.asList("warn", "sign"));
        } else if (args.length == 5 && args[3].equals("sign")) {
            ta.addAll(Arrays.asList("r", "yy", "y", "yg", "g", "atc"));
        } else {
            ta.add("");
        }
        for (String a : ta) {
            if (a.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                result.add(a);
            }
        }
        return result;
    }
}