/*
 * Copyright (c) 2022 Mark A. Hunter
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.dricats.petasos.participant.wup.base;

import net.fhirfactory.dricats.camel.BaseRouteBuilder;
import net.fhirfactory.dricats.configuration.api.services.processingplant.EdgeProcessingPlantConfigurationService;
import net.fhirfactory.dricats.interfaces.petasos.participant.endpoint.WorkUnitProcessorEndpointInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ConcurrencyModeEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ResilienceModeEnum;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.petasos.participant.processingplant.PetasosEnabledProcessingPlantInformationAccessor;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.DeploymentSite;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.InfrastructureNode;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.PlatformNode;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

public abstract class UnmonitoredWorkUnitProcessorBase extends BaseRouteBuilder {
    private ComponentId componentId;
    private String componentName;
    private String componentVersion;
    private boolean workUnitProcessorBaseInitialised;
    private ComponentStatusEnum operationalStatus;
    private ResilienceModeEnum resilienceMode;
    private ConcurrencyModeEnum concurrencyMode;
    private WorkUnitProcessorEndpointInterface ingresEndpoint;
    private WorkUnitProcessorEndpointInterface egressEndpoint;

    @Inject
    private PetasosEnabledProcessingPlantInformationAccessor petasosEnabledProcessingPlantServicesAccessor;

    @Inject
    private EdgeProcessingPlantConfigurationService processingPlantConfigurationService;

    @Inject
    private DeploymentSite deploymentSite;

    @Inject
    private InfrastructureNode infrastructureNode;

    @Inject
    private PlatformNode platformNode;

    //
    // Constructor(s)
    //

    public UnmonitoredWorkUnitProcessorBase(){
        super();
        this.componentName = specifyComponentName();
        this.componentVersion = specifyComponentVersion();
        ComponentId newId = new ComponentId();
        newId.setId(UUID.randomUUID().toString());
        newId.setDisplayName(specifyComponentName()+"("+newId.getId()+")");
        newId.setIdValidityStartInstant(Instant.now());
        newId.setIdValidityEndInstant(Instant.MAX);
        setComponentId(newId);
        this.workUnitProcessorBaseInitialised = false;
        setOperationalStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
    }

    //
    // PostConstruct
    //

    @PostConstruct
    protected void initialiseWorkUnitProcessorBase(){
        getLogger().debug(".initialiseWorkUnitProcessorBase(): Entry");
        if(isWorkUnitProcessorBaseInitialised()){
            getLogger().debug(".initialiseWorkUnitProcessorBase(): Already initialised, nothing to do");
        } else {
            getLogger().info(".initialiseWorkUnitProcessorBase(): Initialisation Start...");
            setConcurrencyMode(getPetasosEnabledProcessingPlantServicesAccessor().getProcessingPlant().getConcurrencyMode());

            setWorkUnitProcessorBaseInitialised(true);
            getLogger().info(".initialiseWorkUnitProcessorBase(): Initialisation Finished...");
        }
    }

    //
    // Abstract Methods
    //

    protected abstract Logger getLogger();
    protected abstract String specifyComponentName();
    protected abstract String specifyComponentVersion();

    protected abstract WorkUnitProcessorEndpointInterface specifyIngresEndpoint();
    protected abstract WorkUnitProcessorEndpointInterface specifyEgressEndpoint();

    //
    // Getters and Setters
    //

    public ComponentId getComponentId() {
        return componentId;
    }

    public void setComponentId(ComponentId componentId) {
        this.componentId = componentId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }

    public boolean isWorkUnitProcessorBaseInitialised() {
        return workUnitProcessorBaseInitialised;
    }

    public void setWorkUnitProcessorBaseInitialised(boolean workUnitProcessorBaseInitialised) {
        this.workUnitProcessorBaseInitialised = workUnitProcessorBaseInitialised;
    }

    public ComponentStatusEnum getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(ComponentStatusEnum operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    protected PetasosEnabledProcessingPlantInformationAccessor getPetasosEnabledProcessingPlantServicesAccessor() {
        return petasosEnabledProcessingPlantServicesAccessor;
    }

    protected EdgeProcessingPlantConfigurationService getProcessingPlantConfigurationService() {
        return processingPlantConfigurationService;
    }

    protected DeploymentSite getDeploymentSite() {
        return deploymentSite;
    }

    protected InfrastructureNode getInfrastructureNode() {
        return infrastructureNode;
    }

    protected PlatformNode getPlatformNode() {
        return platformNode;
    }

    public ResilienceModeEnum getResilienceMode() {
        return resilienceMode;
    }

    public void setResilienceMode(ResilienceModeEnum resilienceMode) {
        this.resilienceMode = resilienceMode;
    }

    public ConcurrencyModeEnum getConcurrencyMode() {
        return concurrencyMode;
    }

    public void setConcurrencyMode(ConcurrencyModeEnum concurrencyMode) {
        this.concurrencyMode = concurrencyMode;
    }

    public void setPetasosEnabledProcessingPlantServicesAccessor(PetasosEnabledProcessingPlantInformationAccessor petasosEnabledProcessingPlantServicesAccessor) {
        this.petasosEnabledProcessingPlantServicesAccessor = petasosEnabledProcessingPlantServicesAccessor;
    }

    public WorkUnitProcessorEndpointInterface getIngresEndpoint() {
        return ingresEndpoint;
    }

    public WorkUnitProcessorEndpointInterface getEgressEndpoint() {
        return egressEndpoint;
    }

    public void setIngressEndpoint(WorkUnitProcessorEndpointInterface ingressEndpoint) {
        this.ingresEndpoint = ingressEndpoint;
    }

    public void setEgressEndpoint(WorkUnitProcessorEndpointInterface egressEndpoint) {
        this.egressEndpoint = egressEndpoint;
    }
}
