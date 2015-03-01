package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoHumanLoader extends InstinctModule {
    private final BooleanStatus running;
    public static final BooleanInput crateInPosition = AutoLoader.crateInPosition;
    public static final BooleanInputPoll requestingRollers = BooleanMixing.invert((BooleanInputPoll) crateInPosition);

    private AutoHumanLoader(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoHumanLoader a = new AutoHumanLoader(b);

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.globalPeriodic);

        //BooleanMixing.onRelease(b).send(Elevator.setBottom);

        b.setFalseWhen(Igneous.startDisabled);

        return b;
    }

    @Override
    public void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            while (true) {
                Elevator.setTop.event();
                waitUntil(Elevator.atTop);

                waitUntil(crateInPosition);

                Elevator.setBottom.event();
                waitUntil(Elevator.atBottom);

                waitForTime(100);
            }
        } finally {
            running.set(false);
        }
    }

    @Override
    protected String getTypeName() {
        return "auto human loader";
    }
}
