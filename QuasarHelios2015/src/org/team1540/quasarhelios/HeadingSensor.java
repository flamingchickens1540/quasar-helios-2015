package org.team1540.quasarhelios;

import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.FloatMixing;
import ccre.drivers.chrobotics.UM7LT;
import ccre.igneous.Igneous;

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
        sensor.start();

        pitch = sensor.pitch;
        yaw = sensor.yaw;
        roll = sensor.roll;

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
