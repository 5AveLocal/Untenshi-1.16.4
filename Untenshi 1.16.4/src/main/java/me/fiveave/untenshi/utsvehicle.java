package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.Set;

import static me.fiveave.untenshi.main.*;

class utsvehicle {
    private double accel; // Acceleration from traindata.yml
    private double decel; // Deceleration from traindata.yml
    private double ebdecel; // EB Deceleration from traindata.yml
    private double speeddrop; // Natural speed drop rate
    private int[] speedsteps; // Speed steps for acceleration and deceleration
    private utsdriver ld; // Driver
    private World savedworld; // World
    private int mascon; // Master controller notch
    private int brake; // Brake handle notch
    private int speedlimit; // Speed limit
    private int signallimit; // Signal speed limit
    private int atsforced; // ATS (ATC) forced status (-1: TrainCarts forced stop, 0: normal, 1: EB applied, 2: ATS Run)
    private int lastsisp; // Speed limit of last recognized signal sign
    private int lastspsp; // Speed limit of last recognized speed limit sign
    private int dooropen; // Door open status (0 (closed) - 30 (open))
    private int[] stopoutput; // Redstone output position after stopping at station
    private int atostoptime; // ATO stopping time at station
    /*  rs = "resettable sign" means signal signs that will be reset after train moves out of signal blocks
        New entries for rs are added after train passes a new signal, and old entries are removed when train at back occupies those positions
        il = "interlock" = interlocking
        New entries for il are first added into ilposlist for queue, then added into ilposoccupied once path is clear,
        but ilposlist and ilposoccupied will not be removed, until whole train passes the signal corresponding to them
        Diagram for "resettable sign" and interlocking positions
                   rs    ------>          il
        <--- ... 3 2 1 0 [Train] 0 1 2 3 ... --->
     */
    private int rsoccupiedpos; // Furthest occupied position in rsposlist
    private int ilpriority; // Priority for interlocking
    private double current; // Electric current
    private double bcpressure; // Brake cylinder pressure
    private double speed; // Train speed
    private double atospeed; // ATO target speed
    private Location stoppos; // Stop position at station
    private Location stopactionpos; // Stop action chest reference position at station
    private Location atodest; // ATO target destination
    private Location lastsisign; // Location of last recognized signal sign
    private Location lastspsign; // Location of last recognized speed limit sign
    private Location lastilchest; // Location of last recognized interlocking chest
    private Location[] rsposlist; // List of positions for "resettable sign"
    private Location[] ilposlist; // List of positions (including both in queue and occupied) for interlocking
    private Location[] ilposoccupied; // Occupied positions for interlocking
    private String safetysystype; // Type of safety system (ATS-P / ATC)
    private String signalorderptn; // Signal order pattern
    private boolean reqstopping; // Stopping at station required
    private boolean overrun; // Overrun
    private boolean fixstoppos; // Fixing stop position needed
    private boolean staaccel; // In-station acceleration
    private boolean staeb; // In-station EB applications
    private int atsping; // ATS-P (ATC) brake run (0: none, 1: B8, 2: EB)
    private boolean atspnear; // ATS-P (ATC) pattern near
    private boolean doordiropen; // Door opening
    private boolean doorconfirm; // Door open / close status confirmed
    private boolean atopisdirect; // ATO Pattern is Direct Pattern
    private boolean atoforcebrake; // ATO brake is forced
    private boolean atoforceslopebrake; // ATO brake on slope is forced
    private boolean atoautodep; // ATO auto departure after doors closed
    private boolean beinglogged; // Train being logged
    private boolean twohandled; // Train is two-handled
    private MinecartGroup train; // MinecartGroup of this utsvehicle
    @SuppressWarnings("rawtypes")
    private MinecartMember driverseat; // Cart belonging to driver seat
    private long ilenterqueuetime; // Enter queue time for interlocking

