package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.BooleanInput;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoLoader extends InstinctModule {
    private final BooleanCell running;
    public static final BooleanInput crateInPosition = FRC.digitalInput(5);

    public static final FloatInput clampHeightThreshold = ControlInterface.mainTuning.getFloat("AutoLoader Clamp Height Threshold +M", 0.49f);

    private AutoLoader(BooleanCell running) {
        this.running = running;
    }

    public static BooleanCell create() {
        BooleanCell b = new BooleanCell(false);
        AutoLoader a = new AutoLoader(b);

        a.setShouldBeRunning(b);

        b.setFalseWhen(FRC.startDisabled);

        Cluck.publish("AutoLoader Crate Loaded", crateInPosition);

        return b;
    }

    @Override
    public void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        boolean orighl = QuasarHelios.autoHumanLoader.get();
        try {
            QuasarHelios.autoHumanLoader.set(true);

            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(clampHeightThreshold.get());

            try {
                Rollers.overrideRollerSpeedOnly.set(true);
                Rollers.leftRollerOverride.set(1.0f);
                Rollers.rightPneumaticOverride.set(true);
                Rollers.rightRollerOverride.set(1.0f);
                Rollers.leftPneumaticOverride.set(true);
                Rollers.direction.set(Rollers.INPUT);
                Rollers.closed.set(true);

                while (true) {
                    Rollers.running.set(true);
                    waitUntil(crateInPosition);
                    Rollers.running.set(false);
                    waitUntilNot(crateInPosition);
                    waitUntil(Elevator.atTop);
                }
            } finally {
                Rollers.overrideRollerSpeedOnly.set(false);
                Rollers.running.set(false);
            }
        } finally {
            QuasarHelios.autoHumanLoader.set(orighl);
            running.set(false);
        }
    }

    @Override
    protected String getTypeName() {
        return "auto loader";
    }
}
