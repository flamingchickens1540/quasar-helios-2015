package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoEjector extends InstinctModule {
    private final BooleanStatus running;
    private FloatInputPoll timeout = ControlInterface.mainTuning.getFloat("ejector-timeout", 2.0f);
    private FloatInputPoll clampHeightPadding = ControlInterface.autoTuning.getFloat("auto-clamp-height-padding", 0.01f);

    private AutoEjector(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoEjector a = new AutoEjector(b);

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.constantPeriodic);

        Rollers.running.setFalseWhen(BooleanMixing.onRelease(b));
        Rollers.direction.setTrueWhen(BooleanMixing.onRelease(b));
        
        b.setFalseWhen(Igneous.startDisabled);

        return b;
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            float currentClampHeight = Clamp.heightOrSpeed.get();
            setClampHeight(1.0f);
            Elevator.setBottom.event();

            waitUntil(Elevator.atBottom);

            Rollers.closed.set(true);
            Rollers.direction.set(true);
            Rollers.running.set(true);

            waitUntil(BooleanMixing.invert(AutoLoader.crateInPosition));
            waitForTime(timeout);

            Rollers.running.set(false);
            setClampHeight(currentClampHeight);
        } finally {
            running.set(false);
        }
    }
    
    private void setClampHeight(float value) throws AutonomousModeOverException, InterruptedException {
        Clamp.mode.set(false);
        Clamp.heightOrSpeed.set(value);
        waitUntil(FloatMixing.floatIsInRange(Clamp.heightReadout, value - clampHeightPadding.get(), value + clampHeightPadding.get()));
    }

}
