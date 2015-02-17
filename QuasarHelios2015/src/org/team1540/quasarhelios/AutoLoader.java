package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInputPoll;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoLoader extends InstinctModule {
    private final BooleanStatus running;
    private static final FloatInputPoll timeout = ControlInterface.mainTuning.getFloat("AutoLoader Timeout +M", 0.5f);
    public static final BooleanInput crateInPosition = BooleanMixing.createDispatch(Igneous.makeDigitalInput(5), Igneous.globalPeriodic);

    private AutoLoader(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoLoader a = new AutoLoader(b);

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.globalPeriodic);

        BooleanMixing.onRelease(b).send(Elevator.setBottom);

        b.setFalseWhen(Igneous.startDisabled);

        Cluck.publish("AutoLoader Crate Loaded", crateInPosition);

        return b;
    }

    @Override
    public void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            Elevator.setTop.event();

            try {
                waitUntil(Elevator.atTop);

                boolean running = Rollers.running.get();
                boolean direction = Rollers.direction.get();
                boolean closed = Rollers.closed.get();

                try {
                    Rollers.direction.set(Rollers.REVERSE);
                    Rollers.running.set(true);
                    Rollers.closed.set(true);

                    waitUntil(crateInPosition);
                    waitForTime(timeout);
                } finally {
                    Rollers.running.set(running);
                    Rollers.direction.set(direction);
                    Rollers.closed.set(closed);
                }
            } finally {
                Elevator.setBottom.event();
            }
            waitUntil(Elevator.atBottom);
            waitForTime(1000);
        } finally {
            running.set(false);
        }
    }

    @Override
    protected String getTypeName() {
        return "auto loader";
    }
}
