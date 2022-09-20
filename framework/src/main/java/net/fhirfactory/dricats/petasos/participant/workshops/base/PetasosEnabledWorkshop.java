/*
 * Copyright (c) 2020 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.participant.workshops.base;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.WorkshopInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.model.component.valuesets.ComponentTypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.DefaultWorkshopSetEnum;
import net.fhirfactory.dricats.petasos.ipc.services.topology.PetasosTopologyReportingServiceAgent;
import net.fhirfactory.dricats.petasos.participant.processingplant.PetasosEnabledProcessingPlantInformationAccessor;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.DeploymentSite;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.InfrastructureNode;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.PlatformNode;
import net.fhirfactory.dricats.petasos.participant.wup.base.UnmonitoredWorkUnitProcessorBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

import static net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL;

public abstract class PetasosEnabledWorkshop extends PetasosParticipant implements WorkshopInterface {

    private boolean initialised;
    private ComponentStatusEnum readinessStatus;
    private ComponentStatusEnum livelinessStatus;
    private Map<String, UnmonitoredWorkUnitProcessorBase> workUnitProcessors;
    private Instant startupInstant;
    private static final Long MAXIMUM_STARTUP_DELAY = 180L;
    private static final Long SYSTEM_READINESS_CHECK_DAEMON_START_DELAY = 30000L;
    private static final Long SYSTEM_READINESS_CHECK_DAEMON_PERIOD = 30000L;

    @Inject
    private DeploymentSite deploymentSite;

    @Inject
    private InfrastructureNode infrastructureNode;

    @Inject
    private PlatformNode platformNode;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlantConfigurationService;

    @Inject
    private PetasosEnabledProcessingPlantInformationAccessor processingPlantServicesAccessor;

    @Inject
    private PetasosTopologyReportingServiceAgent topologyReportingServiceAgent;

    //
    // Constructor(s)
    //

    public PetasosEnabledWorkshop(){
        this.workUnitProcessors = new HashMap<>();
        setInitialised(false);
        PetasosParticipantId newId = new PetasosParticipantId(specifySubsystemName(),specifyParentParticipantName(),specifyParticipantName(), specifyParticipantVersion());
        if(StringUtils.isNotEmpty(specifyWorkshopName())) {
            newId.setId(UUID.randomUUID().toString());
            newId.setDisplayName(specifyWorkshopName() + "(" + newId.getId() + ")");
        }
        if(StringUtils.isNotEmpty(specifyWorkshopVersion())) {
            newId.setVersion(specifyWorkshopVersion());
        }
        newId.setIdValidityStartInstant(Instant.now());
        newId.setIdValidityEndInstant(Instant.MAX);
        setParticipantId(newId);
    }

    //
    // PostConstruct
    //

    @PostConstruct
    private void initialiseWorkshopBase() {
        getLogger().debug(".initialiseWorkshopBase(): Entry");
        if (isInitialised()) {
            getLogger().debug(".initialiseWorkshopBase(): Nothing to do, already initialised");
        } else {
            getLogger().info(".initialiseWorkshopBase(): Start...");
            invokePostConstructInitialisation();
            getLogger().trace(".initialiseWorkshopBase(): Node --> {}", getWorkshopPetasosParticipant());
            setInitialised(true);
            getLogger().info(".initialiseWorkshopBase(): Start...");
        }
    }

    //
    // Business Methods
    //

    // Register Workshop
    //

    public void registerWorkshop(){
        getProcessingPlantServicesAccessor().getProcessingPlant().addWorkshop(this);
    }

    // Readiness Watchdog
    //

    public void scheduleReadinessWatchdog(){
        getLogger().debug(".scheduleReadinessWatchdog(): Entry");
        TimerTask readinessStatusCheckDaemon = new TimerTask() {
            public void run() {
                getLogger().debug(".readinessStatusCheckDaemon(): Entry");
                checkReadinessStatus();
                getLogger().debug(".readinessStatusCheckDaemon(): Exit");
            }
        };
        Timer timer = new Timer("ReadinessStatusCheckDaemon");
        timer.schedule(readinessStatusCheckDaemon, SYSTEM_READINESS_CHECK_DAEMON_START_DELAY, SYSTEM_READINESS_CHECK_DAEMON_PERIOD);
        getLogger().debug(".scheduleReadinessWatchdog(): Exit");
    }

    public void checkReadinessStatus(){
        getLogger().debug(".checkReadinessStatus(): Start");
        boolean allReady = true;
        boolean someAreStarting = false;
        for(UnmonitoredWorkUnitProcessorBase currentWUP: getWorkUnitProcessors().values()){
            boolean isEdgeWorkshop = currentWUP.getComponentName().equals(DefaultWorkshopSetEnum.EDGE_WORKSHOP.getComponentName());
            boolean isInteractWorkshop = currentWUP.getComponentName().equals(DefaultWorkshopSetEnum.INTERACT_WORKSHOP.getComponentName());
            if(isEdgeWorkshop) {
                edgeWorkshopExists = true;
                edgeIsReady = currentWUP.getReadinessStatus().equals(SOFTWARE_COMPONENT_OPERATIONAL);
                edgeIsStarting = currentWorkshop.getReadinessStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
                if (!(edgeIsReady || edgeIsStarting)) {
                    allReady = false;
                }
            }
            if(isInteractWorkshop){
                interactWorkshopExists = true;
                interactIsReady = currentWorkshop.getReadinessStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
                interactIsStarting = currentWorkshop.getReadinessStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
                if (!(interactIsReady || interactIsStarting)) {
                    allReady = false;
                }
            }
        }

        if(interactWorkshopExists && edgeWorkshopExists){
            if(allReady){
                setReadinessStatus(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
            }
            if(edgeIsStarting || interactIsStarting){
                setReadinessStatus(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
            }
            if(!allReady && !edgeIsReady && !interactIsStarting){
                setReadinessStatus(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
            }
        }

        if(!getLivelinessStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL)){
            Long nowCount = Instant.now().getEpochSecond();
            Long startupCount = getStartupInstant().getEpochSecond();
            Long startupDuration = nowCount - startupCount;
            if(startupDuration > MAXIMUM_STARTUP_DELAY){
                setLivelinessStatus(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
            }
        }
        getLogger().debug(".checkReadinessStatus(): Finish");
    }

    //
    // Abstract
    //

    abstract protected String specifyWorkshopName();
    abstract protected String specifyWorkshopVersion();
    abstract protected String specifyParticipantName();
    abstract protected String specifyParticipantDisplayName();
    abstract protected String specifySubsystemName();
    abstract protected String specifyParentParticipantName();
    abstract protected String specifyParticipantVersion();

    abstract protected Logger getLogger();

    //
    // Getters (and Setters)
    //

    public DeploymentSite getDeploymentSite() {
        return deploymentSite;
    }

    public InfrastructureNode getInfrastructureNode() {
        return infrastructureNode;
    }

    public PlatformNode getPlatformNode() {
        return platformNode;
    }

    public ProcessingPlantConfigurationServiceInterface getProcessingPlantConfigurationService() {
        return processingPlantConfigurationService;
    }

    public PetasosEnabledProcessingPlantInformationAccessor getProcessingPlantServicesAccessor() {
        return processingPlantServicesAccessor;
    }

    public PetasosTopologyReportingServiceAgent getTopologyReportingServiceAgent() {
        return topologyReportingServiceAgent;
    }

    @Override
    public ComponentTypeEnum getWorkshopType(){
        return(ComponentTypeEnum.WORKSHOP);
    }

    @Override
    public ComponentId getComponentId() {
        return (getParticipantId());
    }

    @Override
    public ComponentStatusEnum getReadinessState() {
        return (this.readinessStatus);
    }

    @Override
    public ComponentStatusEnum getLivelinessState() {
        return (this.livelinessStatus);
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    public Map<String, UnmonitoredWorkUnitProcessorBase> getWorkUnitProcessors() {
        return workUnitProcessors;
    }

    public void setWorkUnitProcessors(Map<String, UnmonitoredWorkUnitProcessorBase> workUnitProcessors) {
        this.workUnitProcessors = workUnitProcessors;
    }
}