    utsvehicle(MinecartGroup mg) {
        try {
            this.setTrain(mg);
            this.setSavedworld(mg.getWorld());
            this.setDriverseat(mg.head());
        } catch (Exception ignored) {
        }
        // Set accel, decel and speedsteps
        // Init train
        // From traindata (if available)
        String seltrainname = "";
        Set<String> allTrains = Objects.requireNonNull(traindata.dataconfig.getConfigurationSection("trains")).getKeys(false);
        // Choose most suitable type
        for (String tname : allTrains) {
            // Override config accels
            if (mg.getProperties().getDisplayName().contains(tname) && tname.length() > seltrainname.length()) {
                seltrainname = tname;
            }
        }
        // Set as default if none
        if (seltrainname.isEmpty()) {
            seltrainname = "default";
        }
        double tempaccel = 0;
        double tempdecel = 0;
        double tempebdecel = 0;
        int[] tempspeedsteps = new int[6];
        boolean twohandled = false;
        String tDataInfo = "trains." + seltrainname;
        if (traindata.dataconfig.contains(tDataInfo + ".accel"))
            tempaccel = traindata.dataconfig.getDouble(tDataInfo + ".accel");
        if (traindata.dataconfig.contains(tDataInfo + ".decel"))
            tempdecel = traindata.dataconfig.getDouble(tDataInfo + ".decel");
        if (traindata.dataconfig.contains(tDataInfo + ".ebdecel"))
            tempebdecel = traindata.dataconfig.getDouble(tDataInfo + ".ebdecel");
        if (traindata.dataconfig.contains(tDataInfo + ".speeds") && traindata.dataconfig.getIntegerList(tDataInfo + ".speeds").size() == 6) {
            for (int i = 0; i < 6; i++) {
                tempspeedsteps[i] = traindata.dataconfig.getIntegerList(tDataInfo + ".speeds").get(i);
            }
        }
        if (traindata.dataconfig.contains(tDataInfo + ".twohandled")) {
            twohandled = traindata.dataconfig.getBoolean(tDataInfo + ".twohandled");
        } else {
            traindata.dataconfig.set(tDataInfo + ".twohandled", false);
            traindata.save();
        }
        this.setSpeeddrop(plugin.getConfig().getDouble("speeddroprate"));
        this.setAccel(tempaccel);
        this.setDecel(tempdecel);
        this.setEbdecel(tempebdecel);
        this.setSpeedsteps(tempspeedsteps);
        this.setTwohandled(twohandled);
        this.setSpeed(0.0);
        this.setSignallimit(maxspeed);
        this.setSpeedlimit(maxspeed);
        this.setDooropen(0);
        this.setDoordiropen(false);
        this.setDoorconfirm(false);
        this.setFixstoppos(false);
        this.setStaeb(false);
        this.setStaaccel(false);
        this.setBrake(9);
        this.setMascon(0);
        this.setCurrent(0);
        this.setBcpressure(480);
        this.setAtsping(0);
        this.setAtspnear(false);
        this.setOverrun(false);
        this.setSafetysystype("ats-p");
        this.setSignalorderptn("default");
        this.setReqstopping(false);
        this.setAtsforced(0);
        this.setAtopisdirect(false);
        this.setAtoforcebrake(false);
        this.setAtoforceslopebrake(false);
        this.setStoppos(null);
        this.setStopactionpos(null);
        this.setAtospeed(-1);
        this.setAtodest(null);
        this.setAtostoptime(-1);
        this.setLastsisign(null);
        this.setLastspsign(null);
        this.setLastsisp(maxspeed);
        this.setLastspsp(maxspeed);
        this.setIlposlist(null);
        this.setIlposoccupied(null);
        this.setIlenterqueuetime(-1);
        this.setIlpriority(0);
        this.setBeinglogged(false);
        this.setRsoccupiedpos(-1);
        this.setAtoautodep(false);
        this.setLastilchest(null);
    }

    static void initVehicle(MinecartGroup mg) {
        utsvehicle lv = vehicle.get(mg);
        if (lv == null) {
            vehicle.put(mg, new utsvehicle(mg));
            lv = vehicle.get(mg);
            motion.recursiveClockLv(lv);
        }
    }

    public int getMascon() {
        return mascon;
    }

    public void setMascon(int mascon) {
        this.mascon = mascon;
    }

    public int getSpeedlimit() {
        return speedlimit;
    }

