package org.team1540.quasarhelios;

import java.lang.reflect.Field;
import java.util.ArrayList;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;
import ccre.log.Logger;
import ccre.rconf.RConf;
import ccre.rconf.RConf.Entry;
import ccre.rconf.RConfable;

public abstract class AutonomousModeBase extends InstinctModeModule {
    private static final TuningContext context = ControlInterface.autoTuning;
    private static final FloatInput driveSpeed = context.getFloat("Auto Drive Speed +A", 1.0f);
    private static final FloatInput rotateSpeed = context.getFloat("Auto Rotate Speed +A", 0.5f).negated();
    private static final FloatInput rotateMultiplier = context.getFloat("Auto Rotate Multiplier +A", 1.0f);
    private static final FloatInput rotateOffset = context.getFloat("Auto Rotate Offset +A", -30f);
    public static final float STRAFE_RIGHT = 1.0f;
    public static final float STRAFE_LEFT = -1.0f;

    protected final BooleanStatus straightening = new BooleanStatus();

    public AutonomousModeBase(String modeName) {
        super(modeName);
    }

    protected void driveForTime(long time, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        Autonomous.desiredAngle.set(HeadingSensor.absoluteYaw.get());

        FloatInput rightMotorSpeed, leftMotorSpeed;

        if (time > 0) {
            rightMotorSpeed = Autonomous.autoPID.minus(speed);
            leftMotorSpeed = Autonomous.reversePID.minus(speed);
        } else {
            rightMotorSpeed = Autonomous.autoPID.minusRev(speed);
            leftMotorSpeed = Autonomous.reversePID.minusRev(speed);
        }

        EventOutput unbind1 = rightMotorSpeed.sendR(DriveCode.rightMotors);
        EventOutput unbind2 = leftMotorSpeed.sendR(DriveCode.leftMotors);
        try {
            waitForTime(Math.abs(time));
        } finally {
            unbind2.event();
            unbind1.event();
            DriveCode.allMotors.set(0.0f);
            straightening.set(true);
        }
    }

    protected void drive(float distance) throws AutonomousModeOverException, InterruptedException {
        drive(distance, driveSpeed.get());
    }

    protected void drive(float distance, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        Autonomous.desiredAngle.set(HeadingSensor.absoluteYaw.get());

        float startingEncoder = DriveCode.leftEncoder.get();

        FloatInput rightMotorSpeed, leftMotorSpeed;

        if (distance > 0) {
            rightMotorSpeed = Autonomous.reversePID.minus(speed);
            leftMotorSpeed = Autonomous.autoPID.minus(speed);
        } else {
            rightMotorSpeed = Autonomous.reversePID.minusRev(speed);
            leftMotorSpeed = Autonomous.autoPID.minusRev(speed);
        }

        EventOutput unbind1 = rightMotorSpeed.sendR(DriveCode.rightMotors);
        EventOutput unbind2 = leftMotorSpeed.sendR(DriveCode.leftMotors);
        try {
            if (distance > 0) {
                waitUntilAtLeast(DriveCode.leftEncoder, startingEncoder + distance);
            } else {
                waitUntilAtMost(DriveCode.leftEncoder, startingEncoder + distance);
            }
        } finally {
            unbind2.event();
            unbind1.event();
            DriveCode.allMotors.set(0.0f);
            straightening.set(true);
        }
    }

