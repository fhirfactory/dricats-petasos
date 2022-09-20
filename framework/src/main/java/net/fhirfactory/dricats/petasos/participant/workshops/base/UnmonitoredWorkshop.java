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
import net.fhirfactory.dricats.interfaces.topology.PegacornTopologyFactoryInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.model.component.valuesets.ComponentTypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.components.wup.base.WorkUnitProcessorPetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.DefaultWorkshopSetEnum;
import net.fhirfactory.dricats.model.component.valuesets.InformationSystemComponentStatusEnum;
import net.fhirfactory.dricats.petasos.ipc.services.topology.PetasosTopologyReportingServiceAgent;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.DeploymentSite;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.InfrastructureNode;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.PlatformNode;
import net.fhirfactory.dricats.petasos.participant.processingplant.PetasosEnabledProcessingPlantInformationAccessor;
import net.fhirfactory.dricats.petasos.participant.wup.base.UnmonitoredWorkUnitProcessorBase;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeFDN;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeRDN;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

public abstract class UnmonitoredWorkshop extends RouteBuilder implements WorkshopInterface {

    private ComponentId componentId;
    private PetasosParticipant petasosParticipantInformation;
    private boolean workshopBaseIsInitialised;
    private ComponentStatusEnum readinessStatus;
    private ComponentStatusEnum livelinessStatus;
    private Map<String, UnmonitoredWorkUnitProcessorBase> workUnitProcessors;

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

    private Instant startupInstant;

    private static final Long MAXIMUM_STARTUP_DELAY = 180L;
    private static final Long SYSTEM_READINESS_CHECK_DAEMON_START_DELAY = 30000L;
    private static final Long SYSTEM_READINESS_CHECK_DAEMON_PERIOD = 30000L;

    public UnmonitoredWorkshop() {
        super();
        this.workUnitProcessors = new HashMap<>();
        setWorkshopBaseIsInitialised(false);
        ComponentId newId = new ComponentId();
        newId.setId(UUID.randomUUID().toString());
        newId.setDisplayName(specifyWorkshopName()+"("+newId.getId()+")");
        newId.setVersion(specifyWorkshopVersion());
        newId.setIdValidityStartInstant(Instant.now());
        newId.setIdValidityEndInstant(Instant.MAX);
        setComponentId(newId);
    }

    protected abstract Logger specifyLogger();
    protected Logger getLogger() {return(specifyLogger());}

    protected ProcessingPlantConfigurationServiceInterface getProcessingPlantConfigurationService(){
        return(processingPlantConfigurationService);
    }

    protected PetasosEnabledProcessingPlantInformationAccessor getProcessingPlantServicesAccessor(){
        return(processingPlantServicesAccessor);
    }

    abstract protected String specifyWorkshopName();
    abstract protected String specifyWorkshopVersion();
    abstract protected ComponentTypeEnum specifyWorkshopType();
    abstract protected void invokePostConstructInitialisation();

    //
    // PostConstruct Activities
    //

    @PostConstruct
    private void initialiseWorkshopBase() {
        getLogger().debug(".initialiseWorkshopBase(): Entry");
        if (isWorkshopBaseIsInitialised()) {
            getLogger().debug(".initialiseWorkshopBase(): Nothing to do, already initialised");
        } else {
            getLogger().info(".initialiseWorkshopBase(): Start...");
            invokePostConstructInitialisation();
            getLogger().trace(".initialiseWorkshopBase(): Node --> {}", getWorkshopPetasosParticipant());
            setWorkshopBaseIsInitialised(true);
            getLogger().info(".initialiseWorkshopBase(): Start...");
        }
    }

    @Override
    public void initialiseWorkshop(){
        initialiseWorkshopBase();
    }

    //
    // Dummy Route to ensure Startup
    //

    @Override
    public void configure() throws Exception {
        String fromString = "timer://" + getComponentName() + "-ingres" + "?repeatCount=1";

        from(fromString)
            .log(LoggingLevel.DEBUG, "Workshop --> ${body}");
    }


    @Override
    public WorkUnitProcessorPetasosParticipant getWUP(String wupName, String wupVersion) {
        getLogger().debug(".getWUP(): Entry, wupName --> {}, wupVersion --> {}", wupName, wupVersion);
        boolean found = false;
        WorkUnitProcessorPetasosParticipant foundWorkshop = null;
        for (TopologyNodeFDN containedWorkshopFDN : this.workshopNode.getWupSet()) {
            WorkUnitProcessorSoftwareComponent containedWorkshop = (WorkUnitProcessorSoftwareComponent) localSolutionMap.getNode(containedWorkshopFDN);
            TopologyNodeRDN testRDN = new TopologyNodeRDN(DricatsSoftwareComponentTypeEnum.WORKSHOP, wupName, wupVersion);
            if (testRDN.equals(containedWorkshop.getComponentRDN())) {
                found = true;
                foundWorkshop = containedWorkshop;
                break;
            }
        }
        if (found) {
            return (foundWorkshop);
        }
        return (null);
    }

