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
    public static BooleanStatus autoStacker;
    public static final EventInput globalControl = EventMixing.filterEvent(Igneous.getIsTest(), false, Igneous.globalPeriodic);
    public static final EventInput manualControl = EventMixing.filterEvent(BooleanMixing.orBooleans(Igneous.getIsTest(), Igneous.getIsAutonomous()), false, Igneous.globalPeriodic);
    public static final EventInput constantControl = EventMixing.filterEvent(Igneous.getIsTest(), false, Igneous.constantPeriodic);
    public static final EventInput readoutUpdate = new Ticker(100);

    public void setupRobot() {
        Elevator.setup();
        autoLoader = AutoLoader.create();
        autoEjector = AutoEjector.create();
        autoStacker = AutoStacker.create();
        ControlInterface.setup();
        HeadingSensor.setup();
        DriveCode.setup();
        Clamp.setup();
        Rollers.setup();
        Autonomous.setup();
        Pressure.setup();
        CurrentMonitoring.setup();
        publishFaultRConf();

        // This is to provide diagnostics in case of another crash due to OOM.
        new Ticker(60000).send(() -> {
            Logger.info("Current memory usage: " + (Runtime.getRuntime().freeMemory() / 1000) + "k free / " + (Runtime.getRuntime().maxMemory() / 1000) + "k max / " + (Runtime.getRuntime().totalMemory() / 1000) + "k total.");
            System.gc();
            Logger.info("Post-GC-1 memory usage: " + (Runtime.getRuntime().freeMemory() / 1000) + "k free / " + (Runtime.getRuntime().maxMemory() / 1000) + "k max / " + (Runtime.getRuntime().totalMemory() / 1000) + "k total.");
            System.gc();
            Logger.info("Post-GC-2 memory usage: " + (Runtime.getRuntime().freeMemory() / 1000) + "k free / " + (Runtime.getRuntime().maxMemory() / 1000) + "k max / " + (Runtime.getRuntime().totalMemory() / 1000) + "k total.");
            System.gc();
            Logger.info("Post-GC-3 memory usage: " + (Runtime.getRuntime().freeMemory() / 1000) + "k free / " + (Runtime.getRuntime().maxMemory() / 1000) + "k max / " + (Runtime.getRuntime().totalMemory() / 1000) + "k total.");
            MemoryDumper.dumpHeap();
        });
    }

    private static final ArrayList<String> faultNames = new ArrayList<>();
    private static final ArrayList<BooleanInputPoll> faults = new ArrayList<>();
    private static final ArrayList<EventOutput> faultClears = new ArrayList<>();

    private static void publishFaultRConf() {
        if (faultNames.size() != faults.size()) {
            Logger.severe("Fault subsystem unavailable: mismatched list sizes.");
            return;
        }
        Cluck.publishRConf("quasar-faults", new RConfable() {
            public boolean signalRConf(int field, byte[] data) throws InterruptedException {
                field -= 2; // so that it's relative to faultClears.
                if (field >= 0 && field < faultClears.size()) {
                    EventOutput out = faultClears.get(field);
                    if (out != null) {
                        out.event();
                        return true;
                    }
                } else if (field == faultClears.size()) {
                    for (EventOutput out : faultClears) {
                        if (out != null) {
                            out.event();
                        }
                    }
                    return true;
                }
                return false;
            }

            public Entry[] queryRConf() throws InterruptedException {
                synchronized (QuasarHelios.class) {
                    Entry[] entries = new Entry[3 + faultNames.size()];
                    entries[0] = RConf.title("ALL FAULTS");
                    entries[1] = RConf.string("(click to clear sticky faults)");
                    for (int i = 2; i < entries.length - 1; i++) {
                        String str = faultNames.get(i - 2) + ": " + (faults.get(i - 2).get() ? "FAULTING" : "nominal");
                        if (faultClears.get(i - 2) == null) {
                            entries[i] = RConf.string(str); // not interactable
                        } else {
                            entries[i] = RConf.button(str); // interactable
                        }
                    }
                    entries[entries.length - 1] = RConf.button("(clear all)");
                    return entries;
                }
            }
        });
    }

    // These should not be called once publishFaultRConf is called.
    public static EventOutput publishStickyFault(String name) {
        BooleanStatus stickyValue = new BooleanStatus();
        publishFault(name, stickyValue, stickyValue.getSetFalseEvent());
        return stickyValue.getSetTrueEvent();
    }

    public static BooleanInput publishStickyFault(String name, EventInput fault) {
        BooleanStatus stickyValue = new BooleanStatus();
        stickyValue.setTrueWhen(fault);
        return publishFault(name, stickyValue, stickyValue.getSetFalseEvent());
    }

    public static BooleanInput publishStickyFault(String name, EventInput fault, EventInput clearFault) {
        BooleanStatus stickyValue = new BooleanStatus();
        stickyValue.setTrueWhen(fault);
        stickyValue.setFalseWhen(clearFault);
        return publishFault(name, stickyValue, stickyValue.getSetFalseEvent());
    }

    public static BooleanInput publishFault(String name, BooleanInput object) {
        return publishFault(name, object, null);
    }

    public static BooleanInput publishFault(String name, BooleanInput object, EventOutput stickyClear) {
        faultNames.add(name);
        faults.add(object);
        faultClears.add(stickyClear);
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
