package org.team1540.quasarhelios;

import java.util.ArrayList;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;
import ccre.igneous.IgneousApplication;
import ccre.log.Logger;
import ccre.rconf.RConf;
import ccre.rconf.RConf.Entry;
import ccre.rconf.RConfable;

/**
 * The main class for QuasarHelios. This dispatches to all of the other modules.
 */
public class QuasarHelios implements IgneousApplication {
    public static BooleanStatus autoLoader;
    public static BooleanStatus autoEjector;
    public static final EventInput globalControl = EventMixing.filterEvent(Igneous.getIsTest(), false, Igneous.globalPeriodic);
    public static final EventInput constantControl = EventMixing.filterEvent(Igneous.getIsTest(), false, Igneous.constantPeriodic);
    public static final EventInput readoutUpdate = new Ticker(100);
    public static final String testPrefix = "(Test) ";

    public void setupRobot() {
        Elevator.setup();
        autoLoader = AutoLoader.create();
        autoEjector = AutoEjector.create();
        ControlInterface.setup();
        HeadingSensor.setup();
        DriveCode.setup();
        Clamp.setup();
        Rollers.setup();
        Autonomous.setup();
        Pressure.setup();
        publishFaultRConf();
    }

    private static final ArrayList<String> faultNames = new ArrayList<>();
    private static final ArrayList<BooleanInputPoll> faults = new ArrayList<>();

    private static void publishFaultRConf() {
        if (faultNames.size() != faults.size()) {
            Logger.severe("Fault subsystem unavailable: mismatched list sizes.");
            return;
        }
        Cluck.publishRConf("quasar-faults", new RConfable() {
            public boolean signalRConf(int field, byte[] data) throws InterruptedException {
                return false;
            }

            public Entry[] queryRConf() throws InterruptedException {
                synchronized (QuasarHelios.class) {
                    Entry[] entries = new Entry[1 + faultNames.size()];
                    entries[0] = RConf.title("ALL FAULTS");
                    for (int i = 1; i < entries.length; i++) {
                        entries[i] = RConf.string(faultNames.get(i - 1) + ": " + (faults.get(i - 1).get() ? "FAULTING" : "nominal"));
                    }
                    return entries;
                }
            }
        });
    }

    // Should not be called once publishFaultRConf is called.
    public static BooleanInput publishFault(String name, BooleanInput object) {
        faultNames.add(name);
        faults.add(object);
        Cluck.publish("fault-" + name, object);
        return object;
    }

    public static BooleanInput publishFault(String name, BooleanInputPoll object) {
        return publishFault(name, BooleanMixing.createDispatch(object, readoutUpdate));
    }

    public static FloatStatus integrate(FloatInputPoll value, EventInput updateWhen) {
        FloatStatus output = new FloatStatus();
        updateWhen.send(new EventOutput() {
            private long lastRun = System.nanoTime();
            public void event() {
                long now = System.nanoTime();
                float add = value.get() * (lastRun - now) / 1000000000f;
                output.set(output.get() + add);
            }
        });
        return output;
    }
}
