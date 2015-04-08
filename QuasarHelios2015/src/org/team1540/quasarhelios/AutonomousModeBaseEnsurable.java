package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.log.Logger;

public abstract class AutonomousModeBaseEnsurable extends AutonomousModeBase {

    // Ensure methods

    protected static final BooleanInput enableEnsures = ControlInterface.autoTuning.getBoolean("Auto Ensure Enable +A", true);

    public AutonomousModeBaseEnsurable(String modeName) {
        super(modeName);
    }

    protected void ensureFailed(String message) {
        if (enableEnsures.get()) {
            throw new RuntimeException("Ensure failed: " + message + ". Cancelling auto to prevent damage.");
        } else {
            Logger.severe("Ensure failed but disabled: " + message);
        }
    }

    protected void ensure(boolean requirement) {
        if (!requirement) {
            ensureFailed("false");
        }
    }

    protected void ensureNot(boolean requirement) {
        if (requirement) {
            ensureFailed("true");
        }
    }

    protected void ensureInRange(float value, float min, float max) {
        if (value < min || value > max) {
            ensureFailed(value + " not in [" + min + ", " + max + "]");
        } else {
            Logger.finer("Ensure succeeded: " + value + " in [" + min + ", " + max + "]");
        }
    }

    protected void ensureAtLeast(float value, float min) {
        if (value < min) {
            ensureFailed(value + " not >= " + min);
        } else {
            Logger.finer("Ensure succeeded: " + value + " >= " + min + ".");
        }
    }

    protected void ensureAtMost(float value, float max) {
        if (value > max) {
            ensureFailed(value + " not <= " + max);
        } else {
            Logger.finer("Ensure succeeded: " + value + " <= " + max + ".");
        }
    }

    protected void ensureNear(float value, float target, float maxError) {
        float error = Math.abs(target - value);
        if (error > maxError) {
            ensureFailed(value + " too far from " + target + ": " + error + " > " + maxError);
        } else {
            Logger.finer("Ensure succeeded: " + value + " close enough to " + target + ": " + error + " <= " + maxError + ".");
        }
    }

    protected float leftEnc, rightEnc, angle;

    protected void startEnsureBlock() {
        leftEnc = DriveCode.leftEncoder.get();
        rightEnc = DriveCode.rightEncoder.get();
        angle = HeadingSensor.absoluteYaw.get();
    }

    protected void endEnsureBlock(float deltaLeftEnc, float varianceLeftEnc, float deltaRightEnc, float varianceRightEnc, float deltaAngle, float varianceAngle) {
        float newLeftEnc = DriveCode.leftEncoder.get(), newRightEnc = DriveCode.rightEncoder.get(), newAngle = HeadingSensor.absoluteYaw.get();
        Logger.finer("Checking motion ensures (" + leftEnc + ", " + rightEnc + ", " + angle + ") -> (" + newLeftEnc + ", " + newRightEnc + ", " + newAngle + ")");
        ensureNear(newLeftEnc - leftEnc, deltaLeftEnc, varianceLeftEnc);
        ensureNear(newRightEnc - rightEnc, deltaRightEnc, varianceRightEnc);
        ensureNear(newAngle - angle, deltaAngle, varianceAngle);
    }

    protected void endEnsureBlockAngleOnly(float deltaAngle, float varianceAngle) {
        float newLeftEnc = DriveCode.leftEncoder.get(), newRightEnc = DriveCode.rightEncoder.get(), newAngle = HeadingSensor.absoluteYaw.get();
        Logger.finer("Checking angle-only motion ensures (" + leftEnc + ", " + rightEnc + ", " + angle + ") -> (" + newLeftEnc + ", " + newRightEnc + ", " + newAngle + ")");
        ensureNear(newAngle - angle, deltaAngle, varianceAngle);
    }
}
