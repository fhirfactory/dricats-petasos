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
package net.fhirfactory.dricats.petasos.participant.processingplant;

import net.fhirfactory.dricats.configuration.api.services.processingplant.EdgeProcessingPlantConfigurationService;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.WorkshopInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ConcurrencyModeEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ResilienceModeEnum;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.DefaultWorkshopSetEnum;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.NetworkSecurityZoneEnum;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.petasos.participant.workshops.base.UnmonitoredWorkshop;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

public abstract class ProcessingPlantBase  extends RouteBuilder {
    private ComponentId componentId;
    private String componentName;
    private String componentVersion;
    private NetworkSecurityZoneEnum networkSecurityZone;
    private ResilienceModeEnum resilienceMode;
    private ConcurrencyModeEnum concurrencyMode;
    private ComponentStatusEnum livelinessStatus;
    private ComponentStatusEnum readinessStatus;
    private Map<String, UnmonitoredWorkshop> workshops;
    private boolean processingPlantBaseInitialised;
    private Instant startupInstant;

    private static final Long MAXIMUM_STARTUP_DELAY = 180L;
    private static final Long SYSTEM_READINESS_CHECK_DAEMON_START_DELAY = 30000L;
    private static final Long SYSTEM_READINESS_CHECK_DAEMON_PERIOD = 30000L;

    @Inject
    private EdgeProcessingPlantConfigurationService processingPlantConfigurationService;

    //
    // Constructor(s)
    //

    public ProcessingPlantBase(){
        super();
        this.componentId = null;
        this.networkSecurityZone = NetworkSecurityZoneEnum.INTERNET;
        this.resilienceMode = ResilienceModeEnum.RESILIENCE_MODE_STANDALONE;
        this.concurrencyMode = ConcurrencyModeEnum.CONCURRENCY_MODE_STANDALONE;
        this.livelinessStatus = ComponentStatusEnum.SOFTWARE_COMPONENT_STATUS_UNKNOWN;
        this.workshops = new HashMap<String, UnmonitoredWorkshop>();
        this.processingPlantBaseInitialised = false;
        this.startupInstant = Instant.now();
    }

    //
    // Post Construct
    //

    @PostConstruct
    protected void processingPlantBaseInitialisation(){
        getLogger().debug(".processingPlantBaseInitialisation(): Entry");
        if(isProcessingPlantBaseInitialised()) {
            getLogger().debug(".processingPlantBaseInitialisation(): Already initialised, nothing to do!");
        } else {
            getLogger().info("ProcessingPlant::initialise(): [Initialise Component Identification] Start");
            setComponentName(getProcessingPlantConfigurationService().getProcessingPlantPropertyFile().getSubsystemInstant().getSubsystemParticipantName());
            setComponentVersion(getProcessingPlantConfigurationService().getProcessingPlantPropertyFile().getSubsystemInstant().getSubsystemVersion());
            ComponentId newId = new ComponentId();
            newId.setId(UUID.randomUUID().toString());
            newId.setDisplayName(getComponentName()+"("+newId.getId()+")");
            newId.setIdValidityStartInstant(Instant.now());
            newId.setIdValidityEndInstant(Instant.MAX);
            setComponentId(newId);
            getLogger().info("ProcessingPlant::initialise(): [Initialise Component Identification] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Initialise Readiness Watchdog] Start");
            scheduleReadinessWatchdog();
            getLogger().info("ProcessingPlant::initialise(): [Initialise Readiness Watchdog] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Initialise Liveliness Watchdog] Start");
            scheduleLivelinessWatchdog();
            getLogger().info("ProcessingPlant::initialise(): [Initialise Liveliness Watchdog] Finish");
           
            doPostConstructInitialisation();
        }
    }

    //
    // Abstract Methods
    //

    protected abstract Logger getLogger();
    protected abstract void doPostConstructInitialisation();

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
        boolean edgeWorkshopExists = false;
        boolean interactWorkshopExists = false;
        boolean edgeIsReady = false;
        boolean edgeIsStarting = false;
        boolean interactIsReady = false;
        boolean interactIsStarting = false;
        for(UnmonitoredWorkshop currentWorkshop: getWorkshops().values()){
            boolean isEdgeWorkshop = currentWorkshop.getComponentName().equals(DefaultWorkshopSetEnum.EDGE_WORKSHOP.getComponentName());
            boolean isInteractWorkshop = currentWorkshop.getComponentName().equals(DefaultWorkshopSetEnum.INTERACT_WORKSHOP.getComponentName());
            if(isEdgeWorkshop) {
                edgeWorkshopExists = true;
                edgeIsReady = currentWorkshop.getReadinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
                edgeIsStarting = currentWorkshop.getReadinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
                if (!(edgeIsReady || edgeIsStarting)) {
                    allReady = false;
                }
            }
            if(isInteractWorkshop){
                interactWorkshopExists = true;
                interactIsReady = currentWorkshop.getReadinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
                interactIsStarting = currentWorkshop.getReadinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
                if (!(interactIsReady || interactIsStarting)) {
                    allReady = false;
                }
            }
        }

