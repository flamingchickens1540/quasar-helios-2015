package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;
import ccre.log.Logger;

public class Pressure {
    public static final BooleanInput pressureSwitch = Igneous.getPCMPressureSwitch();
    public static FloatInput pressureGauge;
    public static final BooleanStatus compressor = new BooleanStatus(true);

    public static void setup() {
        compressor.send(Igneous.usePCMCompressor());
        QuasarHelios.publishFault("compressor-disabled", compressor.not(), compressor.getSetTrueEvent());
        FloatInput pressureInput = Igneous.makeAnalogInput(0);

        FloatStatus min = ControlInterface.mainTuning.getFloat("Pressure Min +M", 0.0f);
        FloatStatus max = ControlInterface.mainTuning.getFloat("Pressure Max +M", 1.0f);

        pressureGauge = pressureInput.normalize(min, max);

        QuasarHelios.publishFault("underpressure", pressureGauge.atMost(0.30f));
        QuasarHelios.publishFault("overpressure", pressureGauge.atLeast(1.05f));

        BooleanInput compressorRunning = Igneous.getPCMCompressorRunning();
        BooleanInput notPressurizingWhenItShouldBe = pressureGauge.atMost(0.1f).and(compressorRunning);
        ExpirationTimer notPressurizingWarning = new ExpirationTimer();
        notPressurizingWhenItShouldBe.send(notPressurizingWarning.getRunningControl());
        QuasarHelios.publishStickyFault("not-pressurizing", notPressurizingWarning.schedule(2000), notPressurizingWhenItShouldBe.onRelease());

        Cluck.publish("Pressure Switch", pressureSwitch);
        Cluck.publish("Pressure Gauge", pressureGauge);
        Cluck.publish("Pressure Gauge Raw", pressureInput);
        Cluck.publish("Pressure Compressor Enable", compressor);
        Cluck.publish("Pressure Min Set", min.getSetEvent(pressureInput));
        Cluck.publish("Pressure Max Set", max.getSetEvent(pressureInput));

        new Ticker(10000).send(() -> {
            Logger.fine("Pressure: " + pressureGauge.get() * 100 + "% (" + (compressorRunning.get() ? "compressing..." : "standby") + ")");
        });
    }
}
