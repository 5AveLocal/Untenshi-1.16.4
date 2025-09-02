package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartGroupStore;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.properties.CartProperties;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static me.fiveave.untenshi.events.*;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.utsdriver.initDriver;
import static me.fiveave.untenshi.utsvehicle.initVehicle;

class cmds implements CommandExecutor, TabCompleter {
    final String helphead = ChatColor.GOLD + "/uts ";
    final String helpformat = " " + ChatColor.WHITE + "-" + ChatColor.GREEN + " ";

    static void helpSectionTitle(CommandSender sender, ChatColor color, String arg) {
        sender.sendMessage(pureutstitle + color + " ----- " + getLang("help_" + arg + "title") + " -----");
    }

    static void generalMsg(CommandSender sender, ChatColor color, String s) {
        if (sender != null) {
            sender.sendMessage(utshead + color + s);
        }
    }

    static void generalMsg(utsdriver ld, ChatColor color, String s) {
        if (ld != null) {
            ld.getP().sendMessage(utshead + color + s);
        }
    }

    static FileConfiguration getPConfig() {
        return playerdata.dataconfig;
    }

    static void noPerm(CommandSender sender) {
        generalMsg(sender, ChatColor.RED, getLang("noperm"));
    }

    static boolean checkPerm(Player sender2, String name) {
        if (!sender2.hasPermission(name)) {
            noPerm(sender2);
            return true;
        }
        return false;
    }

