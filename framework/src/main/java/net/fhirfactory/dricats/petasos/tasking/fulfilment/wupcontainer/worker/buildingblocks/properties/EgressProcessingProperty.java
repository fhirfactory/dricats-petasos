package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks.properties;

import net.fhirfactory.pegacorn.core.model.generalid.FDNToken;

public class EgressProcessingProperty {
    private boolean hasBeenProcessed;
    private FDNToken instanceName;

    public boolean isHasBeenProcessed() {
        return hasBeenProcessed;
    }

    public void setHasBeenProcessed(boolean hasBeenProcessed) {
        this.hasBeenProcessed = hasBeenProcessed;
    }

    public FDNToken getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(FDNToken instanceName) {
        this.instanceName = instanceName;
    }
}