    public void setSpeedlimit(int speedlimit) {
        this.speedlimit = speedlimit;
    }

    public int getSignallimit() {
        return signallimit;
    }

    public void setSignallimit(int signallimit) {
        this.signallimit = signallimit;
    }

    // -1: TC intervention; 0: normal; 1: EB (ATO pause); 2: EB (SPAD)
    public int getAtsforced() {
        return atsforced;
    }

    public void setAtsforced(int atsforced) {
        this.atsforced = atsforced;
    }

    public int getLastsisp() {
        return lastsisp;
    }

    public void setLastsisp(int lastsisp) {
        this.lastsisp = lastsisp;
    }

    public int getLastspsp() {
        return lastspsp;
    }

    public void setLastspsp(int lastspsp) {
        this.lastspsp = lastspsp;
    }

    public int getDooropen() {
        return dooropen;
    }

    public void setDooropen(int dooropen) {
        this.dooropen = dooropen;
    }

    public int[] getStopoutput() {
        return stopoutput;
    }

    public void setStopoutput(int[] stopoutput) {
        this.stopoutput = stopoutput;
    }

    public Location getAtodest() {
        return atodest;
    }

    public void setAtodest(Location atodest) {
        this.atodest = atodest;
    }

    public int getAtostoptime() {
        return atostoptime;
    }

    public void setAtostoptime(int atostoptime) {
        this.atostoptime = atostoptime;
    }

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAtospeed() {
        return atospeed;
    }

    public void setAtospeed(double atospeed) {
        this.atospeed = atospeed;
    }

    public Location getStoppos() {
        return stoppos;
    }

    public void setStoppos(Location stoppos) {
        this.stoppos = stoppos;
    }

    public Location getLastsisign() {
        return lastsisign;
    }

    public void setLastsisign(Location lastsisign) {
        this.lastsisign = lastsisign;
    }

    public Location getLastspsign() {
        return lastspsign;
    }

    public void setLastspsign(Location lastspsign) {
        this.lastspsign = lastspsign;
    }

    public Location[] getRsposlist() {
        return rsposlist;
    }

    public void setRsposlist(Location[] rsposlist) {
        this.rsposlist = rsposlist;
    }

    public String getSafetysystype() {
        return safetysystype;
    }

    public void setSafetysystype(String safetysystype) {
        this.safetysystype = safetysystype;
    }

    public String getSignalorderptn() {
        return signalorderptn;
    }

    public void setSignalorderptn(String signalorderptn) {
        this.signalorderptn = signalorderptn;
    }

    public boolean isReqstopping() {
        return reqstopping;
    }

    public void setReqstopping(boolean reqstopping) {
        this.reqstopping = reqstopping;
    }

    public boolean isOverrun() {
        return overrun;
    }

    public void setOverrun(boolean overrun) {
        this.overrun = overrun;
    }

    public boolean isFixstoppos() {
        return fixstoppos;
    }

    public void setFixstoppos(boolean fixstoppos) {
        this.fixstoppos = fixstoppos;
    }

    public boolean isStaaccel() {
        return staaccel;
    }

    public void setStaaccel(boolean staaccel) {
        this.staaccel = staaccel;
    }

    public boolean isStaeb() {
        return staeb;
    }

    public void setStaeb(boolean staeb) {
        this.staeb = staeb;
    }

    // ATS-P or ATC Pattern run, 0: none, 1: B8, 2: EB
    public int getAtsping() {
        return atsping;
    }

    public void setAtsping(int atsping) {
        this.atsping = atsping;
    }

    // ATS-P or ATC Pattern near
    public boolean isAtspnear() {
        return atspnear;
    }

    public void setAtspnear(boolean atspnear) {
        this.atspnear = atspnear;
    }

    public boolean isDoordiropen() {
        return doordiropen;
    }

    public void setDoordiropen(boolean doordiropen) {
        this.doordiropen = doordiropen;
    }

    public boolean isDoorconfirm() {
        return doorconfirm;
    }

    public void setDoorconfirm(boolean doorconfirm) {
        this.doorconfirm = doorconfirm;
    }

