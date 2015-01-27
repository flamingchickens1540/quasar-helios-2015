package org.team1540.quasarhelios;

import ccre.channel.BooleanOutput;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.ctrl.Ticker;
import ccre.holders.TuningContext;
import ccre.igneous.Igneous;

public class Suspension {

    private static BooleanOutput[] valves = new BooleanOutput[3];

    private static int currentLevel;

    public static void setup() {/*
        TuningContext context = new TuningContext("suspension").publishSavingEvent();
        FloatInput cutoffForward = context.getFloat("max-tilt-forward", 5.0f);
        FloatInput cutoffBackward = context.getFloat("max-tilt-backward", 5.0f);
        FloatInput cutoffRate = context.getFloat("min-rate", 0.0f);

        valves[0] = Igneous.makeSolenoid(0);
        valves[1] = Igneous.makeSolenoid(1);
        valves[2] = Igneous.makeSolenoid(2);

        setSuspensionLevel(1);

        QuasarHelios.globalControl.send(new EventOutput() {
            @Override
            public void event() {
                float pitch = HeadingSensor.pitch.get();
                float pitchRate = HeadingSensor.pitchRate.get();

                pitch = (((pitch % 360) + 360) % 360);

                if (pitch < (360 - cutoffForward.get()) && pitch > 180.0f && pitchRate < cutoffRate.get() && currentLevel < valves.length - 1) {
                    setSuspensionLevel(currentLevel + 1);
                }

                if (pitch > cutoffBackward.get() && pitch < 180.0f && pitchRate > -cutoffRate.get() && currentLevel > 0) {
                    setSuspensionLevel(currentLevel - 1);
                }
            }
        });

        for (int i = 0; i < valves.length; i++) {
            Cluck.publish(QuasarHelios.testPrefix + "Suspension Level " + (i + 1), valves[i]);
        }*/
    }

    private static void setSuspensionLevel(int level) {
        if (level < 0 || level >= valves.length) {
            throw new IndexOutOfBoundsException();
        }

        for (int i = 0; i < valves.length; i++) {
            valves[i].set(i == level);
        }

        currentLevel = level;
    }
}
