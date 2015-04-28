package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventStatus;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoStacker extends InstinctModule {

    private static final FloatInput startHeight = ControlInterface.mainTuning.getFloat("AutoStacker Start Height +M", 0.2f);
    private static final FloatInput collectHeight = ControlInterface.mainTuning.getFloat("AutoStacker Collect Height +M", -0.1f);
    private static final FloatInput endHeight = ControlInterface.mainTuning.getFloat("AutoStacker Finish Height +M", 0.2f);
    private static final FloatInput clampTime = ControlInterface.mainTuning.getFloat("AutoStacker Clamp Time +M", 0.25f);

    private static final BooleanInput useRollers = ControlInterface.mainTuning.getBoolean("AutoStacker Use Rollers", false);
    private static final EventStatus atDrop = new EventStatus();

    private final BooleanStatus running;

    private AutoStacker(BooleanStatus running) {
        this.running = running;
        Cluck.publish("AutoStacker At Drop", (EventInput) atDrop);
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus();
        AutoStacker s = new AutoStacker(b);

        s.setShouldBeRunning(b);

        b.setFalseWhen(Igneous.startDisabled);
        return b;
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        boolean wasRunning = Rollers.running.get();

        try {
            Rollers.running.set(false);

            if (useRollers.get()) {
                Rollers.overrideRollers.set(true);
                Rollers.leftPneumaticOverride.set(true);
                Rollers.rightPneumaticOverride.set(true);
                Rollers.leftRollerOverride.set(-1.0f);
                Rollers.rightRollerOverride.set(-1.0f);
            }

            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(startHeight.get());
            waitUntil(Clamp.atDesiredHeight);

            atDrop.event();

            if (useRollers.get()) {
                Rollers.overrideRollers.set(false);
            }

            Clamp.open.set(true);
            waitForTime(clampTime);

            Clamp.height.set(collectHeight.get());
            //waitUntil(Clamp.atDesiredHeight);
            waitUntil(Clamp.atBottom);

            Clamp.open.set(false);
            waitForTime(clampTime);

            Clamp.height.set(endHeight.get());
            this.running.set(false);
        } finally {
            Rollers.running.set(wasRunning);
        }
    }

    @Override
    public String getTypeName() {
        return "auto stacker";
    }

}
