package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeThreeTotes extends AutonomousModeBase {
    private FloatInputPoll nudge;
    private FloatInputPoll toteDistance;
    private FloatInputPoll autoZoneDistance;
    private FloatInputPoll strafeTime;
    
    private BooleanInputPoll collectContainers;
    private BooleanInputPoll containerToCollect; // true is first, false is second

    public AutonomousModeThreeTotes() {
        super("Three Tote Auto");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        // Collect first tote + container
        setClampHeight(1.0f);
        collectTote();
        if (containerToCollect.get() && collectContainers.get()) {
            setClampHeight(0.0f);
            setClampOpen(true);
            drive(nudge.get());
            setClampOpen(false);
            setClampHeight(1.0f);
        }
        // TODO: Hold container with top claw
        drive(toteDistance.get());
        // Collect next tote + container
        collectTote();
        if (!containerToCollect.get() && collectContainers.get()) {
            setClampHeight(0.0f);
            setClampOpen(true);
            drive(nudge.get());
            setClampOpen(false);
            setClampHeight(1.0f);
        }
        drive(toteDistance.get());
        // Collect last tote and drive to auto zone
        collectTote();
        turn(90);
        drive(autoZoneDistance.get());
        // Drop everything off
        if (collectContainers.get()) {
            setClampHeight(0.0f);
            setClampOpen(true);
            setClampHeight(1.0f);
            DriveCode.octocanumShifting.set(true);
            strafe(1.0f, strafeTime.get());
        }
        ejectTotes();
        DriveCode.octocanumShifting.set(false);
    }

    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("auto-three-totes-nudge", 1.0f);
        this.toteDistance = context.getFloat("auto-three-totes-tote-distance", 7.0f);
        this.autoZoneDistance = context.getFloat("auto-three-totes-auto-zone-distance", 5.0f);
        this.strafeTime = context.getFloat("auto-three-totes-strafe-time", 1.0f);
        this.containerToCollect = context.getBoolean("auto-three-totes-container-to-collect", true);
        this.collectContainers = context.getBoolean("auto-three-totes-collect-containers", true);
    }
}
