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
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.internals.fhir.r4.resources.endpoint.factories.EndpointPayloadTypeFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.identifier.PegacornIdentifierFactory;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelNamingUtilities;

@ApplicationScoped
public class PetasosClusterMembershipSharedMap {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosClusterMembershipSharedMap.class);

    // ConcurrentHashMap<channelName, PetasosEndpoint>
    private ConcurrentHashMap<String, JGroupsChannelConnectorSummary> integrationPoints;
    // ConcurrentHashMap<channelName, Object>
    private ConcurrentHashMap<String, Object> integrationPointLocks;
    // ConcurrentHashMap<subsystemName, List<channelName>>
    private ConcurrentHashMap<String, List<String>> integrationPointSubsystemMap;
    private Object integrationPointProcessingPlantServiceMapLock;

    @Inject
    private PegacornIdentifierFactory identifierFactory;

    @Inject
    private EndpointPayloadTypeFactory payloadTypeFactory;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlantInterface;

    @Inject
    private JGroupsChannelNamingUtilities integrationPointNamingUtilities;

    //
    // Constructor(s)
    //

    public PetasosClusterMembershipSharedMap(){
        this.integrationPoints = new ConcurrentHashMap<>();
        this.integrationPointLocks = new ConcurrentHashMap<>();

        this.integrationPointSubsystemMap = new ConcurrentHashMap<>();
        this.integrationPointProcessingPlantServiceMapLock = new Object();
    }

    //
    // Abstract Methods
    //

    protected Logger specifyLogger(){
        return(LOG);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(specifyLogger());
    }

    //
    // ServiceMap Methods
    //

    private void addParticipantMembership(String participantName, String endpointName){
        getLogger().debug(".addParticipantMembership(): Entry, participantName->{}, endpointName->{}", participantName, endpointName);
        if(StringUtils.isEmpty(participantName) || StringUtils.isEmpty(endpointName)){
            getLogger().debug(".addParticipantMembership(): Exit, either participantName or endpointName are null, doing nothing");
            return;
        }
        synchronized(this.integrationPointProcessingPlantServiceMapLock) {
            if (this.integrationPointSubsystemMap.containsKey(participantName)) {
                List<String> endpointNames = this.integrationPointSubsystemMap.get(participantName);
                if(endpointNames.contains(endpointName)){
                    // do nothing
                } else {
                    endpointNames.add(endpointName);
                }
            } else {
                List<String> endpointNames = new ArrayList<>();
                endpointNames.add(endpointName);
                this.integrationPointSubsystemMap.put(participantName, endpointNames);
            }
        }
        getLogger().debug(".addParticipantMembership(): Exit, map updated");
    }

    private void removeServiceNameMembership(String participantName, String endpointName) {
        getLogger().debug(".removeServiceNameMembership(): Entry, participantName->{}, endpointName->{}", participantName, endpointName);
        if (StringUtils.isEmpty(participantName) || StringUtils.isEmpty(endpointName)) {
            getLogger().debug(".removeServiceNameMembership(): Exit, either participantName or endpointName are null, doing nothing");
            return;
        }
        synchronized (this.integrationPointProcessingPlantServiceMapLock) {
            if (this.integrationPointSubsystemMap.containsKey(participantName)) {
                List<String> endpointNames = this.integrationPointSubsystemMap.get(participantName);
                if (endpointNames.contains(endpointName)) {
                    endpointNames.remove(endpointName);
                }
                if (endpointNames.isEmpty()) {
                    this.integrationPointSubsystemMap.remove(participantName);
                }
            }
        }
        getLogger().debug(".removeServiceNameMembership(): Exit, map updated");
    }

    private void removeServiceNameMembership(String endpointName){
        getLogger().debug(".removeServiceNameMembership(): Entry, endpointName->{}", endpointName);
        if(StringUtils.isEmpty(endpointName)){
            getLogger().debug(".removeServiceNameMembership(): Exit, endpointName is empty, doing nothing");
            return;
        }
        if(this.integrationPointSubsystemMap.isEmpty()){
            getLogger().debug(".removeServiceNameMembership(): Exit, the map is empty, doing nothing");
            return;
        }
        synchronized (this.integrationPointProcessingPlantServiceMapLock){
            Enumeration<String> serviceNameEnumeration = this.integrationPointSubsystemMap.keys();
            while(serviceNameEnumeration.hasMoreElements()){
                String currentServiceName = serviceNameEnumeration.nextElement();
                List<String> currentEndpointNameSet = this.integrationPointSubsystemMap.get(currentServiceName);
                if(currentEndpointNameSet.contains(endpointName)){
                    currentEndpointNameSet.remove(endpointName);
                    if(currentEndpointNameSet.isEmpty()){
                        this.integrationPointSubsystemMap.remove(currentServiceName);
                    }
                }
            }
        }
        getLogger().debug(".removeServiceNameMembership(): Exit, map updated");
    }

    public List<String> getParticipantFulfillers(String participantName){
        getLogger().debug(".getParticipantFulfillers(): Entry, participantName->{}", participantName);
        List<String> fulfillerList = new ArrayList<>();
        if (StringUtils.isEmpty(participantName)) {
            getLogger().debug(".getParticipantFulfillers(): Exit, participantName is empty, returning empty list");
            return(fulfillerList);
        }
        synchronized (this.integrationPointProcessingPlantServiceMapLock) {
            if (this.integrationPointSubsystemMap.containsKey(participantName)) {
                fulfillerList.addAll(this.integrationPointSubsystemMap.get(participantName));
            }
        }
        getLogger().debug(".getParticipantFulfillers(): Exit, fulfillerList->{}", fulfillerList);
        return(fulfillerList);
    }

    public void updateParticipantIntegrationPointMembership(String participantName, String petasosEndpointName){
        getLogger().debug(".updateParticipantIntegrationPointMembership(): Entry, participantName->{}, petasosEndpointName->{}", participantName, petasosEndpointName);
        if (StringUtils.isEmpty(petasosEndpointName) || StringUtils.isEmpty(participantName)) {
            getLogger().debug(".updateParticipantIntegrationPointMembership(): Exit, either participantName or endpointName are null, doing nothing");
            return;
        }
        boolean alreadyMappedAsRequested = false;
        boolean serviceEntryExists = false;
        synchronized(this.integrationPointProcessingPlantServiceMapLock) {
            if (this.integrationPointSubsystemMap.containsKey(participantName)) {
                serviceEntryExists = true;
                List<String> endpointNames = this.integrationPointSubsystemMap.get(participantName);
                if (endpointNames.contains(petasosEndpointName)) {
                    alreadyMappedAsRequested = true;
                }
            }
        }
        if(alreadyMappedAsRequested) {
            getLogger().debug(".updateParticipantIntegrationPointMembership(): Exit, already in map, no update required");
            return;
        }
        boolean wasMappedElsewhere = false;
        synchronized(this.integrationPointProcessingPlantServiceMapLock) {
            Enumeration<String> serviceNameEnumeration = this.integrationPointSubsystemMap.keys();
            List<String> servicesToBeRemovedFromMap = new ArrayList<>();
            while(serviceNameEnumeration.hasMoreElements()){
                String currentServiceName = serviceNameEnumeration.nextElement();
                List<String> endpointNames = this.integrationPointSubsystemMap.get(currentServiceName);
                if(endpointNames != null) {
                    if (endpointNames.contains(petasosEndpointName)) {
                        wasMappedElsewhere = true;
                        endpointNames.remove(petasosEndpointName);
                    }
                    if (endpointNames.isEmpty()) {
                        servicesToBeRemovedFromMap.add(currentServiceName);
                    }
                }
            }
            if(!servicesToBeRemovedFromMap.isEmpty()) {
                for (String currentServiceToBeRemoved : servicesToBeRemovedFromMap) {
                    this.integrationPointSubsystemMap.remove(currentServiceToBeRemoved);
                }
            }
        }
        addParticipantMembership(participantName, petasosEndpointName);
        getLogger().debug(".updateParticipantIntegrationPointMembership(): Exit, update completed");
    }

    //
    // EndpointMap Methods
    //

    public void printMap(){
        Enumeration<String> endpointNames = integrationPoints.keys();
        getLogger().debug("---------------EndpointMap-------------");
        while(endpointNames.hasMoreElements()){
            String endpointName = endpointNames.nextElement();
            JGroupsChannelConnectorSummary jgroupsIPSummary = integrationPoints.get(endpointName);
            getLogger().debug("Endpoint->{}", jgroupsIPSummary.getComponentId().getDisplayName());
        }
    }

    /**
     * This method adds an PetasosEndpoint to the cache for the given EndpointAddress element if, and only if, there is
     * not an Endpoint already defined within the cache for the given EndpointAddress.getAddressName() value. It also
     * logs the FHIR::Endpoint addition into the audit trail (which is built and contained within the PetasosEndpoint).
     * @param jgroupsIP
     * @return a PetasosEndpoint representing the IPC/OAM interface.
     */
    public JGroupsChannelConnectorSummary addJGroupsIntegrationPoint(JGroupsChannelConnectorSummary jgroupsIP){
        getLogger().debug(".addJGroupsIntegrationPoint(): Entry, jgroupsIP->{}", jgroupsIP);

        if(jgroupsIP == null){
            getLogger().debug(".addJGroupsIntegrationPoint(): Exit, jgroupsIP is null, return(null)");
            return(null);
        }

        String endpointName = jgroupsIP.getComponentId().getDisplayName();
        String participantName = jgroupsIP.getSubsystemParticipantName();
        if(integrationPoints.containsKey(endpointName)){
            JGroupsChannelConnectorSummary retrievedEndpoint = integrationPoints.get(endpointName);
            getLogger().debug(".addJGroupsIntegrationPoint(): Exit, jgroupsIP already registered, jgroupsIP->{}", jgroupsIP);
            return(retrievedEndpoint);
        } else {
            integrationPoints.put(endpointName, jgroupsIP);
            integrationPointLocks.put(endpointName, new Object());
            addParticipantMembership(participantName, endpointName);
            //
            //
            // TODO add an audit event here (detailing the addition of a new Endpoint).
            //
            //
            printMap();
            getLogger().debug(".addJGroupsIntegrationPoint(): Exit, jgroupsIP added, jgroupsIP->{}", jgroupsIP);
            return(jgroupsIP);
        }
    }

    /**
     * This method updates the LastUpdated attribute of the FHIR::Endpoint::Meta element. It is meant to reflect the
     * last instant at which activity occurred (or was detected) on the given endpoint. Node: It does not trigger an update
     * or such.
     * @param endpointName The endpoint name to be "touched".
     */
    public void touchEndpoint(String endpointName){
        getLogger().debug(".touchEndpoint(): Entry, endpointName->{}", endpointName);

        if(StringUtils.isEmpty(endpointName)){
            getLogger().debug(".touchEndpoint(): Exit, endpointName is empty, so doing nothing");
            return;
        }

        if(integrationPoints.containsKey(endpointName)){
            synchronized (getEndpointLock(endpointName)) {
                integrationPoints.get(endpointName).setLastRefreshInstant(Instant.now());
            }
        }
        getLogger().debug(".touchEndpoint(): endpoint updated");
    }

    /**
     * This method retrieves the designated JGroupsIntegrationPointSummary from the cache, as identified by the channelName.
     *
     * @param channelName The name of the FHIR::Endpoint to be retrieved
     * @return THe requested FHIR::Endpoint
     */
    public JGroupsChannelConnectorSummary getJGroupsIntegrationPointSummary(String channelName){
        getLogger().debug(".getEndpoint(): Entry, channelName->{}", channelName);

        if(StringUtils.isEmpty(channelName)){
            getLogger().debug(".touchEndpoint(): Exit, channelName is empty, so returning null");
            return(null);
        }

        if(integrationPoints.containsKey(channelName)){
            JGroupsChannelConnectorSummary endpoint = integrationPoints.get(channelName);
            getLogger().debug(".getEndpoint(): Exit, returning endpoint->{}", endpoint);
            return(endpoint);
        } else {
            getLogger().debug(".getEndpoint(): Exit, endpoint not found, returning null");
            return(null);
        }
    }

    /**
     * This method deletes an PetasosEndpoint (and associated FHIR::Endpoint instance from cache.
     * The FHIR::Endpoint is identified by the instance name (endpointName). An Audit Event is generated for
     * the deletion.
     * @param endpointName The name of the FHIR::Endpoint to be deleted.
     */
    public void deleteSubsystemIntegrationPoint(String endpointName){
        getLogger().debug(".deleteEndpoint(): Entry, endpointName->{}", endpointName);

        if(StringUtils.isEmpty(endpointName)){
            getLogger().debug(".touchEndpoint(): Exit, endpointName is empty, noting to do!");
            return;
        }

        if(integrationPoints.containsKey(endpointName)){
            JGroupsChannelConnectorSummary endpoint = integrationPoints.get(endpointName);
            //
            //
            // TODO Log an AuditEvent when an endpoint is deleted.
            //
            //
            integrationPoints.remove(endpointName);
            integrationPointLocks.remove(endpointName);
            removeServiceNameMembership(endpointName);
            printMap();
            getLogger().debug(".getEndpoint(): Exit, endpoint removed");
            return;
        } else {
            getLogger().debug(".getEndpoint(): Exit, endpoint is not in the cache");
            return;
        }
    }

    public Object getEndpointLock(String endpointName){
        if(StringUtils.isEmpty(endpointName)){
            return(null);
        }
        Object retrievedLockObject = integrationPointLocks.get(endpointName);
        return(retrievedLockObject);
    }


}
