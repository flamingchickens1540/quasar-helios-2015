package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.BooleanInput;
import ccre.channel.FloatCell;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.frc.FRC;
import ccre.log.Logger;
import ccre.timers.ExpirationTimer;
import ccre.timers.Ticker;

public class Pressure {
    public static final BooleanInput pressureSwitch = FRC.pressureSwitchPCM();
    public static FloatInput pressureGauge;
    public static final BooleanCell compressor = new BooleanCell(true);

    public static void setup() {
        compressor.send(FRC.compressorPCM());
        QuasarHelios.publishFault("compressor-disabled", compressor.not(), compressor.eventSet(true));
        FloatInput pressureInput = FRC.analogInput(0);

        FloatCell min = ControlInterface.mainTuning.getFloat("Pressure Min +M", 0.0f);
        FloatCell max = ControlInterface.mainTuning.getFloat("Pressure Max +M", 1.0f);

        pressureGauge = pressureInput.normalize(min, max);

        QuasarHelios.publishFault("underpressure", pressureGauge.atMost(0.30f));
        QuasarHelios.publishFault("overpressure", pressureGauge.atLeast(1.05f));

        BooleanInput compressorRunning = FRC.compressorRunningPCM();
        BooleanInput notPressurizingWhenItShouldBe = pressureGauge.atMost(0.1f).and(compressorRunning);
        ExpirationTimer notPressurizingWarning = new ExpirationTimer();
        notPressurizingWhenItShouldBe.send(notPressurizingWarning.getRunningControl());
        QuasarHelios.publishStickyFault("not-pressurizing", notPressurizingWarning.schedule(2000), notPressurizingWhenItShouldBe.onRelease());

        Cluck.publish("Pressure Switch", pressureSwitch);
        Cluck.publish("Pressure Gauge", pressureGauge);
        Cluck.publish("Pressure Gauge Raw", pressureInput);
        Cluck.publish("Pressure Compressor Enable", compressor);
        Cluck.publish("Pressure Min Set", min.eventSet(pressureInput));
        Cluck.publish("Pressure Max Set", max.eventSet(pressureInput));

        new Ticker(10000).send(() -> {
            Logger.fine("Pressure: " + pressureGauge.get() * 100 + "% (" + (compressorRunning.get() ? "compressing..." : "standby") + ")");
        });
    }
}
