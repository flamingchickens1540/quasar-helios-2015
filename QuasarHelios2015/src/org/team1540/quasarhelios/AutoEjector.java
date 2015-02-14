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
            setClampHeight(1.0f);
            Elevator.setBottom.event();

            waitUntil(Elevator.atBottom);

            float currentClampHeight = Clamp.height.get();
            boolean running = Rollers.running.get();
            boolean direction = Rollers.direction.get();
            boolean closed = Rollers.closed.get();

            try{
                Rollers.closed.set(true);
                Rollers.direction.set(Rollers.FORWARD);
                Rollers.running.set(true);

                waitUntil(BooleanMixing.invert(AutoLoader.crateInPosition));
                waitForTime(timeout);
            } finally {
                Rollers.running.set(running);
                Rollers.direction.set(direction);
                Rollers.closed.set(closed);
                setClampHeight(currentClampHeight);
            }
        } finally {
            running.set(false);
        }
    }
    
    private void setClampHeight(float value) throws AutonomousModeOverException, InterruptedException {
        Clamp.mode.set(Clamp.MODE_HEIGHT);
        Clamp.height.set(value);
        waitUntil(FloatMixing.floatIsInRange(Clamp.heightReadout, value - clampHeightPadding.get(), value + clampHeightPadding.get()));
    }

}
