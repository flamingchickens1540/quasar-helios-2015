package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeThreeTotes extends AutonomousModeBase {
    public FloatInputPoll nudge;
    public FloatInputPoll toteDistance;
    public FloatInputPoll autoZoneDistance;
    public FloatInputPoll strafeTime;

    public AutonomousModeThreeTotes() {
        super("Three Tote Auto");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        // Collect first tote + container
        setClampHeight(1.0f);
        collectTote();
        setClampHeight(0.0f);
        setClampOpen(true);
        drive(nudge.get());
        setClampOpen(false);
        setClampHeight(1.0f);
        // TODO: Hold container with top claw
        drive(toteDistance.get());
        // Collect next tote + container
        collectTote();
        setClampHeight(0.0f);
        setClampOpen(true);
        drive(nudge.get());
        setClampOpen(false);
        setClampHeight(1.0f);
        drive(toteDistance.get());
        // Collect last tote and drive to auto zone
        collectTote();
        turn(90);
        drive(autoZoneDistance.get());
        // Drop everything off
        setClampHeight(0.0f);
        setClampOpen(true);
        setClampHeight(1.0f);
        DriveCode.octocanumShifting.set(true);
        strafe(1.0f, strafeTime.get());
        ejectTotes();
        strafe(1.0f, strafeTime.get());
        // TODO: Put down top container
        DriveCode.octocanumShifting.set(false);
    }

    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("auto-full-nudge", 1.0f);
        this.toteDistance = context.getFloat("auto-full-tote-distance", 7.0f);
        this.autoZoneDistance = context.getFloat("auto-full-auto-zone-distance", 5.0f);
        this.strafeTime = context.getFloat("auto-full-strafe-time", 1.0f);
    }

}
