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
package net.fhirfactory.dricats.petasos.ipc.services.routing;


import net.fhirfactory.dricats.interfaces.pathway.TaskPathwayManagementServiceInterface;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.PetasosRoutingClusterConnection;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantCacheIM;
import org.apache.commons.lang3.StringUtils;
import org.jgroups.Address;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;


public abstract class PetasosParticipantSubscriptionServicesEndpointBase extends PetasosRoutingClusterConnection
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

    public PetasosParticipantSubscriptionServicesEndpointBase(){
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
    // Add Integration Point to ProcessingPlant's IntegrationPointSet
    //

    //
    // Endpoint Definition
    //


    //
    // Processing Plant check triggered by JGroups Cluster membership change
    //


    //
    // Endpoint/Participant tests
    //
    protected boolean hasDifferentParticipantSubsystemName(PetasosParticipantId participant){
        getLogger().debug(".hasDifferentParticipantSubsystemName(): Entry, participant->{}", participant);
        if(participant != null){
            if(StringUtils.isNotEmpty(participant.getSubsystemName())){
                if(participant.getSubsystemName().contentEquals(getProcessingPlantInformationAccessor().getProcessingPlant().getPetasosParticipantInformation().getParticipantId().getSubsystemName())){
                    getLogger().debug(".hasDifferentParticipantSubsystemName(): Exit, returning -true-");
                    return(true);
                }
            }
        }
        getLogger().debug(".hasDifferentParticipantSubsystemName(): Exit, returning -false-");
        return(false);
    }

    public String getAvailableParticipantInstanceName(String participantServiceName){
        getLogger().debug(".getAvailableParticipantInstanceName(): Entry, participantServiceName->{}", participantServiceName);
        Address targetAddress = getTargetMemberAddress(participantServiceName);
        String participantInstanceName = targetAddress.toString();
        getLogger().debug(".getAvailableParticipantInstanceName(): Exit, participantInstanceName->{}", participantInstanceName);
        return(participantInstanceName);
    }

    public boolean isPetasosEndpointChannelAvailable(String petasosEndpointChannelName){
        getLogger().debug(".isParticipantInstanceAvailable(): Entry, participantInstanceName->{}", petasosEndpointChannelName);
        boolean participantInstanceNameStillActive = getTargetMemberAddress(petasosEndpointChannelName) != null;
        getLogger().debug(".isParticipantInstanceAvailable(): Exit, participantInstanceNameStillActive->{}", participantInstanceNameStillActive);
        return(participantInstanceNameStillActive);
    }

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
}

