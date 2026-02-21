package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartGroupStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Objects;

import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;
import static me.fiveave.untenshi.motion.noFreemodeOrATO;
import static me.fiveave.untenshi.utsdriver.initDriver;

class events implements Listener {

    static void toEB(utsvehicle lv) {
        if (lv.getLd() != null && noFreemodeOrATO(lv.getLd()) && lv.getBrake() != 9 && lv.getSpeed() > 20 && lv.getAtsforced() != 2 && lv.getAtsping() == 0) {
            // Misuse EB
            pointCounter(lv.getLd(), ChatColor.YELLOW, getLang("eb_misuse") + " ", -5, "");
        }
        lv.setMascon(0);
        lv.setBrake(9);
        lv.setAtsforced(1);
        trainSound(lv, "ebbutton");
    }

    static void toB8(utsvehicle lv) {
        lv.setMascon(0);
        lv.setBrake(8);
    }

    static void switchBack(utsvehicle lv) {
        utsdriver ld = lv.getLd();
        if (lv.getSpeed() == 0) {
            MinecartGroup mg = lv.getTrain();
            mg.reverse();
            if (lv.getAtodest() != null && lv.getAtospeed() != -1) {
                lv.setAtodest(null);
                lv.setAtospeed(-1);
                generalMsg(ld.getP(), ChatColor.GOLD, getLang("ato_patterncancel"));
            }
            String dirtext = lv.getTrain().head().equals(lv.getDriverseat()) ? getLang("dir_front") : getLang("dir_back");
            generalMsg(ld.getP(), ChatColor.YELLOW, getLang("sb_success") + ChatColor.GRAY + " (" + dirtext + " / " + getLang("dir_" + mg.head().getDirection().toString().toLowerCase()) + ")");
            trainSound(lv, "sblever");
        } else {
            generalMsg(ld.getP(), ChatColor.YELLOW, getLang("sb_inmotion"));
        }
    }