    public WorkUnitProcessorPetasosParticipant getWUP(String workshopName){
        getLogger().debug(".getWorkshop(): Entry, workshopName --> {}", workshopName);
        String version = this.workshopNode.getComponentRDN().getNodeVersion();
        WorkUnitProcessorSoftwareComponent workshop = getWUP(workshopName, version);
        return(workshop);
    }

    //
    // Register Workshop
    //

    public void registerWorkshop(){
        getProcessingPlantServicesAccessor().getProcessingPlant().addWorkshop(this);
    }

    //
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
        for(UnmonitoredWorkUnitProcessorBase currentWorkshop: getWorkUnitProcessors().values()){
            boolean isEdgeWorkshop = currentWorkshop.getComponentName().equals(DefaultWorkshopSetEnum.EDGE_WORKSHOP.getComponentName());
            boolean isInteractWorkshop = currentWorkshop.getComponentName().equals(DefaultWorkshopSetEnum.INTERACT_WORKSHOP.getComponentName());
            if(isEdgeWorkshop) {
                edgeWorkshopExists = true;
                edgeIsReady = currentWorkshop.getReadinessStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
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
    // Liveliness Watchdog
    //

    public void scheduleLivelinessWatchdog(){
        getLogger().debug(".scheduleLivelinessWatchdog(): Entry");
        TimerTask livelinessStatusCheckDaemon = new TimerTask() {
            public void run() {
                getLogger().debug(".livelinessStatusCheckDaemon(): Entry");
                checkLivelinessStatus();
                getLogger().debug(".livelinessStatusCheckDaemon(): Exit");
            }
        };
        Timer timer = new Timer("LivelinessStatusCheckDaemon");
        timer.schedule(livelinessStatusCheckDaemon, SYSTEM_READINESS_CHECK_DAEMON_START_DELAY, SYSTEM_READINESS_CHECK_DAEMON_PERIOD);
        getLogger().debug(".scheduleLivelinessWatchdog(): Exit");
    }

    public void checkLivelinessStatus(){
        getLogger().debug(".checkLivelinessStatus(): Start");
        boolean allReadyOrStarting = true;
        boolean someStarting = false;
        for(UnmonitoredWorkUnitProcessorBase currentWorkshop: getWorkUnitProcessors().values()){
            boolean isReady = currentWorkshop.getStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
            boolean isStarting = currentWorkshop.getStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
            if (!(isReady || isStarting)) {
                allReadyOrStarting = false;
                break;
            }
        }

        if(allReadyOrStarting){
            for(UnmonitoredWorkUnitProcessorBase currentWorkshop: getWorkUnitProcessors().values()){
                boolean isStarting = currentWorkshop.getStatus().equals(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
                if (isStarting) {
                    someStarting = false;
                    break;
                }
            }
        }

        if(!allReadyOrStarting){
            setLivelinessStatus(InformationSystemComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
        }
        getLogger().debug(".checkLivelinessStatus(): Finish");
    }

    //
    // Getters and Setters
    //


    public ComponentId getComponentId() {
        return componentId;
    }

    public void setComponentId(ComponentId componentId) {
        this.componentId = componentId;
    }

    public InformationSystemComponentStatusEnum getReadinessStatus() {
        return readinessStatus;
    }

    public void setReadinessStatus(InformationSystemComponentStatusEnum readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public InformationSystemComponentStatusEnum getLivelinessStatus() {
        return livelinessStatus;
    }

    public void setLivelinessStatus(InformationSystemComponentStatusEnum livelinessStatus) {
        this.livelinessStatus = livelinessStatus;
    }

    public PetasosParticipant getPetasosParticipantInformation() {
        return petasosParticipantInformation;
    }

    public void setPetasosParticipantInformation(PetasosParticipant petasosParticipantInformation) {
        this.petasosParticipantInformation = petasosParticipantInformation;
    }

    public boolean isWorkshopBaseIsInitialised() {
        return workshopBaseIsInitialised;
    }

    public void setWorkshopBaseIsInitialised(boolean workshopBaseIsInitialised) {
        this.workshopBaseIsInitialised = workshopBaseIsInitialised;
    }

    public Instant getStartupInstant() {
        return startupInstant;
    }

    public void setStartupInstant(Instant startupInstant) {
        this.startupInstant = startupInstant;
    }

    public Map<String, UnmonitoredWorkUnitProcessorBase> getWorkUnitProcessors() {
        return workUnitProcessors;
    }

    public void setWorkUnitProcessors(Map<String, UnmonitoredWorkUnitProcessorBase> workUnitProcessors) {
        this.workUnitProcessors = workUnitProcessors;
    }

    public void addWorkUnitProcessor(UnmonitoredWorkUnitProcessorBase wup){
        getLogger().debug(".addWorkUnitProcessor(): Entry, wup->{}", wup);
        if(wup == null){
            getLogger().debug(".addWorkUnitProcessor(): Exit, wup is null, exiting");
        }
        if(getWorkUnitProcessors().containsKey(wup.getComponentId().getId())){
            getWorkUnitProcessors().remove(wup.getComponentId().getId());
        }
        getWorkUnitProcessors().put(wup.getComponentId().getId(), wup);
        getLogger().debug(".addWorkUnitProcessor(): Exit");
    }
}
