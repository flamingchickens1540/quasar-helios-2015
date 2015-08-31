package org.team1540.quasarhelios;

import ccre.channel.FloatInput;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeBump extends AutonomousModeBase {

    @Tunable(60.0f)
    private FloatInput distance;
    @Tunable(1.0f)
    private FloatInput time;
    @Tunable(1.0f)
    private FloatInput speed;

    public AutonomousModeBump() {
        super("Bump");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        // Motion
        drive(distance.get(), speed.get());
        driveForTime((long) (time.get() * 1000), speed.get());
    }
}
