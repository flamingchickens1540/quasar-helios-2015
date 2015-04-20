package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoLoader extends InstinctModule {
    private final BooleanStatus running;
    private static final FloatInputPoll timeout = ControlInterface.mainTuning.getFloat("AutoLoader Timeout +M", 0.0f);
    private static final FloatStatus slowRollerSpeed = ControlInterface.mainTuning.getFloat("AutoLoader Rollers Slow Speed +M", .3f);
    public static final BooleanInput crateInPosition = BooleanMixing.createDispatch(Igneous.makeDigitalInput(5), Igneous.globalPeriodic);

    public static final FloatInputPoll clampHeightThreshold = ControlInterface.mainTuning.getFloat("AutoLoader Clamp Height Threshold +M", 0.49f);

    private AutoLoader(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoLoader a = new AutoLoader(b);

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.globalPeriodic);

        b.setFalseWhen(Igneous.startDisabled);

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
                    FloatMixing.combine(Rollers.leftRollerOverride, Rollers.rightRollerOverride).set(slowRollerSpeed.get());
                    waitUntilNot(crateInPosition);
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
