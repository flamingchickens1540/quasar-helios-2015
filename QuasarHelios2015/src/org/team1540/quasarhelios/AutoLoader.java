package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoLoader extends InstinctModule {
    private final BooleanStatus status;
    public static final BooleanInput crateInPosition = BooleanMixing.createDispatch(Igneous.makeDigitalInput(3), Igneous.globalPeriodic);

    private AutoLoader(BooleanStatus status) {
        this.status = status;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoLoader a = new AutoLoader(b);

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.globalPeriodic);

        BooleanMixing.onRelease(b).send(Elevator.setBottom);

        Cluck.publish(QuasarHelios.testPrefix + "Crate Loaded", crateInPosition);

        return b;
    }

    @Override
    public void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        Elevator.setBottom.event();
        waitUntil(Elevator.atBottom);
        Elevator.setTop.event();
        waitUntil(Elevator.atTop);

        boolean r = Rollers.running.get();
        boolean d = Rollers.direction.get();
        boolean o = Rollers.open.get();

        Rollers.direction.set(true);
        Rollers.running.set(true);
        Rollers.open.set(false);

        waitUntil(crateInPosition);

        Rollers.running.set(r);
        Rollers.direction.set(d);
        Rollers.open.set(o);

        Elevator.setBottom.event();
        status.set(false);
    }
}
