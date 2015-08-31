package org.team1540.quasarhelios;

import ccre.channel.DerivedBooleanInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.drivers.chrobotics.UM7LT;
import ccre.igneous.Igneous;
import ccre.log.Logger;

public class HeadingSensor {
    public static FloatInput pitch;
    public static FloatInput yaw;
    public static FloatInput roll;

    public static FloatInput pitchRate;
    public static FloatInput yawRate;
    public static FloatInput rollRate;

    public static EventOutput zeroGyro;

    private static final FloatStatus accumulator = new FloatStatus();

    public static FloatInput absoluteYaw;

    public static final EventOutput resetAccumulator = accumulator.getSetEvent(0);

    public static void setup() {
        final UM7LT sensor = new UM7LT(Igneous.makeRS232_MXP(115200, "UM7-LT"));
        sensor.autoreportFaults.set(true);
        QuasarHelios.publishFault("heading-sensor-any", new DerivedBooleanInput(sensor.onHealthUpdate) {
            @Override
            protected boolean apply() {
                return sensor.hasFault();
            }
        });
        Cluck.publish("Diagnose Heading Sensor", new EventOutput() {
            public void event() {
                UM7LT.Faults faults = new UM7LT.Faults();
                sensor.getFaults(faults);
                Logger.info("Diagnosing UM7LT...");
                boolean any = false;
                if (faults.accelerometer_distorted) {
                    Logger.warning("UM7LT Accelerometer distorted.");
                    any = true;
                }
                if (faults.accelerometer_failed) {
                    Logger.warning("UM7LT Accelerometer failed.");
                    any = true;
                }
                if (faults.comm_overflow) {
                    Logger.warning("UM7LT Comms Overflow.");
                    any = true;
                }
                if (faults.gyro_failed) {
                    Logger.warning("UM7LT Gyro failed.");
                    any = true;
                }
                if (faults.magnetometer_distorted) {
                    Logger.warning("UM7LT Magnetometer distorted.");
                    any = true;
                }
                if (faults.magnetometer_failed) {
                    Logger.warning("UM7LT Magnetometer failed.");
                    any = true;
                }
                if (!any) {
                    Logger.info("No faults detected.");
                }
            }
        });
        sensor.start();

        pitch = sensor.pitch;
        yaw = sensor.yaw;
        roll = sensor.roll;

        QuasarHelios.publishFault("heading-sensor-all-zeroes", new DerivedBooleanInput(pitch, yaw, roll) {
            @Override
            protected boolean apply() {
                return pitch.get() == 0 && yaw.get() == 0 && roll.get() == 0;
            }
        });

        pitchRate = sensor.pitchRate;
        yawRate = sensor.yawRate;
        rollRate = sensor.rollRate;

        zeroGyro = sensor.zeroGyro;

        absoluteYaw = accumulator.plus(yaw);

        yaw.onUpdate(new EventOutput() {
            float oldyaw = 0;

            public void event() {
                float currentyaw = yaw.get();
                if (Math.abs(currentyaw - oldyaw) > 180) {
                    Logger.fine("YAW JUMP: " + oldyaw + " to " + currentyaw);
                    if (oldyaw > 180) {
                        accumulator.set(accumulator.get() + 360);
                    } else if (oldyaw < -180) {
                        accumulator.set(accumulator.get() - 360);
                    }
                }
                oldyaw = yaw.get();
            }
        });

        Cluck.publish("Heading Sensor Zero Gyro", zeroGyro);
        Cluck.publish("Heading Sensor Reset Accumulator", resetAccumulator);

        Cluck.publish("Heading Sensor Pitch", pitch);
        Cluck.publish("Heading Sensor Yaw", yaw);
        Cluck.publish("Heading Sensor Yaw Absolute", absoluteYaw);
        Cluck.publish("Heading Sensor Roll", roll);

        Cluck.publish("Heading Sensor Pitch Rate", pitchRate);
        Cluck.publish("Heading Sensor Yaw Rate", yawRate);
        Cluck.publish("Heading Sensor Roll Rate", rollRate);
    }
}
