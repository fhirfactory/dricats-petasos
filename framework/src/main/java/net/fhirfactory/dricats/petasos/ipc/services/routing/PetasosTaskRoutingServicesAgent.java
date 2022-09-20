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

import net.fhirfactory.dricats.interfaces.petasos.ipc.services.PetasosTaskRepositoryServiceProviderNameInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.PetasosParticipantRegistration;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.PetasosParticipantRegistrationStatusEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantCacheIM;
import org.apache.commons.lang3.StringUtils;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class PetasosTaskRoutingServicesAgent extends PetasosParticipantSubscriptionServicesEndpointBase{
    private static final Logger LOG = LoggerFactory.getLogger(PetasosTaskRoutingServicesAgent.class);

    private static Long PETASOS_PARTICIPANT_MAP_CACHE_SYNCHRONISATION_INITIAL_WAIT = 60000L;
    private static Long PETASOS_PARTICIPANT_MAP_CACHE_SYNCHRONISATION_PERIOD = 30000L;

    private static Long EXECUTION_STATUS_SYNCHRONISATION_INITIAL_WAIT = 60000L;
    private static Long EXECUTION_STATUS_SYNCHRONISATION_PERIOD = 30000L;

    private boolean synchronisationDaemonInitialised;

    @Inject
    private PetasosTaskRepositoryServiceProviderNameInterface taskServiceProviderName;

    @Inject
    private LocalPetasosParticipantCacheIM localPetasosParticipantCacheIM;

    //
    // Constructor(s)
    //

    public PetasosTaskRoutingServicesAgent(){
        super();
        this.synchronisationDaemonInitialised = false;
    }

    //
    // Post Construct
    //

    @Override
    protected void initialiseCacheSynchronisationDaemon() {
        getLogger().debug(".initialiseCacheSynchronisationDaemon(): Entry");
        scheduleSynchronisationDaemons();
        getLogger().debug(".initialiseCacheSynchronisationDaemon(): Exit");
    }



    //
    // Synchronisation Daemon
    //

    /**
     *
     */
    public void scheduleSynchronisationDaemons() {
        getLogger().debug(".scheduleSynchronisationDaemons(): Entry");
        if (synchronisationDaemonInitialised) {
            // do nothing, it is already scheduled
        } else {
            TimerTask petasosParticipantCacheSynchronisationDaemonScheduler = new TimerTask() {
                public void run() {
                    getLogger().debug(".petasosParticipantCacheSynchronisationDaemonScheduler(): Entry");
                    petasosParticipantCacheSynchronisationDaemon();
                    getLogger().debug(".petasosParticipantCacheSynchronisationDaemonScheduler(): Exit");
                }
            };
            Timer petasosParticipantCacheSynchronisationTimer = new Timer("PetasosParticipantCacheSynchronisationDaemonSchedule");
            petasosParticipantCacheSynchronisationTimer.schedule(petasosParticipantCacheSynchronisationDaemonScheduler, PETASOS_PARTICIPANT_MAP_CACHE_SYNCHRONISATION_INITIAL_WAIT, PETASOS_PARTICIPANT_MAP_CACHE_SYNCHRONISATION_PERIOD);

            TimerTask participantExecutionStatusSynchronisationDaemonScheduler = new TimerTask() {
                public void run() {
                    getLogger().debug(".participantExecutionStatusSynchronisationDaemonScheduler(): Entry");
                    participantExecutionStatusSynchronisationDaemon();
                    getLogger().debug(".participantExecutionStatusSynchronisationDaemonScheduler(): Exit");
                }
            };
            Timer participantExecutionStatusSynchronisationDaemonTimer = new Timer("ParticipantExecutionStatusSynchronisationDaemonSchedule");
            participantExecutionStatusSynchronisationDaemonTimer.schedule(participantExecutionStatusSynchronisationDaemonScheduler, EXECUTION_STATUS_SYNCHRONISATION_INITIAL_WAIT, EXECUTION_STATUS_SYNCHRONISATION_PERIOD);
            synchronisationDaemonInitialised = true;
        }
        getLogger().debug(".scheduleSynchronisationDaemons(): Exit");
    }

    public void petasosParticipantCacheSynchronisationDaemon(){
        getLogger().debug(".petasosParticipantCacheSynchronisationDaemon(): Entry");
        //
        // 1st, let's synchronise my subscriptions
        getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Subscriptions] Start");
 //       PetasosParticipant myProcessingPlantPetasosParticipant = getProcessingPlant().getMyProcessingPlantPetasosParticipant();
//        getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Subscriptions] myProcessingPlantPetasosParticipant->{}", myProcessingPlantPetasosParticipant);
        PetasosParticipantRegistration localRegistration = localPetasosParticipantCacheIM.getLocalParticipantRegistration(getComponentId());
        getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Subscriptions] localRegistration->{}", localRegistration);
        if(localRegistration == null){
            getLogger().error(".petasosParticipantCacheSynchronisationDaemon(): My PetasosParticipantRegistration is all wrong!");
        } else {
            getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): localRegistration->{}", localRegistration);
            if (localRegistration.getRegistrationStatus().equals(PetasosParticipantRegistrationStatusEnum.PETASOS_PARTICIPANT_REGISTRATION_LOCAL_ONLY)) {
                PetasosParticipantRegistration centralRegistration = getPetasosParticipantRegistration(localRegistration.getComponentId());
                if (centralRegistration == null) {
                    centralRegistration = registerPetasosParticipant(localRegistration.getParticipant());
                } else {
                    centralRegistration = updatePetasosParticipant(localRegistration.getParticipant());
                }
                localPetasosParticipantCacheIM.synchroniseLocalWithCentralCacheDetail(centralRegistration);
            }
        }
        getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Subscriptions] Finish");
        //
        // 2nd, let's synchronise my publishing
        getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Publishing] Start");
        String myProcessingPlantParticipantName = getProcessingPlant().getCoreConfigurationFile().getSubsystemInstant().getSubsystemParticipantName();
        Set<PetasosParticipant> downstreamTaskPerformers = getDownstreamTaskPerformersForTaskProducer(myProcessingPlantParticipantName);
        if(!downstreamTaskPerformers.isEmpty()){
            getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Publishing] There are downstream subscribers!");
            for(PetasosParticipant currentDownstreamPerformer: downstreamTaskPerformers){
                if(getLogger().isTraceEnabled()){
                    getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Publishing] downstream subsystem participantName->{}", currentDownstreamPerformer.getParticipantId().getSubsystemName());
                }
                localPetasosParticipantCacheIM.synchroniseLocalWithCentralCacheDetail(currentDownstreamPerformer);
            }
        }
        getLogger().trace(".petasosParticipantCacheSynchronisationDaemon(): [Synchronise My Participant/Publishing] Finish");
        getLogger().debug(".petasosParticipantCacheSynchornisationDaemon(): Exit");
    }

    public void participantExecutionStatusSynchronisationDaemon(){
        getLogger().debug(".participantExecutionStatusSynchronisationDaemon(): Entry");

        getLogger().debug(".participantExecutionStatusSynchronisationDaemon(): Exit");
    }

    //
    // Endpoint Changes
    //

    //
    // Business Methods
    //

    @Override
    public boolean isPetasosParticipantRegistered(PetasosParticipant publisher) {
        return false;
    }


    @Override
    public PetasosParticipantRegistration registerPetasosParticipant(PetasosParticipant participant) {
        getLogger().debug(".registerPetasosParticipant(): Entry, participant->{}", participant);
        if(participant == null ){
            getLogger().trace(".registerPetasosParticipant: participant is null");
            PetasosParticipantRegistration registration = new PetasosParticipantRegistration();
            registration.setRegistrationStatus(PetasosParticipantRegistrationStatusEnum.PETASOS_PARTICIPANT_REGISTRATION_FAILED);
            registration.setRegistrationCommentary("participant (PetasosParticipant) is null");
            registration.setRegistrationInstant(Instant.now());
            getLogger().debug(".registerPetasosParticipant(): Exit");
            return(registration);
        }
        if(participant.getParticipantId().getSubsystemName() == null){
            participant.getParticipantId().setSubsystemName(getProcessingPlant().getCoreConfigurationFile().getSubsystemInstant().getSubsystemParticipantName());
        }
        Address taskServicesAddress = getTargetMemberAddress(taskServiceProviderName.getPetasosTaskRepositoryServiceProviderName());
        getLogger().trace(".registerPetasosParticipant(): Extract JGroups Address->{}", taskServicesAddress);
        if(taskServicesAddress == null){
            getLogger().trace(".registerPetasosParticipant(): Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            PetasosParticipantRegistration registration = new PetasosParticipantRegistration();
            registration.setRegistrationStatus(PetasosParticipantRegistrationStatusEnum.PETASOS_PARTICIPANT_REGISTRATION_FAILED);
            registration.setRegistrationCommentary(" Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            registration.setRegistrationInstant(Instant.now());
            getLogger().debug(".registerPetasosParticipant(): Exit, (Wrong endpoint technology (should be JGroups))");
            return(registration);
        }
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = participant;
            classSet[0] = PetasosParticipant.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PetasosParticipantRegistration registration = getRPCDispatcher().callRemoteMethod(taskServicesAddress, "registerPetasosParticipant", objectSet, classSet, requestOptions);
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".registerPetasosParticipant(): Exit, registration->{}", registration);
            return(registration);
        } catch (NoSuchMethodException e) {
            getLogger().debug(".registerPetasosParticipant(): Error (NoSuchMethodException)->", e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            PetasosParticipantRegistration registration = new PetasosParticipantRegistration();
            registration.setRegistrationStatus(PetasosParticipantRegistrationStatusEnum.PETASOS_PARTICIPANT_REGISTRATION_FAILED);
            registration.setRegistrationInstant(Instant.now());
            registration.setRegistrationCommentary("Error (NoSuchMethodException)" + e.getMessage());
            return(registration);
        } catch (Exception e) {
            getLogger().debug(".registerPetasosParticipant(): Error (GeneralException) ->",e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            PetasosParticipantRegistration registration = new PetasosParticipantRegistration();
            registration.setRegistrationStatus(PetasosParticipantRegistrationStatusEnum.PETASOS_PARTICIPANT_REGISTRATION_FAILED);
            registration.setRegistrationInstant(Instant.now());
            registration.setRegistrationCommentary("Error (NoSuchMethodException)" + e.getMessage());
            return(registration);
        }
    }

    @Override
    public Set<PetasosParticipant> getDownstreamTaskPerformersForTaskProducer(String producerServiceName) {
        getLogger().debug(".getDownstreamTaskPerformersForTaskProducer(): Entry, producerServiceName->{}", producerServiceName);
        if(StringUtils.isEmpty(producerServiceName)){
            return(new HashSet<>());
        }
        Address taskServicesAddress = getTargetMemberAddress(taskServiceProviderName.getPetasosTaskRepositoryServiceProviderName());
        getLogger().trace(".getDownstreamTaskPerformersForTaskProducer(): Extract JGroups Address->{}", taskServicesAddress);
        if(taskServicesAddress == null) {
            getLogger().warn(".getDownstreamTaskPerformersForTaskProducer: Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            return (new HashSet<>());
        }
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = producerServiceName;
            classSet[0] = String.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            Set<PetasosParticipant> participantSet = getRPCDispatcher().callRemoteMethod(taskServicesAddress, "getDownstreamTaskPerformersForTaskProducer", objectSet, classSet, requestOptions);
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".getDownstreamTaskPerformersForTaskProducer(): Exit, participantSet->{}", participantSet);
            return(participantSet);
        } catch (NoSuchMethodException e) {
            getLogger().debug(".getDownstreamTaskPerformersForTaskProducer(): Error (NoSuchMethodException)->", e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(new HashSet<>());
        } catch (Exception e) {
            getLogger().debug(".getDownstreamTaskPerformersForTaskProducer(): Error (GeneralException) ->",e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(new HashSet<>());
        }
    }

    @Override
    public PetasosParticipantRegistration updatePetasosParticipant(PetasosParticipant participant) {
        getLogger().debug(".updatePetasosParticipant(): Entry, participant->{}", participant);
        if(participant == null){
            return(null);
        }
        if(participant.getParticipantId().getSubsystemName() == null){
            participant.getParticipantId().setSubsystemName(getProcessingPlant().getCoreConfigurationFile().getSubsystemInstant().getSubsystemParticipantName());
        }
        Address taskServicesAddress = getTargetMemberAddress(taskServiceProviderName.getPetasosTaskRepositoryServiceProviderName());
        getLogger().trace(".updatePetasosParticipant(): Extract JGroups Address->{}", taskServicesAddress);
        if(taskServicesAddress == null) {
            getLogger().warn(".updatePetasosParticipant: Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            return (null);
        }
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = participant;
            classSet[0] = PetasosParticipant.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PetasosParticipantRegistration registration = getRPCDispatcher().callRemoteMethod(taskServicesAddress, "updatePetasosParticipant", objectSet, classSet, requestOptions);
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".getTaskPerformersForTaskProducer(): Exit, registration->{}", registration);
            return(registration);
        } catch (NoSuchMethodException e) {
            getLogger().debug(".getTaskPerformersForTaskProducer(): Error (NoSuchMethodException)->", e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(null);
        } catch (Exception e) {
            getLogger().debug(".getTaskPerformersForTaskProducer(): Error (GeneralException) ->",e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(null);
        }
    }

    @Override
    public PetasosParticipantRegistration deregisterPetasosParticipant(PetasosParticipantId participantId) {
        getLogger().debug(".deregisterPetasosParticipant(): Entry, participantId->{}", participantId);
        if(participantId == null){
            return(null);
        }
        Address taskServicesAddress = getTargetMemberAddress(taskServiceProviderName.getPetasosTaskRepositoryServiceProviderName());
        getLogger().trace(".deregisterPetasosParticipant(): Extract JGroups Address->{}", taskServicesAddress);
        if(taskServicesAddress == null) {
            getLogger().warn(".deregisterPetasosParticipant: Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            return (null);
        }
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = participantId;
            classSet[0] = PetasosParticipantId.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PetasosParticipantRegistration registration = getRPCDispatcher().callRemoteMethod(taskServicesAddress, "deregisterPetasosParticipant", objectSet, classSet, requestOptions);
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".deregisterPetasosParticipant(): Exit, registration->{}", registration);
            return(registration);
        } catch (NoSuchMethodException e) {
            getLogger().debug(".deregisterPetasosParticipant(): Error (NoSuchMethodException)->", e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(null);
        } catch (Exception e) {
            getLogger().debug(".deregisterPetasosParticipant(): Error (GeneralException) ->",e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(null);
        }
    }

    @Override
    public PetasosParticipantRegistration getPetasosParticipantRegistration(ComponentId participantId) {
        getLogger().debug(".getPetasosParticipantRegistration(): Entry, participantId->{}", participantId);
        if(participantId == null){
            return(null);
        }
        Address taskServicesAddress = getTargetMemberAddress(taskServiceProviderName.getPetasosTaskRepositoryServiceProviderName());
        getLogger().trace(".getPetasosParticipantRegistration(): Extract JGroups Address->{}", taskServicesAddress);
        if(taskServicesAddress == null) {
            getLogger().warn(".getPetasosParticipantRegistration(): Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            return (null);
        }
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = participantId;
            classSet[0] = ComponentId.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PetasosParticipantRegistration registration = getRPCDispatcher().callRemoteMethod(taskServicesAddress, "getPetasosParticipantRegistration", objectSet, classSet, requestOptions);
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".getPetasosParticipantRegistration(): Exit, registration->{}", registration);
            return(registration);
        } catch (NoSuchMethodException e) {
            getLogger().debug(".getPetasosParticipantRegistration(): Error (NoSuchMethodException)->", e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(null);
        } catch (Exception e) {
            getLogger().debug(".getPetasosParticipantRegistration(): Error (GeneralException) ->",e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(null);
        }
    }

    @Override
    public Set<PetasosParticipantRegistration> getParticipantRegistrationSetForParticipantName(String participantSeviceName) {
        getLogger().debug(".getParticipantRegistrationSetForService(): Entry, performerServiceName->{}", participantSeviceName);
        if(StringUtils.isEmpty(participantSeviceName)){
            return(new HashSet<>());
        }
        Address taskServicesAddress = getTargetMemberAddress(taskServiceProviderName.getPetasosTaskRepositoryServiceProviderName());
        getLogger().trace(".getParticipantRegistrationSetForService(): Extract JGroups Address->{}", taskServicesAddress);
        if(taskServicesAddress == null) {
            getLogger().warn(".getParticipantRegistrationSetForService: Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            return (new HashSet<>());
        }
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = participantSeviceName;
            classSet[0] = String.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            Set<PetasosParticipantRegistration> participantSet = getRPCDispatcher().callRemoteMethod(taskServicesAddress, "getParticipantRegistrationSetForService", objectSet, classSet, requestOptions);
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".getParticipantRegistrationSetForService(): Exit, participantSet->{}", participantSet);
            return(participantSet);
        } catch (NoSuchMethodException e) {
            getLogger().debug(".getParticipantRegistrationSetForService(): Error (NoSuchMethodException)->", e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(new HashSet<>());
        } catch (Exception e) {
            getLogger().debug(".getParticipantRegistrationSetForService(): Error (GeneralException) ->",e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(new HashSet<>());
        }
    }

    @Override
    public Set<PetasosParticipantRegistration> getAllRegistrations() {
        getLogger().debug(".getAllRegistrations(): Entry");
        Address taskServicesAddress = getTargetMemberAddress(taskServiceProviderName.getPetasosTaskRepositoryServiceProviderName());
        getLogger().trace(".getAllRegistrations(): Extract JGroups Address->{}", taskServicesAddress);
        if(taskServicesAddress == null) {
            getLogger().warn(".getAllRegistrations: Cannot resolve Task Services Provider endpoint (JGroups Channel Name)");
            return (new HashSet<>());
        }
        try {
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            Set<PetasosParticipantRegistration> participantSet = getRPCDispatcher().callRemoteMethod(taskServicesAddress, "getAllRegistrations", null, null, requestOptions);
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".getTaskPerformerServiceRegistration(): Exit, participantSet->{}", participantSet);
            return(participantSet);
        } catch (NoSuchMethodException e) {
            getLogger().debug(".getTaskPerformerServiceRegistration(): Error (NoSuchMethodException)->", e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(new HashSet<>());
        } catch (Exception e) {
            getLogger().debug(".getTaskPerformerServiceRegistration(): Error (GeneralException) ->",e);
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            return(new HashSet<>());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public PetasosParticipant getMyPetasosParticipant() {
        return getProcessingPlantInformationAccessor().getProcessingPlant().getPetasosParticipantInformation();
    }

    @Override
    public ClusterFunctionNameEnum getChannelFunction() {
        return null;
    }
}
