package org.team1540.quasarhelios;

import ccre.channel.BooleanOutput;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeThreeTotes extends AutonomousModeBase {
    public AutonomousModeThreeTotes() {
        super("Three Totes");
    }
    
    private FloatInputPoll angle;
    private FloatInputPoll toteDistance;
    private FloatInputPoll startDistance;
    private FloatInputPoll startAngle;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneDistance;
    private FloatInputPoll nudge;
    private FloatInputPoll longDistance;
    private FloatInputPoll driftingAngle;
    private FloatInputPoll containerDistance;
    
    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        try {
            Rollers.overrideRollers.set(false);
            BooleanOutput closed = BooleanMixing.combine(Rollers.leftPneumaticOverride, Rollers.rightPneumaticOverride);
            // Collect first tote.
            collectToteWithElevator();
            startSetClampHeight(0.0f);
            setClampOpen(true);
            waitForTime(2000);
            turn(-startAngle.get(), false, 0.25f);
            drive(startDistance.get());
            // Pickup first container.
            pickupContainer(nudge.get());
            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(1.0f);
            
    
            // Drive to next tote.
            waitForTime(1000);
            drive(longDistance.get());
            waitForTime(1000);
    
            // Collect tote.
            collectToteWithElevator();
            // Go around second container.
            straightening.set(false);
            Rollers.overrideRollers.set(true);
            closed.set(true);
            turn(-driftingAngle.get(), false);
            drive(containerDistance.get());
         
            // Turn to face tote.
            turn(angle.get(), false);
            // Open rollers.
            closed.set(false);
            Rollers.overrideRollers.set(false);
            waitForTime(1000);
            // Drive to tote and collect.
            drive(toteDistance.get());
            waitForTime(1000);
            collectToteWithElevator();
                        
            // Go to auto zone.
            turn(-autoZoneAngle.get(), false);
            straightening.set(false);
            drive(autoZoneDistance.get(), 0.75f);
            
            // Eject totes.
            ejectTotes();
            
        } finally {
            Rollers.overrideRollers.set(false);
        }
    }

    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("Auto Mode Three Totes Nudge +A", 14.5f);
        this.toteDistance = context.getFloat("Auto Mode Three Totes Tote Distance +A", 36.0f);
        this.angle = context.getFloat("Auto Mode Avoid Three Totes Angle +A", 20.0f);
        this.autoZoneAngle = context.getFloat("Auto Mode Three Totes Auto Zone Angle", 100.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Three Totes Auto Zone Distance", 62.0f);
        this.longDistance = context.getFloat("Auto Mode Three Totes Long Distance", 53.5f);
        this.driftingAngle = context.getFloat("Auto Mode Three Totes Drifting Angle", 20.0f);
        this.containerDistance = context.getFloat("Auto Mode Three Totes Container Distance", 40.0f);
        
        // These are probably wrong.
        this.startAngle = context.getFloat("Auto Mode Three Totes Start Angle", 29.2f);
        this.startDistance = context.getFloat("Auto Mode Three Totes Start Distance", 14.1f);
    }
}