        if(interactWorkshopExists && edgeWorkshopExists){
            if(allReady){
                setReadinessStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
            }
            if(edgeIsStarting || interactIsStarting){
                setReadinessStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
            }
            if(!allReady && !edgeIsReady && !interactIsStarting){
                setReadinessStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
            }
        }

        if(!getLivelinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL)){
            Long nowCount = Instant.now().getEpochSecond();
            Long startupCount = getStartupInstant().getEpochSecond();
            Long startupDuration = nowCount - startupCount;
            if(startupDuration > MAXIMUM_STARTUP_DELAY){
                setLivelinessStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
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
        for(UnmonitoredWorkshop currentWorkshop: getWorkshops().values()){
            boolean isReady = currentWorkshop.getReadinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
            boolean isStarting = currentWorkshop.getReadinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
            if (!(isReady || isStarting)) {
                allReadyOrStarting = false;
                break;
            }
        }

        if(allReadyOrStarting){
            for(UnmonitoredWorkshop currentWorkshop: getWorkshops().values()){
                boolean isStarting = currentWorkshop.getReadinessStatus().equals(ComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
                if (isStarting) {
                    someStarting = false;
                    break;
                }
            }
        }

        if(!allReadyOrStarting){
            setLivelinessStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
        }
        getLogger().debug(".checkLivelinessStatus(): Finish");
    }


    //
    // Dummy Route to ensure Startup
    //

    @Override
    public void configure() throws Exception {
        String processingPlantName = getComponentName();

        from("timer://"+processingPlantName+"?delay=1000&repeatCount=1")
                .routeId("ProcessingPlant::"+processingPlantName)
                .log(LoggingLevel.DEBUG, "Starting....");
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

    public NetworkSecurityZoneEnum getNetworkSecurityZone() {
        return networkSecurityZone;
    }

    public void setNetworkSecurityZone(NetworkSecurityZoneEnum networkSecurityZone) {
        this.networkSecurityZone = networkSecurityZone;
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

    public ComponentStatusEnum getLivelinessStatus() {
        return livelinessStatus;
    }

    public void setLivelinessStatus(ComponentStatusEnum livelinessStatus) {
        this.livelinessStatus = livelinessStatus;
    }

    public Map<String, UnmonitoredWorkshop> getWorkshops() {
        return workshops;
    }

    protected void setWorkshops(Map<String, UnmonitoredWorkshop> workshops) {
        this.workshops.clear();
        if(workshops != null){
            for(String currentEntry: workshops.keySet()){
                this.workshops.put(currentEntry, workshops.get(currentEntry));
            }
        }
    }

    public void addWorkshop(WorkshopInterface workshop){
        getLogger().debug(".addWorkshop(): Entry, workshop->{}", workshop);
        if(workshop == null){
            getLogger().debug(".addWorkshop(): Exit, workshop is null");
            return;
        }
        String existingWorkshopId = null;
        for(String id: workshops.keySet()){
            if(id.equals(workshop.getComponentId())){
                existingWorkshopId = id;
                break;
            }
        }
        if(existingWorkshopId != null){
            workshops.remove(existingWorkshopId);
        }
        workshops.put(workshop.getComponentId().getId(), workshop);
    }

    public boolean isProcessingPlantBaseInitialised() {
        return processingPlantBaseInitialised;
    }

    public void setProcessingPlantBaseInitialised(boolean processingPlantBaseInitialised) {
        this.processingPlantBaseInitialised = processingPlantBaseInitialised;
    }

    public Instant getStartupInstant(){
        return(startupInstant);
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

    public void setStartupInstant(Instant startupInstant) {
        this.startupInstant = startupInstant;
    }

    public ComponentStatusEnum getReadinessStatus() {
        return readinessStatus;
    }

    public void setReadinessStatus(ComponentStatusEnum readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public EdgeProcessingPlantConfigurationService getProcessingPlantConfigurationService() {
        return processingPlantConfigurationService;
    }
}
