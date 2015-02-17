package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoStacker extends InstinctModule {

    private static final FloatInput startHeight = ControlInterface.mainTuning.getFloat("clamp-auto-start-height", 0.2f);
    private static final FloatInput collectHeight = ControlInterface.mainTuning.getFloat("clamp-auto-collect-height", 0.0f);
    private static final FloatInput endHeight = ControlInterface.mainTuning.getFloat("clamp-auto-finish-height", 0.2f);

    private final BooleanStatus running;

    private AutoStacker(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus();
        AutoStacker s = new AutoStacker(b);

        s.setShouldBeRunning(b);
        s.updateWhen(Igneous.globalPeriodic);

        b.setFalseWhen(Igneous.startDisabled);
        return b;
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        boolean running = Rollers.running.get();

        try {
            Rollers.running.set(false);
            
            Rollers.overrideRollers.set(true);
            Rollers.leftPneumaticOverride.set(true);
            Rollers.rightPneumaticOverride.set(true);
            Rollers.leftRollerOverride.set(-1.0f);
            Rollers.rightRollerOverride.set(-1.0f);

            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(startHeight.get());
            waitUntil(Clamp.atDesiredHeight);

            Rollers.overrideRollers.set(false);

            Clamp.openControl.set(true);
            Clamp.height.set(collectHeight.get());
            waitUntil(Clamp.atDesiredHeight);

            Clamp.openControl.set(false);
            Clamp.height.set(endHeight.get());
            this.running.set(false);
        } finally {
            Rollers.running.set(running);
        }
    }

    @Override
    public String getTypeName() {
        return "auto stacker";
    }

}
