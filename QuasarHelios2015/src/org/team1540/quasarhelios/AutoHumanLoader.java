package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoHumanLoader extends InstinctModule {
    private final BooleanStatus running;
    public static final BooleanInput crateInPosition = AutoLoader.crateInPosition;
    public static final BooleanInput requestingRollers = crateInPosition.not();
    public static final FloatInput settleDelay = ControlInterface.teleTuning.getFloat("AutoHumanLoader Settle Time +T", 0.25f);

    private AutoHumanLoader(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoHumanLoader a = new AutoHumanLoader(b);

        a.setShouldBeRunning(b);

        b.setFalseWhen(FRC.startDisabled);

        return b;
    }

    @Override
    public void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            while (true) {
                Elevator.setTop.event();
                waitUntil(Elevator.atTop);

                if (crateInPosition.get()) {
                    waitForTime(100);
                }
                waitUntil(crateInPosition);

                waitForTime(settleDelay);

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