    private static boolean reqActivate(utsdriver ld) {
        if (!ld.isPlaying()) {
            generalMsg(ld.getP(), ChatColor.YELLOW, getLang("activate_onfirst"));
            return true;
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                generalMsg(sender, ChatColor.RED, getLang("playeronlycmd"));
                return true;
            }
            if (!sender.hasPermission("uts.signtool") && !sender.isOp()) {
                noPerm(sender);
                return true;
            }
            // Initialize from playerdata.yml
            Player p = (Player) sender;
            // Initialize if none (not duplicating)
            initDriver(p);
            utsdriver ld = driver.get(p);
            // Force freemode false if perm not given
            if (!sender.hasPermission("uts.freemode")) {
                ld.setFreemode(false);
            }
            // Change arg to case-non-sensitive
            // For each arg need check length of arg to ensure safety
            if (args.length > 0) {
                label:
                switch (args[0].toLowerCase()) {
                    case "help":
                        if (args.length == 1) {
                            helpMsg(sender);
                            break;
                        }
                        if (args.length == 2) {
                            switch (args[1]) {
                                case "1":
                                    helpSectionTitle(sender, ChatColor.GREEN, args[1]);
                                    helpDesc(sender, getLang("help_cmdlist"));
                                    helpInfo(sender, "help <page>", getLang("help_show"));
                                    helpInfo(sender, "activate <true/false>", getLang("help_activateinfo"));
                                    helpInfo(sender, "ac", getLang("help_acinfo"));
                                    helpInfo(sender, "switchends/se", getLang("help_seinfo"));
                                    helpInfo(sender, "pa <text>", getLang("help_painfo"));
                                    helpDesc(sender, getLang("help_showdetails"));
                                    break;
                                case "2":
                                    helpSectionTitle(sender, ChatColor.LIGHT_PURPLE, args[1]);
                                    helpDesc(sender, getLang("help_cmdlist"));
                                    helpInfo(sender, "help <page>", getLang("help_show"));
                                    helpInfo(sender, "reload", getLang("help_reloadinfo"));
                                    helpInfo(sender, "freemode <true/false>", getLang("help_freemodeinfo"));
                                    helpInfo(sender, "allowato <true/false>", getLang("help_allowatoinfo"));
                                    helpDesc(sender, getLang("help_showdetails"));
                                    break;
                                case "3":
                                    helpSectionTitle(sender, ChatColor.GOLD, args[1]);
                                    String link = "https://drive.google.com/drive/folders/1GjEoksu5aTYFMZb9Pt0-qSMqssjjNsKV?usp=drive_link";
                                    sender.sendMessage(ChatColor.GREEN + getLang("help_usermanuallink") + "\n" + ChatColor.YELLOW + link);
                                    break;
                                default:
                                    generalMsg(sender, ChatColor.YELLOW, getLang("help_pagenotexist"));
                                    break;
                            }
                        }
                        break;
                    case "activate":
                        if (args.length == 2) {
                            switch (args[1].toLowerCase()) {
                                case "true":
                                    if (ld.isPlaying()) {
                                        generalMsg(sender, ChatColor.YELLOW, getLang("activate_onalready"));
                                        break label;
                                    }
                                    if (!p.isInsideVehicle()) {
                                        generalMsg(sender, ChatColor.YELLOW, getLang("activate_sitincart"));
                                        break label;
                                    }
                                    // Get train
                                    Entity selcart = p.getVehicle();
                                    MinecartGroup mg = MinecartGroupStore.get(selcart);
                                    TrainProperties tprop = mg.getProperties();
                                    // Detect owner
                                    if ((tprop.getOwners().size() == 1 && tprop.getOwners().contains(p.getName().toLowerCase()) || tprop.getOwners().isEmpty())) {
                                        // Train settings
                                        tprop.setOwner(p.getName(), true);
                                        // Set train group for player
                                        if (selcart instanceof Minecart) {
                                            MinecartMember<?> mem = MinecartMemberStore.getFromEntity(selcart);
                                            if (!selcart.getPassengers().isEmpty() && selcart.getPassengers().get(0) instanceof Player) {
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> CartProperties.setEditing((Player) selcart.getPassengers().get(0), mem.getProperties()), tickdelay);
                                            }
                                            MinecartGroup mg2 = mem.getGroup();
                                            initVehicle(mg2);
                                            utsvehicle lv = vehicle.get(mg2);
                                            ld.setLv(lv);
                                            lv.setLd(ld);
                                            lv.setTrain(mg2);
                                        }
                                        // Save inventory
                                        ld.setInv(p.getInventory().getContents());
                                        // Clear other slots
                                        for (int i = 0; i < 41; i++) {
                                            p.getInventory().setItem(i, new ItemStack(Material.AIR));
                                        }
                                        // Set wands in place
                                        if (ld.getLv().isTwohandled()) {
                                            p.getInventory().setItem(0, downWand());
                                            p.getInventory().setItem(1, upWand());
                                            p.getInventory().setItem(2, nWand());
                                            p.getInventory().setItem(3, leftWand());
                                            p.getInventory().setItem(4, rightWand());
                                        } else {
                                            p.getInventory().setItem(0, upWand());
                                            p.getInventory().setItem(1, nWand());
                                            p.getInventory().setItem(2, downWand());
                                        }
                                        p.getInventory().setItem(6, ebButton());
                                        p.getInventory().setItem(7, sbLever());
                                        p.getInventory().setItem(8, doorButton());
                                        // Playing = true
                                        ld.setPlaying(true);
                                        motion.recursiveClockLd(ld);
                                        generalMsg(ld.getP(), ChatColor.YELLOW, getLang("activate_set"));
                                        generalMsg(ld.getP(), ChatColor.YELLOW, getLang("activate") + " " + ChatColor.GREEN + getLang("activate_on"));
                                    } else {
                                        generalMsg(ld.getP(), ChatColor.RED, getLang("activate_notowner"));
                                    }
                                    break label;
                                case "false":
                                    if (ld.isPlaying()) {
                                        restoreInitLd(ld);
                                    } else {
                                        generalMsg(sender, ChatColor.YELLOW, getLang("activate_offalready"));
                                    }
                                    break label;
                            }
                        }
                        sender.sendMessage(pureutstitle + ChatColor.YELLOW + "[" + getLang("help_usage") + " " + ChatColor.GOLD + "/uts activate <true/false>" + ChatColor.YELLOW + "]\n" + getLang("activate_info1") + "\n" + getLang("activate_info2"));
                        break;
                    case "ac":
                        if (reqActivate(ld)) {
                            generalMsg(sender, ChatColor.YELLOW, getLang("activate_sitincart"));
                            break;
                        }
                        if (ld.getLv().getSignallimit() != 0 && ld.getLv().getAtsforced() == 2) {
                            ld.getLv().setAtsforced(0);
                            generalMsg(sender, ChatColor.GOLD, ld.getLv().getSafetysystype().toUpperCase() + " " + getLang("ac_success"));
                        } else if (ld.getLv().getSignallimit() == 0) {
                            generalMsg(sender, ChatColor.RED, ld.getLv().getSafetysystype().toUpperCase() + " " + getLang("ac_failed"));
                        } else {
                            generalMsg(sender, ChatColor.YELLOW, ld.getLv().getSafetysystype().toUpperCase() + " " + getLang("ac_noneed"));
                        }
                        break;
                    case "freemode":
                        if (reqDeactivate(ld)) break;
                        if (cannotSetTrain(args, ld) || !args[1].equalsIgnoreCase("true") && !args[1].equalsIgnoreCase("false")) {
                            sender.sendMessage(pureutstitle + ChatColor.YELLOW + "[" + getLang("help_usage") + " " + ChatColor.GOLD + "/uts freemode <true/false>" + ChatColor.YELLOW + "]\n" + getLang("freemode_info1") + "\n" + getLang("freemode_info2"));
                        } else {
                            ld.setFreemode(Boolean.parseBoolean(args[1].toLowerCase()));
                            sender.sendMessage(pureutstitle + ChatColor.YELLOW + getLang("freemode") + " " + (ld.isFreemode() ? ChatColor.GREEN + getLang("activate_on") : ChatColor.RED + getLang("activate_off")));
                        }
                        break;
                    case "allowato":
                        if (reqDeactivate(ld)) break;
                        if (cannotSetTrain(args, ld) || !args[1].equalsIgnoreCase("true") && !args[1].equalsIgnoreCase("false")) {
                            sender.sendMessage(pureutstitle + ChatColor.YELLOW + "[" + getLang("help_usage") + " " + ChatColor.GOLD + "/uts allowato <true/false>" + ChatColor.YELLOW + "]\n" + getLang("ato_info1") + "\n" + getLang("ato_info2"));
                        } else {
                            ld.setAllowatousage(Boolean.parseBoolean(args[1].toLowerCase()));
                            sender.sendMessage(pureutstitle + ChatColor.YELLOW + getLang("ato") + " " + (ld.isAllowatousage() ? ChatColor.GREEN + getLang("activate_on") : ChatColor.RED + getLang("activate_off")));
                        }
                        break;
                    case "switchends":
                    case "se":
                        if (reqActivate(ld)) break;
                        if (ld.getLv().getSpeed() != 0) {
                            generalMsg(sender, ChatColor.YELLOW, getLang("se_inmotion"));
                            break;
                        }
                    {
                        Entity selcart = p.getVehicle();
                        MinecartGroup mg = MinecartGroupStore.get(selcart);
                        MinecartMember<?> mm = MinecartMemberStore.getFromEntity(selcart);
                        MinecartMember<?> mm2 = mm;
                        assert mm != null;
                        // Check if head or tail is not current cart and they are minecarts
                        if ((mg.head() != mm || mg.tail() != mm) && !ld.isFrozen()) {
                            // Must clear passenger in cart, or else player will bug out
                            if (mg.head() != mm) {
                                mm2 = mg.head();
                                mm2.eject();
                            } else if (mg.tail() != mm) {
                                mm2 = mg.tail();
                                mm2.eject();
                            }
                            if (mm2.getEntity().getEntity() instanceof RideableMinecart) {
                                // MUST wait after eject, and teleport, and finally enter, or else will bug
                                ld.setFrozen(true);
                                mm.eject();
                                MinecartMember<?> finalMm2 = mm2;
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    ld.getP().teleport(finalMm2.getEntity().getLocation());
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        finalMm2.addPassengerForced(ld.getP());
                                        generalMsg(ld.getP(), ChatColor.YELLOW, getLang("se_success"));
                                        ld.setFrozen(false);
                                        ld.getLv().setDriverseat(finalMm2);
                                    }, tickdelay);
                                }, tickdelay);
                                break;
                            }
                            generalMsg(sender, ChatColor.RED, getLang("se_failed"));
                        }
                    }
                    break;
                    case "pa":
                        if (checkPerm(p, "uts.pa")) break;
                        if (reqActivate(ld)) break;
                    {
                        MinecartGroup mg = MinecartGroupStore.get(p.getVehicle());
                        mg.forEach(mm -> {
                            if (!mm.getEntity().getPassengers().isEmpty()) {
                                Player p2 = (Player) mm.getEntity().getPassengers().get(0);
                                String s;
                                int arglength = args.length;
                                if (arglength > 1 && args[1] != null) {
                                    StringBuilder sBuilder = null;
                                    for (int i = 1; i < arglength; i++) {
                                        if (sBuilder != null) {
                                            sBuilder.append(" ").append(args[i]);
                                        } else {
                                            sBuilder = new StringBuilder(args[i]);
                                        }
                                    }
                                    s = sBuilder.toString();
                                    // To keep & type \&
                                    s = s.replaceAll("\\\\&", "\\\\and");
                                    s = s.replaceAll("&", "§");
                                    s = s.replaceAll("\\\\and", "&");
                                    generalMsg(p2, ChatColor.YELLOW, getLang("help_painfo") + ": " + s);
                                } else {
                                    generalMsg(p, ChatColor.RED, getLang("panoempty"));
                                }
                            }
                        });
                    }
                    break;
                    case "reload":
                        if (checkPerm(p, "uts.reload")) break;
                        plugin.reloadConfig();
                        config = new abstractfile(plugin, "config.yml");
                        traindata = new abstractfile(plugin, "traindata.yml");
                        signalorder = new abstractfile(plugin, "signalorder.yml");
                        langdata = new abstractfile(plugin, "lang_" + plugin.getConfig().getString("lang") + ".yml");
                        generalMsg(sender, ChatColor.YELLOW, getLang("reloaded"));
                        break;
                    default:
                        generalMsg(sender, ChatColor.YELLOW, getLang("cmdnotexist"));
                        break;
                }
            } else {
                helpMsg(sender);
            }
            // Save in config
            getPConfig().set("players." + p.getUniqueId() + ".freemode", ld.isFreemode());
            getPConfig().set("players." + p.getUniqueId() + ".allowatousage", ld.isAllowatousage());
            playerdata.save();
        } catch (Exception e) {
            generalMsg(sender, ChatColor.RED, getLang("error"));
            errorLog(e, "cmds.onCommand");
        }
        return true;
    }

    static void errorLog(Exception e, String loc) {
        plugin.getLogger().log(Level.SEVERE, utshead + loc + "() did not function properly!\n");
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
    }

    private boolean cannotSetTrain(String[] args, utsdriver ld) {
        return checkPerm(ld.getP(), "uts." + args[0].toLowerCase()) || args.length != 2;
    }

    private boolean reqDeactivate(utsdriver ld) {
        if (ld.isPlaying()) {
            generalMsg(ld.getP(), ChatColor.YELLOW, getLang("activate_offfirst"));
            return true;
        }
        return false;
    }

    void helpDesc(CommandSender sender, String s) {
        sender.sendMessage(ChatColor.YELLOW + s);
    }

    void helpInfo(CommandSender sender, String s, String t) {
        sender.sendMessage(helphead + s + helpformat + t);
    }

    void helpMsg(CommandSender sender) {
        sender.sendMessage(pureutstitle + helphead + "help <page>" + helpformat + getLang("help_show"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> ta = new ArrayList<>();
        List<String> result = new ArrayList<>();
        int arglength = args.length;
        if (arglength == 1) {
            ta.addAll(Arrays.asList("help", "activate", "ac", "switchends", "se", "freemode", "reload", "pa", "allowato"));
            ta.forEach(a -> {
                if (a.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(a);
                }
            });
            return result;
        } else if (arglength == 2) {
            switch (args[0].toLowerCase()) {
                case "help":
                    ta.addAll(Arrays.asList("1", "2", "3"));
                    break;
                case "activate":
                case "freemode":
                case "allowato":
                    ta.addAll(Arrays.asList("true", "false"));
                    break;
                default:
                    ta.add("");
                    break;
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