    public boolean isAtopisdirect() {
        return atopisdirect;
    }

    public void setAtopisdirect(boolean atopisdirect) {
        this.atopisdirect = atopisdirect;
    }

    public boolean isAtoforcebrake() {
        return atoforcebrake;
    }

    public void setAtoforcebrake(boolean atoforcebrake) {
        this.atoforcebrake = atoforcebrake;
    }

    public MinecartGroup getTrain() {
        return train;
    }

    public void setTrain(MinecartGroup train) {
        this.train = train;
    }

    public Location[] getIlposlist() {
        return ilposlist;
    }

    public void setIlposlist(Location[] ilposlist) {
        this.ilposlist = ilposlist;
    }

    public long getIlenterqueuetime() {
        return ilenterqueuetime;
    }

    public void setIlenterqueuetime(long ilenterqueuetime) {
        this.ilenterqueuetime = ilenterqueuetime;
    }

    public Location[] getIlposoccupied() {
        return ilposoccupied;
    }

    public void setIlposoccupied(Location[] ilposoccupied) {
        this.ilposoccupied = ilposoccupied;
    }

    public utsdriver getLd() {
        return ld;
    }

    public void setLd(utsdriver ld) {
        this.ld = ld;
    }

    public World getSavedworld() {
        return savedworld;
    }

    public void setSavedworld(World savedworld) {
        this.savedworld = savedworld;
    }

    @SuppressWarnings("rawtypes")
    public MinecartMember getDriverseat() {
        return driverseat;
    }

    @SuppressWarnings("rawtypes")
    public void setDriverseat(MinecartMember driverseat) {
        this.driverseat = driverseat;
    }

    public int getIlpriority() {
        return ilpriority;
    }

    public void setIlpriority(int ilpriority) {
        this.ilpriority = ilpriority;
    }

    public boolean isBeinglogged() {
        return beinglogged;
    }

    public void setBeinglogged(boolean beinglogged) {
        this.beinglogged = beinglogged;
    }

    public double getAccel() {
        return accel;
    }

    public void setAccel(double accel) {
        this.accel = accel;
    }

    public double getDecel() {
        return decel;
    }

    public void setDecel(double decel) {
        this.decel = decel;
    }

    public double getEbdecel() {
        return ebdecel;
    }

    public void setEbdecel(double ebdecel) {
        this.ebdecel = ebdecel;
    }

    public int[] getSpeedsteps() {
        return speedsteps;
    }

    public void setSpeedsteps(int[] speedsteps) {
        this.speedsteps = speedsteps;
    }

    public double getSpeeddrop() {
        return speeddrop;
    }

    public void setSpeeddrop(double speeddrop) {
        this.speeddrop = speeddrop;
    }

    public int getRsoccupiedpos() {
        return rsoccupiedpos;
    }

    public void setRsoccupiedpos(int rsoccupiedpos) {
        this.rsoccupiedpos = rsoccupiedpos;
    }

    public boolean isAtoautodep() {
        return atoautodep;
    }

    public void setAtoautodep(boolean atoautodep) {
        this.atoautodep = atoautodep;
    }

    public double getBcpressure() {
        return bcpressure;
    }

    public void setBcpressure(double bcpressure) {
        this.bcpressure = bcpressure;
    }

    public int getBrake() {
        return brake;
    }

    public void setBrake(int brake) {
        this.brake = brake;
    }

    public boolean isTwohandled() {
        return twohandled;
    }

    public void setTwohandled(boolean twohandled) {
        this.twohandled = twohandled;
    }

    public boolean isAtoforceslopebrake() {
        return atoforceslopebrake;
    }

    public void setAtoforceslopebrake(boolean atoforceslopebrake) {
        this.atoforceslopebrake = atoforceslopebrake;
    }

    public Location getStopactionpos() {
        return stopactionpos;
    }

    public void setStopactionpos(Location stopactionpos) {
        this.stopactionpos = stopactionpos;
    }

    public Location getLastilchest() {
        return lastilchest;
    }

    public void setLastilchest(Location lastilchest) {
        this.lastilchest = lastilchest;
    }
}
