package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoEjector extends InstinctModule {
    private final BooleanStatus running;
    private FloatInputPoll timeout = ControlInterface.mainTuning.getFloat("ejector-timeout", 2.0f);

    private AutoEjector(BooleanStatus status) {
        this.running = status;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoEjector a = new AutoEjector(b);

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.constantPeriodic);

        Rollers.running.setFalseWhen(BooleanMixing.onRelease(b));
        Rollers.direction.setTrueWhen(BooleanMixing.onRelease(b));

        return b;
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        Elevator.setBottom.event();

        waitUntil(Elevator.atBottom);

        Rollers.open.set(true);
        Rollers.direction.set(false);
        Rollers.running.set(true);

        waitUntil(BooleanMixing.invert(AutoLoader.crateInPosition));
        waitForTime(timeout);

        Rollers.running.set(false);
        
        running.set(false);
    }
}
