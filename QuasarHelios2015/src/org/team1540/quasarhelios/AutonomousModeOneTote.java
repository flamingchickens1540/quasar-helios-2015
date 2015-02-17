package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;
import ccre.util.Utils;

public class AutonomousModeOneTote extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll returnDistance;
    
    private BooleanInputPoll collectContainer;
    private FloatInputPoll nudge;
    private FloatInputPoll strafeTime;

    public AutonomousModeOneTote() {
        super("One Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        float time0 = Utils.getCurrentTimeSeconds();
        Logger.fine("Tote (1)...");
        drive(toteDistance.get());
        float time1 = Utils.getCurrentTimeSeconds();
        Logger.fine("Tote (2): " + (time1 - time0));
        collectTote();
        float time2 = Utils.getCurrentTimeSeconds();
        Logger.fine("Tote (3): " + (time2 - time1));
        if (collectContainer.get()) {
            setClampHeight(0.0f);
            setClampOpen(true);
            drive(nudge.get());
            setClampOpen(false);
        }
        turn(90);
        waitForTime(500);
        float time3 = Utils.getCurrentTimeSeconds();
        Logger.fine("Tote (4): " + (time3 - time2));
        drive(autoZoneDistance.get());
        waitForTime(500);
        float time4 = Utils.getCurrentTimeSeconds();
        Logger.fine("Tote (5): " + (time4 - time3));
        ejectTotes();
        float time5 = Utils.getCurrentTimeSeconds();
        Logger.fine("Tote (6): " + (time5 - time4));
        if (collectContainer.get()) {
            setClampHeight(0.0f);
            setClampOpen(true);
            setClampHeight(1.0f);
            DriveCode.octocanumShifting.set(true);
            strafe(STRAFE_RIGHT, strafeTime.get());
        }
        drive(-returnDistance.get());
        float time6 = Utils.getCurrentTimeSeconds();
        Logger.fine("Tote (7): " + (time6 - time5));
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("Auto Mode Single Tote Tote Distance +A", 0.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Single Tote Auto Zone Distance +A", 12.0f);
        this.returnDistance = context.getFloat("Auto Mode Single Tote Return Distance +A", 12.0f);
        this.collectContainer = context.getBoolean("Auto Mode Single Tote Should Collect Container +A", false);
        this.nudge = context.getFloat("Auto Mode Single Tote Nudge +A", 1.0f);
        this.strafeTime = context.getFloat("Auto Mode Single Tote Strafe Time +A", 0.4f);
    }

}
