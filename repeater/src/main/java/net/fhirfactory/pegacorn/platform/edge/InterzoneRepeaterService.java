package net.fhirfactory.pegacorn.platform.edge;

import net.fhirfactory.pegacorn.platform.edge.messaging.InterZoneRepeater;

public abstract class InterzoneRepeaterService {

    private static InterZoneRepeater ipcRouter = new InterZoneRepeater();

    public static void main(String[] args) {
        ipcRouter.run();
        System.exit(0);
    }
}
