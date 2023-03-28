package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartGroupStore;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.Objects;

import static me.fiveave.untenshi.cmds.*;
import static me.fiveave.untenshi.main.mascon;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.motion.freemodenoato;

class events implements Listener {

    @EventHandler
    void onPlayerClicks(PlayerInteractEvent event) {
        // Init
        Player p = event.getPlayer();
        Action ac = event.getAction();
        ItemStack item = event.getItem();
        mascon.putIfAbsent(p, -9);
        int masconstat = mascon.get(p);
        dooropen.putIfAbsent(p, 0);
        doorconfirm.putIfAbsent(p, true);
        playing.putIfAbsent(p, false);
        // Main Part
        if ((ac.equals(Action.LEFT_CLICK_AIR) || ac.equals(Action.LEFT_CLICK_BLOCK) || ac.equals(Action.RIGHT_CLICK_AIR) || ac.equals(Action.RIGHT_CLICK_BLOCK)) && item != null && playing.get(p) && !frozen.get(p)) {
            if (!atodest.containsKey(p) || atsforced.get(p) == 1) {
                if (upWand().equals(item)) {
                    if (masconstat > -9) {
                        mascon.put(p, masconstat - 1);
                    }
                    event.setCancelled(true);
                }
                if (nWand().equals(item)) {
                    if (!atsping.get(p) && !atsbraking.get(p)) {
                        mascon.put(p, 0);
                    }
                    event.setCancelled(true);
                }
                if (downWand().equals(item)) {
                    if (!atsping.get(p) && !atsbraking.get(p) && (dooropen.get(p) == 0 || (dooropen.get(p) > 0 && masconstat < 0))) {
                        if (masconstat < 5) {
                            mascon.put(p, masconstat + 1);
                        }
                    } else {
                        mascon.put(p, -9);
                    }
                    event.setCancelled(true);
                }
            }
            if (doorButton().equals(item)) {
                event.setCancelled(true);
                Boolean rev = doordiropen.get(p);
                doorControls(p, !rev);
            }
            if (sbLever().equals(item)) {
                switchback(p);
                event.setCancelled(true);
            }
            if (ebButton().equals(item)) {
                toEB(p);
                event.setCancelled(true);
            }
        }
    }

    static void toEB(Player p) {
        mascon.put(p, -9);
        atsforced.put(p, 1);
    }

    static void switchback(Player p) {
        if (p.isInsideVehicle()) {
            if (speed.get(p).equals(0.0)) {
                MinecartGroupStore.get(p.getVehicle()).reverse();
                if (atodest.containsKey(p) && atospeed.containsKey(p)) {
                    atodest.remove(p);
                    atospeed.remove(p);
                    p.sendMessage(utshead + ChatColor.GOLD + getlang("atopatterncancel"));
                }
                helpnotitle(p, ChatColor.YELLOW, getlang("sbsuccess") + ChatColor.GRAY + " (" + Objects.requireNonNull(MinecartMemberStore.getFromEntity(p.getVehicle())).getDirection() + ")");
            } else {
                helpnotitle(p, ChatColor.YELLOW, getlang("sbinmotion"));
            }
        }
    }

    static void doorControls(Player p, Boolean open) {
        doordiropen.putIfAbsent(p, false);
        if (open) {
            if (speed.get(p) > 0.0) {
                helpnotitle(p, ChatColor.YELLOW, getlang("dooropeninmotion"));
                return;
            }
            if (fixstoppos.get(p) || reqstopping.get(p)) {
                helpnotitle(p, ChatColor.YELLOW, getlang("fixstoppos"));
                return;
            }
            doordiropen.put(p, true);
            fixstoppos.put(p, false);
            doorconfirm.put(p, false);
            // Provide output when open door
            if (stopoutput.containsKey(p)) {
                Block b = p.getWorld().getBlockAt(stopoutput.get(p)[0], stopoutput.get(p)[1], stopoutput.get(p)[2]);
                b.getChunk().load();
                b.setType(Material.REDSTONE_BLOCK);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    b.setType(Material.AIR);
                    stopoutput.remove(p);
                }, 4);
            }
            // Stop penalties (If have)
            if (freemodenoato(p)) {
                // In station EB
                if (staeb.get(p)) {
                    staeb.put(p, false);
                    pointCounter(p, ChatColor.YELLOW, getlang("ebstop"), -5, "");
                }
                // In station accel
                if (staaccel.get(p)) {
                    staaccel.put(p, false);
                    pointCounter(p, ChatColor.YELLOW, getlang("reaccel"), -5, "");
                }
            }
            // ATO Stop Time Countdown, cancelled if door is closed
            atodepartcountdown(p);
        } else {
            doordiropen.put(p, false);
            reqstopping.put(p, false);
            overrun.put(p, false);
            doorconfirm.put(p, false);
        }
    }

    @EventHandler
    void collision(VehicleBlockCollisionEvent event) {
        try {
            MinecartGroup mg = MinecartGroupStore.get(event.getVehicle());
            for (String s : mg.getProperties().getOwners()) {
                Player p = Bukkit.getPlayer(s);
                if (playing.get(p) && p != null && !speed.get(p).equals(0.0)) {
                    DecimalFormat df0 = new DecimalFormat("#");
                    double spd = speed.get(p);
                    String sp = df0.format(spd);
                    toEB(p);
                    current.put(p, -480.0);
                    speed.put(p, 0.0);
                    pointCounter(p, ChatColor.YELLOW, getlang("collidebuffer"), -10, " " + sp + " km/h");
                }
            }
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    void onDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isItems(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onClickItem(InventoryClickEvent event) {
        playing.putIfAbsent((Player) event.getWhoClicked(), false);
        if (playing.get((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (isItems(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onCreativeMoveItem(InventoryCreativeEvent event) {
        ItemStack item = event.getCursor();
        if (isItems(item)) {
            event.setCancelled(true);
        }
    }

    private boolean isItems(ItemStack item) {
        return item.equals(upWand()) || item.equals(nWand()) || item.equals(downWand()) || item.equals(doorButton()) || item.equals(sbLever()) || item.equals(ebButton());
    }


    @EventHandler
        // Prevent player leaving affecting playing status
    void onLeave(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (playing.containsKey(p)) {
            if (playing.get(p)) {
                restoreinit(p);
            }
        }
    }

    // Simplify
    protected static ItemStack getItem(Material m, ChatColor color, String name) {
        ItemStack wand = new ItemStack(m, 1);
        ItemMeta wanditm = wand.getItemMeta();
        Objects.requireNonNull(wanditm).setDisplayName(color + name);
        wand.setItemMeta(wanditm);
        wand.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        return wand;
    }

    static ItemStack upWand() {
        return getItem(Material.STONE_AXE, ChatColor.RED, getlang("upwandname"));
    }

    protected static ItemStack nWand() {
        return getItem(Material.IRON_AXE, ChatColor.YELLOW, getlang("nwandname"));
    }

    protected static ItemStack downWand() {
        return getItem(Material.DIAMOND_AXE, ChatColor.GREEN, getlang("downwandname"));
    }

    protected static ItemStack doorButton() {
        return getItem(Material.IRON_TRAPDOOR, ChatColor.GOLD, getlang("doorbuttonname"));
    }

    protected static ItemStack sbLever() {
        return getItem(Material.LEVER, ChatColor.YELLOW, getlang("sblevername"));
    }

    protected static ItemStack ebButton() {
        return getItem(Material.STONE_BUTTON, ChatColor.DARK_RED, getlang("ebbuttonname"));
    }
}