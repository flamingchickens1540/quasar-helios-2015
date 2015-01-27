package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.instinct.InstinctMultiModule;

public class Autonomous {
    public static InstinctMultiModule mainModule = new InstinctMultiModule(ControlInterface.autoTuning);

    public static void setup() {
        mainModule.publishDefaultControls(true, true);
        mainModule.addMode(new AutonomousModeDrive());
        mainModule.addMode(new AutonomousModeOneTote());
        mainModule.addMode(new AutonomousModeFull());
        mainModule.loadSettings(mainModule.addNullMode("none", "I'm a sitting chicken!"));
        Igneous.registerAutonomous(mainModule);
    }
}
