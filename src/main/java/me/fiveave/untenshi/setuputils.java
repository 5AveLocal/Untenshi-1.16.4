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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;

import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.cmds.noPerm;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.signcache.getSignFromLoc;
import static me.fiveave.untenshi.signcache.isLocOfSign;
import static me.fiveave.untenshi.utscmduser.initCmdUser;

class setuputils implements CommandExecutor, TabCompleter, Listener {

    protected static HashMap<Player, ArrayList<String>> book1 = new HashMap<>();
    protected static HashMap<Player, ArrayList<String>> book2 = new HashMap<>();

    final List<String> alldirlist = Arrays.asList("b", "backwards", "d", "downwards", "e", "east", "f", "forward",
            "l", "left", "n", "ne", "none", "north", "northeast", "northwest", "nw", "r", "right",
            "s", "se", "south", "southeast", "southwest", "sw", "u", "upwards", "w", "west");

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
            if (!sender.hasPermission("uts.setuputils") && !sender.isOp()) {
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
                            boolean matchdir = alldirlist.contains(args[1].toLowerCase());
                            if (matchdir) {
                                cu.setSigndir(args[1].toLowerCase());
                                generalMsg(p, ChatColor.GREEN, getLang("signdirset"));
                            } else {
                                generalMsg(p, ChatColor.RED, getLang("signdirwrong"));

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
                    case "book":
                        switch (args[1]) {
                            case "addloc":
                                ItemStack book = p.getInventory().getItemInMainHand();
                                if (book.getType().equals(Material.WRITABLE_BOOK)) {
                                    BookMeta bm = (BookMeta) book.getItemMeta();
                                    List<String> pages;
                                    if (bm != null) {
                                        pages = new ArrayList<>(bm.getPages());
                                        Location loc2;
                                        if (args.length == 5) {
                                            pages.add(args[2] + " " + args[3] + " " + args[4]);
                                            loc2 = new Location(p.getWorld(), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                                        } else {
                                            Block bb = p.getTargetBlock(Collections.singleton(Material.AIR), 5);
                                            pages.add(bb.getX() + " " + bb.getY() + " " + bb.getZ());
                                            loc2 = bb.getLocation();
                                        }
                                        bm.setPages(pages);
                                        book.setItemMeta(bm);
                                        p.getInventory().setItem(p.getInventory().getHeldItemSlot(), book);
                                        boolean isbbofsign = isLocOfSign(loc2);
                                        p.sendMessage(UTS_HEAD + ChatColor.YELLOW + getLang("bookaddloc1")
                                                + " (" + bm.getPageCount() + " " + getLang("bookaddloc2") + " - "
                                                + (isbbofsign ? ChatColor.GOLD + getLang("bookaddlocsign")
                                                : ChatColor.GREEN + getLang("bookaddlocother")) + ChatColor.YELLOW + ") ");
                                    }
                                }
                                break;
                            case "combine":
                                switch (args[2]) {
                                    case "sel1":
                                        ItemStack is1 = p.getInventory().getItemInMainHand();
                                        ItemStack selbook1;
                                        if (is1.getType().equals(Material.WRITABLE_BOOK)) {
                                            selbook1 = is1;
                                            BookMeta bm = (BookMeta) selbook1.getItemMeta();
                                            assert bm != null;
                                            book1.put(p, new ArrayList<>(bm.getPages()));
                                            p.sendMessage(UTS_HEAD + ChatColor.YELLOW + getLang("bookcombinesel1"));
                                        }
                                        break;
                                    case "sel2":
                                        ItemStack is2 = p.getInventory().getItemInMainHand();
                                        ItemStack selbook2;
                                        if (is2.getType().equals(Material.WRITABLE_BOOK)) {
                                            selbook2 = is2;
                                            BookMeta bm = (BookMeta) selbook2.getItemMeta();
                                            assert bm != null;
                                            book2.put(p, new ArrayList<>(bm.getPages()));
                                            p.sendMessage(UTS_HEAD + ChatColor.YELLOW + getLang("bookcombinesel2"));
                                        }
                                        break;
                                    case "confirm":
                                        if (book1.containsKey(p) && book2.containsKey(p)) {
                                            ItemStack newbook = new ItemStack(Material.WRITABLE_BOOK, 1);
                                            BookMeta bm = (BookMeta) newbook.getItemMeta();
                                            assert bm != null;
                                            ArrayList<String> pages = new ArrayList<>();
                                            pages.addAll(book1.get(p));
                                            pages.addAll(book2.get(p));
                                            bm.setPages(pages);
                                            newbook.setItemMeta(bm);
                                            p.getInventory().addItem(newbook);
                                            p.sendMessage(UTS_HEAD + ChatColor.YELLOW + getLang("bookcombineconfirm"));
                                        }
                                        break;
                                }
                                break;
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
            errorLog(e);
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
        switch (arglength) {
            case 1:
                ta.addAll(Arrays.asList("getsign", "setdir", "setsign", "book"));
                ta.forEach(a -> {
                    if (a.toLowerCase().startsWith(args[0].toLowerCase())) {
                        result.add(a);
                    }
                });
                return result;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "setdir":
                        ta.addAll(alldirlist);
                        break;
                    case "book":
                        ta.addAll(Arrays.asList("addloc", "combine"));
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
            case 3:
                switch (args[1].toLowerCase()) {
                    case "addloc":
                        Player p = (Player) sender;
                        Block block = p.getTargetBlock(Collections.singleton(Material.AIR), 5);
                        ta.add(block.getX() + " " + block.getY() + " " + block.getZ());
                        break;
                    case "combine":
                        ta.addAll(Arrays.asList("sel1", "sel2", "confirm"));
                        break;
                    default:
                        ta.add("");
                        break;
                }
                ta.forEach(a -> {
                    if (a.toLowerCase().startsWith(args[2].toLowerCase())) {
                        result.add(a);
                    }
                });
                return result;
            // Stop spamming player names
            default:
                result.add("");
                return result;
        }
    }
}