package org.team1540.quasarhelios;

import java.util.ArrayList;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.DerivedFloatInput;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
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
    public static BooleanStatus autoHumanLoader;
    public static final EventInput globalControl = Igneous.globalPeriodic.andNot(Igneous.getIsTest());
    public static final BooleanInput isInManualControl = Igneous.getIsTest().or(Igneous.getIsAutonomous()).not();
    public static final EventInput manualControl = Igneous.globalPeriodic.and(isInManualControl);
    public static final EventInput constantControl = Igneous.constantPeriodic.andNot(Igneous.getIsTest());
    public static final EventInput readoutUpdate = new Ticker(100);

    public void setupRobot() {
        Cluck.publish("(DEBUG) Report Threads", new EventOutput() {
            public void event() {
                int count = Thread.activeCount();
                Thread[] thrs = new Thread[count];
                if (Thread.enumerate(thrs) != count) {
                    Logger.warning("Thread list modified!");
                } else {
                    for (Thread thr : thrs) {
                        Logger.info("Thread: " + thr);
                    }
                }
            }
        });
        Elevator.setup();
        autoLoader = AutoLoader.create();
        autoEjector = AutoEjector.create();
        autoStacker = AutoStacker.create();
        autoHumanLoader = AutoHumanLoader.create();
        autoHumanLoader.setFalseWhen(Igneous.startDisabled);
        ControlInterface.setup();
        HeadingSensor.setup();
        DriveCode.setup();
        Clamp.setup();
        Rollers.setup();
        Autonomous.setup();
        Pressure.setup();
        CurrentMonitoring.setup();
        ContainerGrabber.setup();
        publishFaultRConf();

        // This is to provide diagnostics in case of another crash due to OOM.
        new Ticker(60000).send(() -> {
            Logger.info("Current memory usage: " + (Runtime.getRuntime().freeMemory() / 1000) + "k free / " + (Runtime.getRuntime().maxMemory() / 1000) + "k max / " + (Runtime.getRuntime().totalMemory() / 1000) + "k total.");
        });
    }

    private static final ArrayList<String> faultNames = new ArrayList<>();
    private static final ArrayList<BooleanInput> faults = new ArrayList<>();
    private static final ArrayList<EventOutput> faultClears = new ArrayList<>();

    private static void publishFaultRConf() {
        if (faultNames.size() != faults.size()) {
            Logger.severe("Fault subsystem unavailable: mismatched list sizes.");
            return;
        }
        Cluck.publishRConf("quasar-faults", new RConfable() {
            public boolean signalRConf(int field, byte[] data) throws InterruptedException {
                field -= 2;// so that it's relative to faultClears.
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
                    Entry[] entries = new Entry[4 + faultNames.size()];
                    entries[0] = RConf.title("ALL FAULTS");
                    entries[1] = RConf.string("(click to clear sticky faults)");
                    for (int i = 2; i < entries.length - 2; i++) {
                        boolean faulting = faults.get(i - 2).get();
                        String str = faultNames.get(i - 2) + ": " + (faulting ? "FAULTING" : "nominal");
                        if (faultClears.get(i - 2) != null) {
                            str += " (S)";
                        }
                        if (faulting) {
                            entries[i] = RConf.button(str);
                        } else {
                            entries[i] = RConf.string(str);
                        }
                    }
                    entries[entries.length - 2] = RConf.button("(clear all)");
                    entries[entries.length - 1] = RConf.autoRefresh(1000);
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

    public static FloatStatus integrate(FloatInput value, EventInput updateWhen) {
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

    public static FloatInput limitSwitches(FloatInput value, BooleanInput forcePositive, BooleanInput forceNegative) {
        return new DerivedFloatInput(value, forcePositive, forceNegative) {
            protected float apply() {
                float f = value.get();
                if (forceNegative.get()) {
                    f = Math.min(0, f);
                }
                if (forcePositive.get()) {
                    f = Math.max(0, f);
                }
                return f;
            }
        };
    }
}
