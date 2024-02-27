package me.fiveave.untenshi;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.Set;

import static me.fiveave.untenshi.main.*;

class utsvehicle {
    private double accel;
    private double decel;
    private double ebdecel;
    private double speeddrop;
    private int[] speedsteps;
    private utsdriver ld;
    private World savedworld;
    private int mascon;
    private int speedlimit;
    private int signallimit;
    private int atsforced;
    private int lastsisp;
    private int lastspsp;
    private int dooropen;
    private int[] stopoutput;
    private int[] atodest;
    private int atostoptime;
    private int rsoccupiedpos;
    private int ilpriority;
    private double current;
    private double speed;
    private double atospeed;
    private double[] stoppos;
    private Location lastsisign;
    private Location lastspsign;
    private Location[] rsposlist;
    private Location[] ilposlist;
    private Location[] ilposoccupied;
    private String safetysystype;
    private String signalorderptn;
    private boolean reqstopping;
    private boolean overrun;
    private boolean fixstoppos;
    private boolean staaccel;
    private boolean staeb;
    private int atsping;
    private boolean atspnear;
    private boolean doordiropen;
    private boolean doorconfirm;
    private boolean atopisdirect;
    private boolean atoforcebrake;
    private boolean beinglogged;
    private MinecartGroup train;
    @SuppressWarnings("rawtypes")
    private MinecartMember driverseat;
    private long ilenterqueuetime;

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
        this.setSpeeddrop(plugin.getConfig().getDouble("speeddroprate"));
        this.setAccel(tempaccel);
        this.setDecel(tempdecel);
        this.setEbdecel(tempebdecel);
        this.setSpeedsteps(tempspeedsteps);
        this.setSpeed(0.0);
        this.setSignallimit(maxspeed);
        this.setSpeedlimit(maxspeed);
        this.setDooropen(0);
        this.setDoordiropen(false);
        this.setDoorconfirm(false);
        this.setFixstoppos(false);
        this.setStaeb(false);
        this.setStaaccel(false);
        this.setMascon(-9);
        this.setCurrent(-480.0);
        this.setAtsping(0);
        this.setAtspnear(false);
        this.setOverrun(false);
        this.setSafetysystype("ats-p");
        this.setSignalorderptn("default");
        this.setReqstopping(false);
        this.setAtsforced(0);
        this.setAtopisdirect(false);
        this.setAtoforcebrake(false);
        this.setStoppos(null);
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

    public int[] getAtodest() {
        return atodest;
    }

    public void setAtodest(int[] atodest) {
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

    public double[] getStoppos() {
        return stoppos;
    }

    public void setStoppos(double[] stoppos) {
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
}