    protected void turnAbsolute(float start, float degree, boolean adjustAngle) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);

        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                DriveCode.rotate.set(-rotateSpeed.get());
                waitUntilAtMost(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                DriveCode.rotate.set(rotateSpeed.get());
                waitUntilAtLeast(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
    }

    protected void turnAbsolute(float start, float degree, boolean adjustAngle, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);

        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                DriveCode.rotate.set(speed);
                waitUntilAtMost(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                DriveCode.rotate.set(-speed);
                waitUntilAtLeast(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
    }

    protected void turn(float degree, boolean adjustAngle) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        float startingYaw = HeadingSensor.absoluteYaw.get();

        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                DriveCode.rotate.set(-rotateSpeed.get());
                waitUntilAtMost(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                DriveCode.rotate.set(rotateSpeed.get());
                waitUntilAtLeast(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
    }

    protected void turn(float degree, boolean adjustAngle, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        float startingYaw = HeadingSensor.absoluteYaw.get();

        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                DriveCode.rotate.set(speed);
                waitUntilAtMost(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                DriveCode.rotate.set(-speed);
                waitUntilAtLeast(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
    }

    protected void singleSideTurnDistance(long distance, boolean side) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);

        FloatOutput motors = side ? DriveCode.leftMotors : DriveCode.rightMotors;
        motors.set(rotateSpeed.get());
        FloatInput enc = side ? DriveCode.leftEncoder : DriveCode.rightEncoder;
        if (distance > 0) {
            waitUntilAtLeast(enc, enc.get() + distance);
        } else {
            waitUntilAtMost(enc, enc.get() + distance);
        }
        motors.set(0.0f);
    }

    protected void singleSideTurnAbsolute(float start, float degree, boolean side, boolean adjustAngle, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);

        FloatOutput motors = side ? DriveCode.leftMotors : DriveCode.rightMotors;
        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                motors.set(speed);
                waitUntilAtMost(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                motors.set(-speed);
                waitUntilAtLeast(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        }
        motors.set(0.0f);
    }

    protected void singleSideTurn(long time, boolean side) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);

        FloatOutput motors = side ? DriveCode.leftMotors : DriveCode.rightMotors;
        motors.set(rotateSpeed.get());
        waitForTime(time);
        motors.set(0.0f);
    }

    protected void collectTote() throws AutonomousModeOverException, InterruptedException {
        collectTote(false, 0);
    }

    protected void collectTote(boolean shake, int timeout) throws AutonomousModeOverException, InterruptedException {
        // Move elevator.
        Elevator.setTop.event();

        // Move clamp.
        float currentClampHeight = Clamp.heightReadout.get();
        if (currentClampHeight < AutoLoader.clampHeightThreshold.get()) {
            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(AutoLoader.clampHeightThreshold.get());
            // TODO: avoid extra channel baggage
            waitUntil(Clamp.atDesiredHeight.and(Elevator.atTop));
        } else {
            waitUntil(Elevator.atTop);
        }

        // Run rollers.
        Rollers.direction.set(Rollers.INPUT);
        Rollers.running.set(true);
        Rollers.closed.set(true);

        if (shake) {
            while (true) {
                DriveCode.rotate.set(0.5f);
                waitForTime(100);
                if (AutoLoader.crateInPosition.get()) {
                    break;
                }
                DriveCode.rotate.set(-0.5f);
                waitForTime(100);
                if (AutoLoader.crateInPosition.get()) {
                    break;
                }
            }
            Rollers.running.set(false);
            Rollers.closed.set(false);
        } else if (timeout > 0) {
            waitUntil(timeout, AutoLoader.crateInPosition);
            Rollers.running.set(false);
            Rollers.closed.set(false);
        } else if (timeout == 0) {
            waitUntil(AutoLoader.crateInPosition);
            Rollers.running.set(false);
            Rollers.closed.set(false);
        } else {
            waitForTime(250);
        }
    }

    protected void collectToteWithElevator() throws AutonomousModeOverException, InterruptedException {
        QuasarHelios.autoLoader.set(true);
        waitUntilNot(QuasarHelios.autoLoader);
    }

    protected void collectToteFast(boolean reopen) throws AutonomousModeOverException, InterruptedException {
        collectToteFastStart();
        collectToteFastEnd(reopen);
    }

    protected void collectToteFastStart() {
        QuasarHelios.autoHumanLoader.set(true);
        Rollers.direction.set(Rollers.INPUT);
        Rollers.running.set(true);
        Rollers.closed.set(true);
    }

    protected void collectToteFastEnd(boolean reopen) throws AutonomousModeOverException, InterruptedException {
        waitUntil(AutoLoader.crateInPosition);
        Rollers.running.set(false);
        Rollers.closed.set(!reopen);
    }

    protected void setClampOpen(boolean value) throws InterruptedException, AutonomousModeOverException {
        Clamp.open.set(value);
        waitForTime(30);
    }

    protected void startSetClampHeight(float value) {
        Clamp.mode.set(Clamp.MODE_HEIGHT);
        Clamp.height.set(value);
    }

    protected void setClampHeight(float value) throws AutonomousModeOverException, InterruptedException {
        Clamp.mode.set(Clamp.MODE_HEIGHT);
        Clamp.height.set(value);
        waitUntil(Clamp.atDesiredHeight);
    }

    protected void ejectTotes() throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        QuasarHelios.autoEjector.set(true);
        waitUntilNot(QuasarHelios.autoEjector);
    }

    protected void pickupContainer(float nudge) throws AutonomousModeOverException, InterruptedException {
        if (nudge != 0) {
            setClampOpen(true);
            drive(nudge);
        }
        setClampOpen(false);
        waitForTime(1000);
        setClampHeight(0.2f);
        waitForTime(1000);
        // align container
        setClampHeight(0.0f);
        waitForTime(1000);
        setClampOpen(true);
        waitForTime(1000);
        setClampOpen(false);
        waitForTime(2000);
        setClampHeight(0.5f);
    }

    protected void depositContainer(float height) throws AutonomousModeOverException, InterruptedException {
        setClampHeight(height);
        waitForTime(100);
        setClampOpen(true);
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            DriveCode.disablePitMode.event();
            // TODO: Move these elsewhere... this is a terrible plan.
            Autonomous.autoPID.send(DriveCode.leftMotors.filter(straightening));
            Autonomous.reversePID.send(DriveCode.rightMotors.filter(straightening));
            runAutonomous();
        } finally {
            straightening.set(false);
            DriveCode.allMotors.set(0);
        }
    }

    protected abstract void runAutonomous() throws InterruptedException, AutonomousModeOverException;

    @Override
    public void loadSettings(TuningContext ctx) {
        ArrayList<String> settings = new ArrayList<>();
        for (Field f : this.getClass().getDeclaredFields()) {
            Tunable annot = f.getAnnotation(Tunable.class);
            if (annot != null) {
                f.setAccessible(true);
                try {
                    String name = "Auto Mode " + getModeName() + " " + toTitleCase(f.getName()) + " +A";
                    if (f.getType() == FloatInput.class) {
                        f.set(this, ctx.getFloat(name, annot.value()));
                    } else if (f.getType() == BooleanInput.class) {
                        f.set(this, ctx.getBoolean(name, annot.valueBoolean()));
                    } else {
                        Logger.severe("Invalid application of @Tunable to " + f.getType());
                        continue;
                    }
                    settings.add(name);
                } catch (Exception e) {
                    Logger.severe("Could not load autonomous configuration for " + this.getClass().getName() + "." + f.getName(), e);
                }
            }
        }
        Cluck.publishRConf("Auto Mode " + getModeName() + " Settings", new RConfable() {
            public boolean signalRConf(int field, byte[] data) throws InterruptedException {
                if (field == 1) {
                    Autonomous.mainModule.setActiveMode(AutonomousModeBase.this);
                    return true;
                }
                return false;
            }

            public Entry[] queryRConf() throws InterruptedException {
                ArrayList<Entry> entries = new ArrayList<>();
                entries.add(RConf.title("Settings for " + getModeName()));
                if (Autonomous.mainModule.getActiveMode() == AutonomousModeBase.this) {
                    entries.add(RConf.string("Activate"));
                } else {
                    entries.add(RConf.button("Activate"));
                }
                for (String setting : settings) {
                    entries.add(RConf.cluckRef(setting));
                }
                entries.add(RConf.autoRefresh(10000));
                return entries.toArray(new Entry[entries.size()]);
            }
        });
    }

    private String toTitleCase(String name) {
        StringBuilder sb = new StringBuilder();
        int lastStart = 0;
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i)) || (Character.isDigit(name.charAt(i)) && !Character.isDigit(name.charAt(i - 1)))) {
                sb.append(name.substring(lastStart, i)).append(' ');
                lastStart = i;
            }
        }
        sb.append(name.substring(lastStart));
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }
}
