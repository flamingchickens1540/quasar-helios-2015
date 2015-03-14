package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;
import ccre.log.Logger;

public class Pressure {
    public static final BooleanInput pressureSwitch = BooleanMixing.createDispatch(Igneous.getPCMPressureSwitch(), Igneous.constantPeriodic);
    public static FloatInput pressureGauge;
    public static final BooleanStatus compressor = new BooleanStatus(true);

    public static void setup() {
        compressor.send(Igneous.usePCMCompressor());
        QuasarHelios.publishFault("compressor-disabled", compressor.asInvertedInput(), compressor.getSetTrueEvent());
        FloatInput pressureInput = FloatMixing.createDispatch(Igneous.makeAnalogInput(0), Igneous.globalPeriodic);

        FloatStatus min = ControlInterface.mainTuning.getFloat("Pressure Min +M", 0.0f);
        FloatStatus max = ControlInterface.mainTuning.getFloat("Pressure Max +M", 1.0f);

        pressureGauge = FloatMixing.createDispatch(FloatMixing.normalizeFloat(pressureInput, min, max), Igneous.globalPeriodic);

        QuasarHelios.publishFault("underpressure", FloatMixing.floatIsAtMost(pressureGauge, 0.30f));
        QuasarHelios.publishFault("overpressure", FloatMixing.floatIsAtLeast(pressureGauge, 1.05f));

        BooleanInputPoll compressorRunning = Igneous.getPCMCompressorRunning();
        BooleanInput notPressurizingWhenItShouldBe = BooleanMixing.andBooleans(FloatMixing.floatIsAtMost(pressureGauge, 0.1f), BooleanMixing.createDispatch(compressorRunning, Igneous.globalPeriodic));
        ExpirationTimer notPressurizingWarning = new ExpirationTimer();
        notPressurizingWhenItShouldBe.send(notPressurizingWarning.getRunningControl());
        QuasarHelios.publishStickyFault("not-pressurizing", notPressurizingWarning.schedule(2000), BooleanMixing.onRelease(notPressurizingWhenItShouldBe));

        Cluck.publish("Pressure Switch", pressureSwitch);
        Cluck.publish("Pressure Gauge", pressureGauge);
        Cluck.publish("Pressure Gauge Raw", pressureInput);
        Cluck.publish("Pressure Compressor Enable", compressor);
        Cluck.publish("Pressure Min Set", FloatMixing.pumpEvent(pressureInput, min));
        Cluck.publish("Pressure Max Set", FloatMixing.pumpEvent(pressureInput, max));

        new Ticker(10000).send(() -> {
            Logger.fine("Pressure: " + pressureGauge.get() * 100 + "% (" + (compressorRunning.get() ? "compressing..." : "standby") + ")");
        });
    }
}