    static void doorControls(utsvehicle lv, Boolean open) {
        utsdriver ld = lv.getLd();
        if (open) {
            if (lv.getSpeed() > 0.0) {
                generalMsg(ld, ChatColor.YELLOW, getLang("door_openinmotion"));
                return;
            }
            if (lv.isFixstoppos() || lv.isReqstopping()) {
                generalMsg(ld, ChatColor.YELLOW, getLang("stoppos_reqfix"));
                return;
            }
            lv.setDoordiropen(true);
            lv.setDoorconfirm(false);
        } else {
            lv.setDoordiropen(false);
            lv.setDoorconfirm(false);
        }
        trainSound(lv, "doorbutton");
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

    protected static ItemStack upWand() {
        return getItem(Material.STONE_AXE, ChatColor.RED, getLang("item_upwand"));
    }

    protected static ItemStack nWand() {
        return getItem(Material.IRON_AXE, ChatColor.YELLOW, getLang("item_nwand"));
    }

    protected static ItemStack downWand() {
        return getItem(Material.DIAMOND_AXE, ChatColor.GREEN, getLang("item_downwand"));
    }

    protected static ItemStack leftWand() {
        return getItem(Material.DIAMOND_SHOVEL, ChatColor.GREEN, getLang("item_leftwand"));
    }

    protected static ItemStack rightWand() {
        return getItem(Material.STONE_SHOVEL, ChatColor.RED, getLang("item_rightwand"));
    }

    protected static ItemStack doorButton() {
        return getItem(Material.IRON_TRAPDOOR, ChatColor.GOLD, getLang("item_doorbutton"));
    }

    protected static ItemStack sbLever() {
        return getItem(Material.LEVER, ChatColor.YELLOW, getLang("item_sblever"));
    }

    protected static ItemStack ebButton() {
        return getItem(Material.STONE_BUTTON, ChatColor.DARK_RED, getLang("item_ebbutton"));
    }

    private static void downWandAction(utsvehicle lv) {
        // No ATS-P run or ATS-P Service brake run but in EB
        if ((lv.getAtsping() == 0 || (lv.getAtsping() == 1 && lv.getBrake() == 9)) && (lv.getDooropen() == 0 || lv.getDooropen() > 0 && lv.getBrake() > 0)) {
            if (lv.getMascon() < 5 && (lv.isTwohandled() || lv.getBrake() == 0)) {
                lv.setMascon(lv.getMascon() + 1);
            } else if (!lv.isTwohandled() && lv.getBrake() > 0) {
                lv.setBrake(lv.getBrake() - 1);
            }
            lv.setAtsforced(0);
            trainSound(lv, "mascon");
        }
    }

    private static void nWandAction(utsvehicle lv) {
        if (lv.getAtsping() == 0) {
            lv.setBrake(0);
            lv.setMascon(0);
            lv.setAtsforced(0);
            trainSound(lv, "mascon");
        }
    }

    private static void upWandAction(utsvehicle lv) {
        if (lv.getMascon() > 0) {
            lv.setMascon(lv.getMascon() - 1);
        } else if (!lv.isTwohandled()) {
            if (lv.getBrake() < 8) {
                lv.setBrake(lv.getBrake() + 1);
            } else {
                toEB(lv);
                trainSound(lv, "ebbutton");
            }
        }
        trainSound(lv, "mascon");
    }

    private static void leftWandAction(utsvehicle lv) {
        // No ATS-P run or ATS-P Service brake run but in EB
        if ((lv.getAtsping() == 0 || (lv.getAtsping() == 1 && lv.getBrake() == 9)) && lv.getBrake() > 0) {
            lv.setBrake(lv.getBrake() - 1);
            lv.setAtsforced(0);
            trainSound(lv, "mascon");
        }
    }

    private static void rightWandAction(utsvehicle lv) {
        if (lv.getBrake() < 8) {
            lv.setBrake(lv.getBrake() + 1);
            trainSound(lv, "mascon");
        } else if (lv.getBrake() == 8) {
            toEB(lv);
            trainSound(lv, "ebbutton");
        }
    }

    static void trainSound(utsvehicle lv, String type) {
        if (lv.getTrain() != null) {
            // Divide by at least 32
            double cartlen = lv.getTrain().head().getProperties().getModel().getCartLength();
            switch (type) {
                case "brake_apply":
                    lv.getTrain().forEach(mm -> lv.getSavedworld().playSound(mm.getEntity().getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, (float) cartlen / 80, 0.75f));
                    break;
                case "brake_release":
                    lv.getTrain().forEach(mm -> lv.getSavedworld().playSound(mm.getEntity().getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, (float) cartlen / 80, 1.5f));
                    break;
                case "accel_on":
                    lv.getTrain().forEach(mm -> lv.getSavedworld().playSound(mm.getEntity().getLocation(), Sound.BLOCK_PISTON_EXTEND, (float) cartlen / 160, 2f));
                    break;
                case "accel_off":
                    lv.getTrain().forEach(mm -> lv.getSavedworld().playSound(mm.getEntity().getLocation(), Sound.BLOCK_PISTON_CONTRACT, (float) cartlen / 160, 2f));
                    break;
                case "mascon":
                    if (lv.getLd() != null) {
                        lv.getLd().getP().playSound(lv.getLd().getP().getLocation(), Sound.BLOCK_WOOD_PLACE, 0.5f, 1.5f);
                    }
                    break;
                case "ebbutton":
                    if (lv.getLd() != null) {
                        lv.getLd().getP().playSound(lv.getLd().getP().getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 0.5f, 0.75f);
                    }
                    break;
                case "sblever":
                    if (lv.getLd() != null) {
                        lv.getLd().getP().playSound(lv.getLd().getP().getLocation(), Sound.BLOCK_FENCE_GATE_CLOSE, 0.5f, 1.25f);
                    }
                    break;
                case "doorbutton":
                    if (lv.getLd() != null) {
                        lv.getLd().getP().playSound(lv.getLd().getP().getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 0.5f, 1.5f);
                    }
                    break;
            }
        }
    }

    @EventHandler
    void onPlayerClicks(PlayerInteractEvent event) {
        // Init
        Player p = event.getPlayer();
        initDriver(p);
        Action ac = event.getAction();
        ItemStack item = event.getItem();
        utsdriver ld = driver.get(p);
        utsvehicle lv = ld.getLv();
        // Main Part
        if ((ac.equals(Action.LEFT_CLICK_AIR) || ac.equals(Action.LEFT_CLICK_BLOCK) || ac.equals(Action.RIGHT_CLICK_AIR) || ac.equals(Action.RIGHT_CLICK_BLOCK)) && item != null && ld.isPlaying() && !ld.isFrozen()) {
            // Either not in ATO mode, or ATO mode with EB
            if ((lv.getAtodest() == null || lv.getAtsforced() == 1) && lv.getAtsforced() != 2) {
                if (upWand().equals(item)) {
                    upWandAction(lv);
                    event.setCancelled(true);
                }
                if (nWand().equals(item)) {
                    nWandAction(lv);
                    event.setCancelled(true);
                }
                if (downWand().equals(item)) {
                    downWandAction(lv);
                    event.setCancelled(true);
                }
                if (leftWand().equals(item)) {
                    leftWandAction(lv);
                    event.setCancelled(true);
                }
                if (rightWand().equals(item)) {
                    rightWandAction(lv);
                    event.setCancelled(true);
                }
            }
            if (doorButton().equals(item)) {
                event.setCancelled(true);
                boolean rev = lv.isDoordiropen();
                doorControls(lv, !rev);
            }
            if (sbLever().equals(item)) {
                switchBack(lv);
                event.setCancelled(true);
            }
            if (ebButton().equals(item) && lv.getAtsforced() != 2) {
                toEB(lv);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    void collision(VehicleBlockCollisionEvent event) {
        try {
            MinecartGroup mg = MinecartGroupStore.get(event.getVehicle());
            mg.getProperties().getOwners().forEach(s -> {
                Player p = Bukkit.getPlayer(s);
                utsdriver ld = driver.get(p);
                utsvehicle lv = ld.getLv();
                if (lv != null && ld.isPlaying() && lv.getSpeed() != 0) {
                    double spd = lv.getSpeed();
                    toEB(lv);
                    lv.setCurrent(0);
                    lv.setBcpressure(480);
                    lv.setSpeed(0);
                    pointCounter(ld, ChatColor.YELLOW, getLang("collidebuffer") + " ", -10, " " + String.format("%.0f km/h", spd));
                }
            });
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
        Player p = (Player) event.getWhoClicked();
        initDriver(p);
        utsdriver ld = driver.get(p);
        if (ld.isPlaying()) {
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
        ItemStack[] is = new ItemStack[]{upWand(), nWand(), downWand(), leftWand(), rightWand(), doorButton(), sbLever(), ebButton()};
        return Arrays.asList(is).contains(item);
    }

    @EventHandler
    void onRotateItemFrame(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        Entity e = event.getRightClicked();
        utsdriver ld = driver.get(p);
        if (ld != null && ld.isPlaying() && e.getType().equals(EntityType.ITEM_FRAME)) {
            utsvehicle lv = ld.getLv();
            if (lv != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    void onDamageItemFrame(EntityDamageByEntityEvent event) {
        Entity e1 = event.getDamager();
        Entity e2 = event.getEntity();
        if (e1.getType().equals(EntityType.PLAYER)) {
            Player p = (Player) e1;
            utsdriver ld = driver.get(p);
            if (ld != null && ld.isPlaying() && e2.getType().equals(EntityType.ITEM_FRAME)) {
                utsvehicle lv = ld.getLv();
                if (lv != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
        // Prevent player leaving affecting playing status
    void onLeave(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        initDriver(p);
        utsdriver ld = driver.get(p);
        if (ld != null && ld.isPlaying()) {
            restoreInitLd(ld);
        }
        driver.remove(p);
    }

    @EventHandler
        // Inventory retrieval after crash
    void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String uuid = p.getUniqueId().toString();
        // If have inventory data on joining (due to crash occurred before)
        if (invdata.dataconfig.contains(uuid)) {
            initDriver(p);
            utsdriver ld = driver.get(p);
            retrieveInv(ld);
        }
    }
}