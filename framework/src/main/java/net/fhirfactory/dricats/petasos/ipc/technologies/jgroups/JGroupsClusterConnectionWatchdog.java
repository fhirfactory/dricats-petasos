/*
 * Copyright (c) 2021 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.ipc.technologies.jgroups;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.interfaces.edge.PetasosHealthCheckCallBackInterface;
import net.fhirfactory.dricats.interfaces.pathway.PetasosRoutingChangeCallbackRegistrationInterface;
import net.fhirfactory.dricats.interfaces.pathway.PetasosRoutingChangeInterface;
import net.fhirfactory.dricats.model.petasos.ipc.endpoints.PetasosEndpoint;
import net.fhirfactory.dricats.model.petasos.ipc.endpoints.valuesets.JGroupsChannelStatusEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.components.endpoint.valuesets.EndpointStatusEnum;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.map.PetasosClusterMembershipSharedMap;
import net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.maps.JGroupsClusterConnectionStatusMap;

@ApplicationScoped
public class JGroupsClusterConnectionWatchdog
        implements PetasosHealthCheckCallBackInterface,
        PetasosRoutingChangeInterface,
        PetasosRoutingChangeCallbackRegistrationInterface {
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsClusterConnectionWatchdog.class);



    private JGroupsChannelStatusEnum aggregateStatus;
    private PetasosEndpoint edgeAnswerHTTP;
    private PetasosEndpoint edgeAnswerRPC;

    private boolean initialised;

    private Long STARTUP_CHECK_INITIAL_DELAY = 5000L;
    private Long STARTUP_CHECK_PERIOD = 5000L;
    private boolean startupCheckRequired;
    private Long MAX_STARTUP_DURATION = 90L;

    private Long WATCHDOG_INITIAL_START_DELAY = 10000L;
    private Long WATCHDOG_SCAN_PERIOD = 30000L;
    private boolean watchdogCheckRequired;

    private Instant startupTime;
    private Instant lastCheckTime;
    private Instant lastOperationalTime;
    private int suspectIterationCount;

    private int FAILED_ITERATION_MAX = 3;

    private List<PetasosRoutingChangeInterface> publisherChangeCallbacks;

    @Inject
    PetasosClusterMembershipSharedMap endpointMap;

    @Inject
    private JGroupsClusterConnectionStatusMap localConnectionSetStatus;

    //
    // Constructor
    //

    public JGroupsClusterConnectionWatchdog(){

        this.edgeAnswerHTTP = null;
        this.edgeAnswerRPC = null;
        this.aggregateStatus = JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED;

        this.initialised = false;

        this.startupCheckRequired = true;
        this.watchdogCheckRequired = false;

        this.startupTime = Instant.now();

        this.publisherChangeCallbacks = new ArrayList<>();
    }

    //
    // PostConstruct(or)
    //

    @PostConstruct
    public void initialise(){
        if(this.initialised){
            return;
        }
        scheduleStartupWatchdog();
        initialised = true;
    }

    //
    // Watchdog (Startup)
    //

    public void scheduleStartupWatchdog() {
        getLogger().debug(".scheduleStartupWatchdog(): Entry");
        TimerTask startupWatchdogTask = new TimerTask() {
            public void run() {
                getLogger().debug(".startupWatchdogTask(): Entry");
                startupWatchdog();
                if (!getAggregateStatus().equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED)) {
                    cancel();
                    scheduleOngoingStatusWatchdog();
                    startupCheckRequired = false;
                }
                getLogger().debug(".startupWatchdogTask(): Exit");
            }
        };
        if(startupCheckRequired) {
            Timer timer = new Timer("scheduleStartupWatchdog");
            timer.schedule(startupWatchdogTask, STARTUP_CHECK_INITIAL_DELAY, STARTUP_CHECK_PERIOD);
        }
        getLogger().debug(".scheduleStartupWatchdog(): Exit");
    }

    public void startupWatchdog(){
        getLogger().debug(".startupWatchdog(): Entry");
        Instant timeRightNow = Instant.now();
        setLastCheckTime(timeRightNow);
        Long startupTimeSoFar = timeRightNow.getEpochSecond() - startupTime.getEpochSecond();
        if(startupTimeSoFar > MAX_STARTUP_DURATION){
            setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_FAILED);
            getLogger().error(".startupWatchdog(): Core Petasos Endpoints have failed to startup (within defined startup period)!!!!");
            return;
        }
        JGroupsChannelStatusEnum currentStatus = localConnectionSetStatus.getAggregateStatus();
        if(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED)){
            setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED);
            getLogger().debug(".startupWatchdog(): Exit, Startup not completed, awaiting ports");
            return;
        }
        if(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL)){
            if(checkMinimumViablePortSetHasLaunched()) {
                setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL);
                getLogger().debug(".startupWatchdog(): Exit, Core Petasos Endpoints Startup Completed");
            } else {
                setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED);
                getLogger().debug(".startupWatchdog(): Exit, Startup not completed, awaiting ports");
            }
            return;
        }
        if(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED)){
            getLogger().debug(".startupWatchdog(): Exit, Core Petasos Endpoints Startup Continuing");
            return;
        }
        if(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_FAILED)) {
            setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_FAILED);
            getLogger().error(".startupWatchdog(): Core Petasos Endpoints Startup Failed!!!!");
            return;
        }
    }

    /**
     * This function is used to "qualify" which systems (ports) need to be up and running in-order to
     * assuming the CorePetasosEndpoint set is functional. It is split into 3: (1) the core single-site ports,
     * (2) multi-site communication ports and (3) the edgeAnswer ports. Change as deployment dictates.
     *
     * @return A conditional report on whether a MinimumViableProduct set of ports has launched.
     */
    private boolean checkMinimumViablePortSetHasLaunched(){
        JGroupsChannelStatusEnum currentStatus = localConnectionSetStatus.getAggregateStatus();
        if (currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED) || currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL)) {
        	return(true);
        
        } else {
        	return(false);
        }
    }

    //
    // Watchdog (Ongoing)
    //

    public void scheduleOngoingStatusWatchdog() {
        getLogger().debug(".scheduleOngoingStatusWatchdog(): Entry");
        TimerTask ongoingWatchdogTask = new TimerTask() {
            public void run() {
                getLogger().debug(".ongoingWatchdogTask(): Entry");
                statusWatchDog();
                getLogger().debug(".ongoingWatchdogTask(): Exit");
            }
        };
        Timer timer = new Timer("scheduleOngoingStatusWatchdog");
        timer.schedule(ongoingWatchdogTask, WATCHDOG_INITIAL_START_DELAY, WATCHDOG_SCAN_PERIOD);

        getLogger().debug(".scheduleOngoingStatusWatchdog(): Exit");
    }

    public void statusWatchDog(){
        getLogger().debug(".statusWatchDog(): Entry");
        Instant timeRightNow = Instant.now();
        setLastCheckTime(timeRightNow);
        JGroupsChannelStatusEnum currentStatus = localConnectionSetStatus.getAggregateStatus();
        switch(currentStatus){
            case JGROUPS_CHANNEL_LOCAL_STATUS_STARTED: {
                Long timeSinceStartup = Instant.now().getEpochSecond() - startupTime.getEpochSecond();
                if(timeSinceStartup > MAX_STARTUP_DURATION) {
                	setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_FAILED);
                	getLogger().error(".statusWatchDog(): Core Petasos Channels/Connectors failed!!!!");
                } 
                return;
            }
            case JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL:{
            	setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL);
            	getLogger().debug(".statusWatchDog(): Core Petasos Channels/Connectors Operational");
                return;
            }
            case JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN:{
                if(getSuspectIterationCount() > FAILED_ITERATION_MAX){
                    setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_FAILED);
                    getLogger().error(".statusWatchDog(): Core Petasos Endpoints have failed!!!!");
                    return;
                } else {
                    int updatedSuspectCount = getSuspectIterationCount() + 1;
                    setSuspectIterationCount(updatedSuspectCount);
                    return;
                }
            }
            case JGROUPS_CHANNEL_LOCAL_STATUS_FAILED:
            default:{
                setAggregateStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_FAILED);
                getLogger().warn(".statusWatchDog(): Core Petasos Channels/Connectors failed!!!!");
                return;
            }
        }
    }


    //
    // New publisher Status
    //

    @Override
    public void notifyNewParticipant(PetasosParticipant newPublisher) {
        if(newPublisher == null){
            return;
        }
        for(PetasosRoutingChangeInterface currentCallback: this.publisherChangeCallbacks){
            currentCallback.notifyNewParticipant(newPublisher);
        }
    }

    @Override
    public void registerPubSubCallbackChange(PetasosRoutingChangeInterface publisherChangeCallback) {
        this.publisherChangeCallbacks.add(publisherChangeCallback);
    }

    //
    // Getters (and Setters)
    //

    public JGroupsChannelStatusEnum getAggregateStatus() {
        return aggregateStatus;
    }

    public void setAggregateStatus(JGroupsChannelStatusEnum aggregateStatus) {
        this.aggregateStatus = aggregateStatus;
    }

    public PetasosClusterMembershipSharedMap getEndpointMap() {
        return endpointMap;
    }

    public boolean existsEdgeAnswerHTTP(){
        boolean exists = this.edgeAnswerHTTP != null;
        return(exists);
    }

    public PetasosEndpoint getEdgeAnswerHTTP() {
        return edgeAnswerHTTP;
    }

    public void setEdgeAnswerHTTP(PetasosEndpoint edgeAnswerHTTP) {
        this.edgeAnswerHTTP = edgeAnswerHTTP;
    }

    public boolean existsEdgeAnswerRPC(){
        boolean exists = this.edgeAnswerRPC != null;
        return(exists);
    }

    public PetasosEndpoint getEdgeAnswerRPC() {
        return edgeAnswerRPC;
    }

    public void setEdgeAnswerRPC(PetasosEndpoint edgeAnswerRPC) {
        this.edgeAnswerRPC = edgeAnswerRPC;
    }

    protected Logger getLogger() {
        return (LOG);
    }

    public Instant getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(Instant startupTime) {
        this.startupTime = startupTime;
    }

    public Instant getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(Instant lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public Instant getLastOperationalTime() {
        return lastOperationalTime;
    }

    public void setLastOperationalTime(Instant lastOperationalTime) {
        this.lastOperationalTime = lastOperationalTime;
    }

    public int getSuspectIterationCount() {
        return suspectIterationCount;
    }

    public void setSuspectIterationCount(int suspectIterationCount) {
        this.suspectIterationCount = suspectIterationCount;
    }

    @Override
    public EndpointStatusEnum getAggregatePetasosEndpointStatus() {
    	  JGroupsChannelStatusEnum currentStatus = localConnectionSetStatus.getAggregateStatus();
          switch(currentStatus){
              case JGROUPS_CHANNEL_LOCAL_STATUS_STARTED: 
            	  return(EndpointStatusEnum.DRICATS_ENDPOINT_STATUS_STARTED);
              case JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL:
            	  return(EndpointStatusEnum.DRICATS_ENDPOINT_STATUS_OPERATIONAL);
              case JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN:
            	  return(EndpointStatusEnum.DRICATS_ENDPOINT_STATUS_UNKNOWN);
              case JGROUPS_CHANNEL_LOCAL_STATUS_FAILED:
              default:{
                  return(EndpointStatusEnum.DRICATS_ENDPOINT_STATUS_FAILED);
              }
          }
    }
}
