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
    public static final FloatInputPoll timeout = ControlInterface.mainTuning.getFloat("AutoLoader Timeout +M", 0.5f);
    public static final BooleanInput crateInPosition = BooleanMixing.createDispatch(Igneous.makeDigitalInput(5), Igneous.globalPeriodic);
    
    public static final FloatInputPoll clampHeightThreshold = ControlInterface.mainTuning.getFloat("main-autoloader-clamp-height-threshold", 0.5f);

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

            float currentClampHeight = Clamp.heightReadout.get();
            if (currentClampHeight < clampHeightThreshold.get()) {
                Clamp.mode.set(Clamp.MODE_HEIGHT);
                Clamp.height.set(clampHeightThreshold.get());
                waitUntil(BooleanMixing.andBooleans(Clamp.atDesiredHeight, Elevator.atTop));
            } else {
                waitUntil(Elevator.atTop);
            }

            try {
                boolean running = Rollers.running.get();
                boolean direction = Rollers.direction.get();
                boolean closed = Rollers.closed.get();

                try {
                    Rollers.direction.set(Rollers.INPUT);
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
