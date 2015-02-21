package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeThreeTotes extends AutonomousModeBase {
    private FloatInputPoll nudge;
    private FloatInputPoll toteDistance;
    private FloatInputPoll autoZoneDistance;
    private FloatInputPoll strafeTime;
    
    private BooleanInputPoll collectFirstContainer;
    private BooleanInputPoll collectSecondContainer;
    
    public AutonomousModeThreeTotes() {
        super("Three Tote Auto");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        boolean bothEnabled = collectFirstContainer.get() && collectSecondContainer.get();
        
        if (bothEnabled) {
            Logger.warning("Can't collect two containers! Go sit in the corner and think about what you've done.");
        }
        
        // Collect first tote + container
        setClampHeight(1.0f);
        collectTote();
        if (collectFirstContainer.get() || bothEnabled) {
            pickupContainer(nudge.get());
        }
        // TODO: Hold container with top claw
        drive(toteDistance.get());
        // Collect next tote + container
        collectTote();
        if (collectSecondContainer.get() ^ bothEnabled) {
            pickupContainer(nudge.get());
        }
        drive(toteDistance.get());
        // Collect last tote and drive to auto zone
        collectTote();
        // turn(90);
        drive(autoZoneDistance.get());
        // Drop everything off
        ejectTotes();
        DriveCode.octocanumShifting.set(true);
        // strafe(STRAFE_RIGHT, strafeTime.get());

        if (collectFirstContainer.get() || collectSecondContainer.get()) {
            depositContainer(0.0f);
        }
        
        DriveCode.octocanumShifting.set(false);
    }

    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("Auto Mode Three Totes Nudge +A", 1.0f);
        this.toteDistance = context.getFloat("Auto Mode Three Totes Tote Distance +A", 7.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Three Totes Auto Zone Distance +A", 5.0f);
        this.strafeTime = context.getFloat("Auto Mode Three Totes Strafe Time +A", 1.0f);
        this.collectFirstContainer = context.getBoolean("Auto Mode Three Totes Collect First Container +A", true);
        this.collectSecondContainer = context.getBoolean("Auto Mode Three Totes Collect Second Container +A", true);
    }
}
