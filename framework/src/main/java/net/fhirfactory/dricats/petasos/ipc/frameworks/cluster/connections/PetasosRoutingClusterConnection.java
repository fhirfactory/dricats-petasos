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
package net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections;

import net.fhirfactory.dricats.interfaces.pathway.TaskPathwayManagementServiceInterface;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.base.PetasosClusterConnection;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantCacheIM;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;


public abstract class PetasosRoutingClusterConnection extends PetasosClusterConnection
        implements TaskPathwayManagementServiceInterface {

    private boolean subscriptionCheckScheduled;
    private Object subscriptionCheckLock;

    private static Long SUBSCRIPTION_CHECK_INITIAL_DELAY=5000L;
    private static Long SUBSCRIPTION_CHECK_PERIOD = 5000L;

    private int subscriptionCheckCount;
    private static int CHANGE_DETECTION_SUBSCRIPTION_CHECK_COUNT = 10;

    @Inject
    private LocalPetasosParticipantCacheIM participantCacheIM;

    //
    // Constructor(s)
    //

    public PetasosRoutingClusterConnection(){
        super();
        subscriptionCheckScheduled = false;
        subscriptionCheckLock = new Object();
        subscriptionCheckCount = 0;
    }

    //
    // Post Construct
    //

    @Override
    protected void executePostConstructActivities(){
        getLogger().info(".executePostConstructActivities(): Start");
        initialiseCacheSynchronisationDaemon();
        getLogger().info(".executePostConstructActivities(): Finish");
    }

    //
    // Further post construct activities
    //

    abstract protected void initialiseCacheSynchronisationDaemon();

    //
    // Getters (and Setters)
    //

    //
    // Endpoint Definition
    //

    @Override
    protected ClusterFunctionNameEnum getClusterFunction() {
        return (ClusterFunctionNameEnum.PETASOS_ROUTING_SERVICES);
    }


    //
    // Processing Plant check triggered by JGroups Cluster membership change
    //

    @Override
    protected void doIntegrationPointBusinessFunctionCheck(JGroupsChannelConnectorSummary integrationPointSummary, boolean isRemoved, boolean isAdded) {

    }

    //
    // Endpoint/Participant tests
    //

    public String getServiceNameFromParticipantInstanceName(String participantInstanceName){
        getLogger().debug(".getServiceNameFromParticipantInstanceName(): Entry, participantInstanceName->{}", participantInstanceName);
        if(StringUtils.isEmpty(participantInstanceName)){
            getLogger().debug(".getServiceNameFromParticipantInstanceName(): Exit, participantInstanceName is empty!");
            return(null);
        }
        String[] nameParts = StringUtils.split(participantInstanceName, "(");
        String serviceName = nameParts[0];
        getLogger().debug(".getServiceNameFromParticipantInstanceName(): Exit, serviceName->{}", serviceName);
        return(serviceName);
    }

    protected String extractPublisherServiceName(String participantInstanceName){
        return(getServiceNameFromParticipantInstanceName(participantInstanceName));
    }

    //
    // Helpers
    //

    /**
     * This method returns a set of possible endpoints supporting the PUBSUB function for the given publisherServiceName.
     *
     * It first pulls ALL the petasosEndpointNames that are part of the generic publisherServiceName list (i.e. OAM.PubSub,
     * OAM.Discovery & IPC based endpoints) and then filters them down to only include the OAM.PubSub entries.
     *
     * @param publisherServiceName The "Publisher Service Name" to which candidate endpoints are to be found
     * @return The list of .OAM.PubSub endpoints supporting that service.
     */
    List<String> getPublisherServicePubSubCandidateSet(String publisherServiceName){
        List<String> candidateSet = new ArrayList<>();
        if(StringUtils.isEmpty(publisherServiceName)){
            return(candidateSet);
        }
        List<String> serviceNameMembership = getIntegrationPointMap().getParticipantFulfillers(publisherServiceName);
        if(serviceNameMembership.isEmpty()){
            return(candidateSet);
        }
        for(String currentMember: serviceNameMembership){
            if(currentMember.contains(ClusterFunctionNameEnum.PETASOS_ROUTING_SERVICES.getParticipantName())){
                candidateSet.add(currentMember);
            }
        }
        return(candidateSet);
    }


}
