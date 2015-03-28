package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.ExtendedMotorFailureException;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;
import ccre.log.Logger;
import ccre.util.Utils;

public class CANTalonWrapper {
    private final ExtendedMotor talon;
    private final FloatOutput output;
    private final String name;

    public CANTalonWrapper(String name, int port) {
        this.name = name;
        this.talon = Igneous.makeCANTalon(port);
        FloatOutput out = null;
        try {
            out = talon.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);
        } catch (ExtendedMotorFailureException e) {
            Logger.severe("Could not initialize elevator CAN", e);
        }
        this.output = (out == null) ? FloatMixing.ignoredFloatOutput : out;

        publishToCluck();
    }

    private void publishToCluck() {
        Cluck.publish(name + " Enable", talon.asEnable());
        Cluck.publish(name + " Bus Voltage", FloatMixing.createDispatch(talon.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE), QuasarHelios.readoutUpdate));
        Cluck.publish(name + " Output Current", FloatMixing.createDispatch(talon.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT), QuasarHelios.readoutUpdate));
        Cluck.publish(name + " Output Voltage", FloatMixing.createDispatch(talon.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE), QuasarHelios.readoutUpdate));
        Cluck.publish(name + " Temperature", FloatMixing.createDispatch(talon.asStatus(ExtendedMotor.StatusType.TEMPERATURE), QuasarHelios.readoutUpdate));
        Cluck.publish(name + " Any Fault", QuasarHelios.publishFault("elevator-can", talon.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT)));
        Cluck.publish(name + " Bus Voltage Fault", BooleanMixing.createDispatch(talon.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT), QuasarHelios.readoutUpdate));
        Cluck.publish(name + " Temperature Fault", BooleanMixing.createDispatch(talon.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT), QuasarHelios.readoutUpdate));
    }

    public EventInput setupCurrentBreakerWithFaultPublish(String faultName) {
        EventInput evt = setupCurrentBreaker();
        QuasarHelios.publishStickyFault(faultName, evt);
        return evt;
    }

    public EventInput setupCurrentBreaker() {
        FloatInputPoll current = talon.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT);
        BooleanInputPoll maxCurrentNow = FloatMixing.floatIsAtLeast(current, ControlInterface.mainTuning.getFloat(name + " Max Current Amps +M", 45));
        EventInput maxCurrentEvent = EventMixing.filterEvent(maxCurrentNow, true, Igneous.constantPeriodic);
        Cluck.publish(name + " Max Current Amps Reached", maxCurrentEvent);
        return maxCurrentEvent;
    }

    public FloatOutput getMotor() {
        return output;
    }

    public FloatOutput getAdvanced(float ramping, String controlName) {
        final FloatStatus wantedS = new FloatStatus();
        BooleanStatus doRamping = new BooleanStatus(true);
        Cluck.publish(name + " Ramping Enabled", doRamping);
        Igneous.constantPeriodic.send(new EventOutput() {
            private float last = wantedS.get();

            public void event() {
                float wanted = wantedS.get();
                if (Math.abs(wanted) < Math.abs(last) || !doRamping.get()) {
                    last = wanted;
                } else {
                    last = Utils.updateRamping(last, wanted, ramping);
                }
                output.set(last);
            }
        });
        Cluck.publish(controlName, (FloatOutput) wantedS);
        return wantedS;
    }
}
