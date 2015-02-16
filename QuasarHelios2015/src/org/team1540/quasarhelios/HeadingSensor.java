package org.team1540.quasarhelios;

import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.FloatMixing;
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
        UM7LT sensor = new UM7LT(Igneous.makeRS232_MXP(115200, "UM7-LT"));
        sensor.autoreportFaults.set(true);
        QuasarHelios.publishFault("heading-sensor-any", () -> sensor.hasFault());
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

        QuasarHelios.publishFault("heading-sensor-all-zeroes", () -> pitch.get() == 0 && yaw.get() == 0 && roll.get() == 0);

        pitchRate = sensor.pitchRate;
        yawRate = sensor.yawRate;
        rollRate = sensor.rollRate;

        zeroGyro = sensor.zeroGyro;

        absoluteYaw = FloatMixing.addition.of((FloatInput) accumulator, yaw);

        Igneous.globalPeriodic.send(new EventOutput() {
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

        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Zero", zeroGyro);
        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Reset Accumulator", resetAccumulator);

        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Pitch", pitch);
        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Yaw", yaw);
        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Absolute Yaw", absoluteYaw);
        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Roll", roll);

        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Pitch Rate", pitchRate);
        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Yaw Rate", yawRate);
        Cluck.publish(QuasarHelios.testPrefix + "Heading Sensor Roll Rate", rollRate);
    }
}
