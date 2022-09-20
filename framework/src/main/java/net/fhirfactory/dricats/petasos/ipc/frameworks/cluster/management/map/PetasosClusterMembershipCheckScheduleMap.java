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
package net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.map;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.map.datatypes.PetasosClusterMembershipCheckScheduleElement;

public class PetasosClusterMembershipCheckScheduleMap {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosClusterMembershipCheckScheduleMap.class);

    // ConcurrentHashMap<channelName, PetasosClusterMembershipCheckScheduleElement>
    private ConcurrentHashMap<String, PetasosClusterMembershipCheckScheduleElement> integrationPointCheckSchedule;

    private Object integrationPointCheckScheduleLock;

    private int ENDPOINT_CHECK_DELAY=10;

//    @Inject
//    private JGroupsIntegrationPointNamingUtilities integrationPointNamingUtilities;

    //
    // Constructor(s)
    //

    public PetasosClusterMembershipCheckScheduleMap(){
        this.integrationPointCheckSchedule = new ConcurrentHashMap<>();
        this.integrationPointCheckScheduleLock = new Object();
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Scheduled Activities
    //

    //
    // Check Schedule Management
    //

    public void scheduleJGroupsIntegrationPointCheck(JGroupsChannelConnectorSummary ipSummary, boolean endpointRemoved, boolean endpointAdded, int checkCount ) {
        getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Entry, ipSummary->{}, endpointRemoved->{}, endpointAdded->{}, checkCount->{}", ipSummary, endpointRemoved, endpointAdded, checkCount);
        if(ipSummary == null){
            getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Exit, ipSummary is null");
            return;
        }
        if(StringUtils.isEmpty(ipSummary.getChannelName())){
            getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Exit, ipSummary.getChannelName() is empty");
            return;
        }
        if(!endpointRemoved && !endpointAdded){
            getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Exit, neither endpointRemoved or endpointAdded is set");
            return;
        }
        synchronized (this.integrationPointCheckScheduleLock) {
            if (this.integrationPointCheckSchedule.containsKey(ipSummary.getSubsystemParticipantName())) {
                getLogger().trace(".scheduleJGroupsIntegrationPointCheck(): Exit, already scheduled");
            } else {
                PetasosClusterMembershipCheckScheduleElement newScheduleElement = new PetasosClusterMembershipCheckScheduleElement(ipSummary, endpointRemoved, endpointAdded, checkCount);
                this.integrationPointCheckSchedule.put(ipSummary.getSubsystemParticipantName(), newScheduleElement);
                getLogger().trace(".scheduleJGroupsIntegrationPointCheck(): Exit, check scheduled");
            }
        }
    }

    public void scheduleJGroupsIntegrationPointCheck(JGroupsChannelConnectorSummary ipSummary, boolean endpointRemoved, boolean endpointAdded ){
        getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Entry, ipSummary->{}, endpointRemoved->{}, endpointAdded->{} ", ipSummary, endpointRemoved, endpointAdded);
        if(ipSummary == null){
            getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Exit, ipSummary is null");
            return;
        }
        if(StringUtils.isEmpty(ipSummary.getChannelName())){
            getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Exit, ipSummary.getChannelName() is empty");
            return;
        }
        if(!endpointRemoved && !endpointAdded){
            getLogger().debug(".scheduleJGroupsIntegrationPointCheck(): Exit, neither endpointRemoved or endpointAdded is set");
            return;
        }
        synchronized (this.integrationPointCheckScheduleLock) {
            if (this.integrationPointCheckSchedule.containsKey(ipSummary.getSubsystemParticipantName())) {
                getLogger().trace(".scheduleJGroupsIntegrationPointCheck(): Exit, already scheduled");
            } else {
                PetasosClusterMembershipCheckScheduleElement newScheduleElement = new PetasosClusterMembershipCheckScheduleElement(ipSummary, endpointRemoved, endpointAdded);
                this.integrationPointCheckSchedule.put(ipSummary.getSubsystemParticipantName(), newScheduleElement);
                getLogger().trace(".scheduleJGroupsIntegrationPointCheck(): Exit, check scheduled");
            }
        }
    }

    public List<PetasosClusterMembershipCheckScheduleElement> getEndpointsToCheck(){
        getLogger().debug(".getEndpointsToCheck(): Entry");
        List<PetasosClusterMembershipCheckScheduleElement> endpointSet = new ArrayList<>();
        if(this.integrationPointCheckSchedule.isEmpty()){
            getLogger().debug(".getEndpointsToCheck(): Exit, schedule is empty");
            return(endpointSet);
        }
        synchronized (this.integrationPointCheckScheduleLock) {
            Set<String> endpointNameSet = this.integrationPointCheckSchedule.keySet();

            for (String currentEndpointName : endpointNameSet) {
                PetasosClusterMembershipCheckScheduleElement currentElement = this.integrationPointCheckSchedule.get(currentEndpointName);
                getLogger().trace(".getEndpointsToCheck(): Checking entry ->{}", currentElement);
                boolean appropriateDelayPassed = (currentElement.getTargetTime().getEpochSecond() + ENDPOINT_CHECK_DELAY) < (Instant.now().getEpochSecond());
                if (appropriateDelayPassed) {
                    getLogger().trace(".getEndpointsToCheck(): Adding...");
                    endpointSet.add(currentElement);
                }
            }
            for (PetasosClusterMembershipCheckScheduleElement currentScheduleElement : endpointSet) {
                this.integrationPointCheckSchedule.remove(currentScheduleElement.getJgroupsIPSummary().getSubsystemParticipantName());
            }
        }
        getLogger().debug(".getEndpointsToCheck(): Exit, size->{}, remaining entries size->{}", endpointSet.size(), this.integrationPointCheckSchedule.size());
        return(endpointSet);
    }

    public boolean isCheckScheduleIsEmpty(){
        Set<String> endpointNameSet = this.integrationPointCheckSchedule.keySet();
        for(String currentName: endpointNameSet){
            getLogger().debug(".isCheckScheduleIsEmpty(): Entry->{}", currentName);
        }
        if(this.integrationPointCheckSchedule.isEmpty()){
            return(true);
        } else {
            return(false);
        }
    }
}
