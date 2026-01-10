package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static me.fiveave.untenshi.cmds.generalMsg;
import static me.fiveave.untenshi.main.*;

class driverlog implements CommandExecutor, TabCompleter {

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (sender.isOp()) {
                if (args.length == 1) {
                    String trainname = args[0];
                    utsvehicle lv = null;
                    try {
                        lv = vehicle.get(TrainProperties.get(trainname).getHolder());
                    } catch (Exception ignored) {
                    }
                    LocalDateTime myDateObj = LocalDateTime.now();
                    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss:SS");
                    String timestamp = myDateObj.format(myFormatObj);
                    if (lv != null && !lv.isBeinglogged()) {
                        lv.setBeinglogged(true);
                        startlog(sender, lv, timestamp);
                    } else if (lv != null && lv.isBeinglogged()) {
                        lv.setBeinglogged(false);
                    } else {
                        generalMsg(sender, ChatColor.RED, getLang("driverlog_notuts"));
                    }
                } else {
                    sender.sendMessage(PURE_UTS_TITLE + ChatColor.YELLOW + "[" + getLang("help_usage") + " " + ChatColor.GOLD + "/utslogger <vehicle>" + ChatColor.YELLOW + "]");
                }
            } else {
                cmds.noPerm(sender);
            }
        } catch (Exception e) {
            generalMsg(sender, ChatColor.RED, getLang("error"));
            errorLog(e);
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> ta = new ArrayList<>();
        List<String> result = new ArrayList<>();
        List<String> vehiclelist = new ArrayList<>();
        vehicle.keySet().forEach(mg -> vehiclelist.add(mg.getProperties().getTrainName()));
        if (args.length == 1) {
            ta.addAll(vehiclelist);
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

    public void startlog(CommandSender sender, utsvehicle lv, String timestamp) throws IOException {
        create(lv, timestamp);
        logging(sender, lv, timestamp);
        generalMsg(sender, ChatColor.GREEN, getLang("driverlog_on") + ChatColor.GRAY + " (" + lv.getTrain().getProperties().getTrainName() + ")");
    }

    public void logging(CommandSender sender, utsvehicle lv, String timestamp) {
        if (lv.getTrain() != null && lv.isBeinglogged()) {
            LocalDateTime myDateObj = LocalDateTime.now();
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss.SS");
            String timestampnow = myDateObj.format(myFormatObj);
            String s = String.format("%s,%1.2f,%d,%d,%d,%d", timestampnow, lv.getSpeed(), lv.getMascon(), lv.getBrake(), lv.getSpeedlimit(), lv.getSignallimit());
            try {
                write(lv, timestamp, s);
                Bukkit.getScheduler().runTaskLater(plugin, () -> logging(sender, lv, timestamp), TICK_DELAY);
            } catch (Exception e) {
                lv.setBeinglogged(false);
                generalMsg(sender, ChatColor.RED, getLang("driverlog_off") + ChatColor.GRAY + " (" + lv.getTrain().getProperties().getTrainName() + ")");
                throw new RuntimeException(e);
            }
        } else {
            lv.setBeinglogged(false);
            generalMsg(sender, ChatColor.RED, getLang("driverlog_off") + ChatColor.GRAY + " (" + lv.getTrain().getProperties().getTrainName() + ")");
        }
    }

    public void create(utsvehicle lv, String timestamp) throws IOException {
        File dir = new File(plugin.getDataFolder().getPath() + "/driverlogs");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        File file = new File(dir, lv.getTrain().getProperties().getTrainName() + "_" + timestamp.replaceAll(":", "_") + ".csv");
        if (!file.exists() && file.createNewFile()) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write("t,v,notch,brake,splim,silim");
            bw.close();
        }
    }

    public void write(utsvehicle lv, String timestamp, String s) {
        File dir = new File(plugin.getDataFolder().getPath() + "/driverlogs");
        if (dir.exists()) {
            File file = new File(dir, lv.getTrain().getProperties().getTrainName() + "_" + timestamp.replaceAll(":", "_") + ".csv");
            if (file.exists()) {
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
                    bw.newLine();
                    bw.write(s);
                    bw.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
