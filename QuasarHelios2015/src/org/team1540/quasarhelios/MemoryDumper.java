package org.team1540.quasarhelios;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ccre.log.Logger;

public class MemoryDumper { // based on http://stackoverflow.com/a/12297339/3369324
    private static volatile Object hotspotMBean;

    public static void dumpHeap() {
        if (!new File("/tmp/do-dumping").exists()) {
            Logger.fine("Not dumping.");
            return;
        }
        try {
            initHotspotMBean();
            Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            File temp = File.createTempFile("heap-dump-" + System.currentTimeMillis() + "-", ".bin");
            temp.delete();
            Method m = clazz.getMethod("dumpHeap", String.class, boolean.class);
            m.setAccessible(true);
            m.invoke(hotspotMBean, temp.getAbsolutePath(), true);
            Logger.info("Dumped heap to " + temp.getAbsolutePath());
        } catch (ClassNotFoundException | IOException | NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
            Logger.warning("Could not dump heap", e);
        }
    }

    private static void initHotspotMBean() throws ClassNotFoundException, IOException {
        if (hotspotMBean == null) {
            synchronized (MemoryDumper.class) {
                if (hotspotMBean == null) {
                    hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
                            "com.sun.management:type=HotSpotDiagnostic", Class.forName("com.sun.management.HotSpotDiagnosticMXBean"));
                }
            }
        }
    }
}
