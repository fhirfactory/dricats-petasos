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

package net.fhirfactory.dricats.petasos.participant.manager;

import net.fhirfactory.dricats.interfaces.pathway.TaskPathwayManagementServiceInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.PetasosParticipantRegistration;
import net.fhirfactory.dricats.model.petasos.participant.topology.ProcessingPlantPetasosParticipantHolder;
import net.fhirfactory.dricats.model.petasos.participant.topology.ProcessingPlantPetasosParticipantNameHolder;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.TaskWorkItemManifestType;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.petasos.tasking.routing.cache.LocalPetasosParticipantCacheDM;
import net.fhirfactory.dricats.petasos.participant.cache.LocalPetasosParticipantSubscriptionMapDM;
import net.fhirfactory.pegacorn.core.interfaces.pathway.TaskPathwayManagementServiceInterface;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.component.SoftwareComponent;
import net.fhirfactory.pegacorn.core.model.component.valuesets.SoftwareComponentExecutionControlEnum;
import net.fhirfactory.pegacorn.core.model.component.valuesets.SoftwareComponentStatusEnum;
import net.fhirfactory.pegacorn.core.model.componentid.ComponentId;
import net.fhirfactory.pegacorn.core.model.componentid.PegacornSystemComponentTypeTypeEnum;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipant;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipantRegistration;
import net.fhirfactory.pegacorn.core.model.petasos.participant.ProcessingPlantPetasosParticipantHolder;
import net.fhirfactory.pegacorn.core.model.petasos.participant.ProcessingPlantPetasosParticipantNameHolder;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.work.datatypes.TaskWorkItemManifestType;
import net.fhirfactory.pegacorn.core.model.petasos.subscription.datatypes.DataParcelManifestSubscriptionMaskType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class LocalPetasosParticipantCacheIM {
	private static final Logger LOG = LoggerFactory.getLogger(LocalPetasosParticipantCacheIM.class);

	@Inject
	private LocalPetasosParticipantCacheDM participantCacheDM;

	@Inject
	private LocalPetasosParticipantSubscriptionMapDM participantSubscriptionMapDM;

	@Inject
	private TaskPathwayManagementServiceInterface taskPathwayManagementService;

	@Inject
	private ProcessingPlantConfigurationServiceInterface myProcessingPlant;

	@Inject
	private ProcessingPlantPetasosParticipantHolder participantHolder;

	@Inject
	private ProcessingPlantPetasosParticipantNameHolder participantNameHolder;


	//
	// Constructor(s)
 	//

    public LocalPetasosParticipantCacheIM(){
    }

	//
	// Business Methods
	//

	public PetasosParticipantRegistration getLocalParticipantRegistration(ComponentId participantId){
    	getLogger().debug(".getLocalParticipantRegistration(): Entry, participantId->{}", participantId);
		PetasosParticipantRegistration registration = participantCacheDM.getPetasosParticipantRegistration(participantId);
		getLogger().debug(".getLocalParticipantRegistration(): Exit, registration->{}", registration);
		return(registration);
	}

	@Deprecated
	public PetasosParticipantRegistration registerPetasosParticipant(String preferredParticipantName, PetasosParticipant participant, Set<TaskWorkItemManifestType> publishedTopics, Set<TaskWorkItemManifestType> subscribedTopics) {
		getLogger().debug(".registerPetasosParticipant(): Entry, preferredParticipantName->{}, participant->{}", preferredParticipantName, participant);
		//
		// 1st, some basic defensive programming and parameter checking
		if (participant == null || publishedTopics == null || subscribedTopics == null) {
			getLogger().debug(".registerPetasosParticipant(): Exit, publisherId, publishedTopics or subscribedTopics is null, not registering anything");
			return (null);
		}
		if (StringUtils.isNotEmpty(preferredParticipantName)) {

			participant.setParticipantName(preferredParticipantName);
		}
		PetasosParticipantRegistration petasosParticipantRegistration = registerPetasosParticipant(participant, publishedTopics, subscribedTopics);
		getLogger().debug(".registerPetasosParticipant(): Exit, registeredParticipant->{}", petasosParticipantRegistration);
		return(petasosParticipantRegistration);
	}


	public PetasosParticipantRegistration registerPetasosParticipant(PetasosParticipant participantSoftwareComponent, Set<DataParcelManifest> publishedTopics, Set<DataParcelManifestSubscriptionMask> subscribedTopics) {
		getLogger().debug(".registerPetasosParticipant(): Entry, participantSoftwareComponent->{}", participantSoftwareComponent);
		if (participantSoftwareComponent == null || publishedTopics == null) {
			getLogger().debug(".registerPetasosParticipant(): Exit, publisherId or publishedTopics is null, not registering anything");
			return (null);
		}
		PetasosParticipant participant = new PetasosParticipant(participantSoftwareComponent);
		if (!publishedTopics.isEmpty()) {
			getLogger().trace(".registerPetasosParticipant(): Has published topics");
			for (TaskWorkItemManifestType currentParcelManifest : publishedTopics) {
				participant.getPublishedWorkItemManifests().add(currentParcelManifest);
			}
		}
		if(!subscribedTopics.isEmpty()) {
			getLogger().trace(".registerPetasosParticipant(): Has topics to subscribe to!");
			for (TaskWorkItemManifestType currentParcelManifest : subscribedTopics) {
				DataParcelManifestSubscriptionMask currentFilter = new DataParcelManifestSubscriptionMask(currentParcelManifest);
				participant.getSubscriptions().add(currentFilter);
			}
		}
		PetasosParticipantRegistration registeredParticipant = registerPetasosParticipant(participant);
		getLogger().debug(".registerPetasosParticipant(): Exit, registeredParticipant->{}", registeredParticipant);
		return(registeredParticipant);
	}


	public PetasosParticipantRegistration registerPetasosParticipant(PetasosParticipant participant){
		getLogger().debug(".registerPetasosParticipant(): Entry, participant->{}", participant);
		if(participant == null ){
			getLogger().debug(".registerPetasosParticipant(): Exit, publisherId or publishedTopics is null, not registering anything");
			return(null);
		}
		//
		// Register with local DM
		getLogger().trace(".registerPetasosParticipant(): [Register Participant into the DM] Start");
		PetasosParticipantRegistration registration = getParticipantCacheDM().addPetasosParticipant(participant);
		getLogger().trace(".registerPetasosParticipant(): [Register Participant into the DM] Finish");
		/*
		//
		// Register with global Pathway Service Manager
		PetasosParticipantRegistration registration = getTaskPathwayManagementService().registerPetasosParticipant(participant);
		//
		// Update local DM with Gobal details
		getParticipantCacheDM().updatePetasosParticipant(registration.getParticipant());
		 */
		//
		// Now update local subscription map
		getLogger().trace(".registerPetasosParticipant(): [Update local Subscription Map] Start");
		PetasosParticipant registeredParticipant = registration.getParticipant();
		if(!registeredParticipant.getSubscriptions().isEmpty()){
			getLogger().trace(".registerPetasosParticipant(): [Update local Subscription Map] Has subscription requirements");
			getLogger().trace(".registerPetasosParticipant(): [Update local Subscription Map] Subscribing to Topics");
			for (DataParcelManifestSubscriptionMask currentSubscribedManifest : registeredParticipant.getSubscriptions()) {
				getLogger().trace(".registerPetasosParticipant(): [Update local Subscription Map] adding topic->{}", currentSubscribedManifest);
				boolean doSubscribe = true;
				if(currentSubscribedManifest.hasOriginMask()) {
					boolean sourceTaskProducerIsMe = currentSubscribedManifest.getOriginMask().getBoundaryPointProcessingPlantParticipantNameMask().equals(participantNameHolder.getSubsystemParticipantName());
					boolean sourceSystemIsWildcard = false;
					if(currentSubscribedManifest.hasExternalSourceSystem()){
						sourceSystemIsWildcard = currentSubscribedManifest.getExternalSourceSystem().equals(DataParcelManifestSubscriptionMask.WILDCARD_CHARACTER);
					}
					if(sourceTaskProducerIsMe || sourceSystemIsWildcard){
						doSubscribe = true;
					} else {
						doSubscribe = false;
					}
				} else {
					doSubscribe = true;
				}
				if(doSubscribe) {
					getParticipantSubscriptionMapDM().addSubscriber(currentSubscribedManifest, participant);
				}
			}
		}
		getLogger().trace(".registerPetasosParticipant(): [Update local Subscription Map] Finish");
		//
		// Our work is done
		getLogger().debug(".registerPetasosParticipant(): Exit, registration->{}", registration);
		return(registration);
	}

	/**
	 *
	 * @return
	 */
	public Set<PetasosParticipant> getDownstreamParticipants(){
		Set<PetasosParticipant> downstreamParticipantSet = getParticipantCacheDM().getDownstreamParticipantSet();
		return(downstreamParticipantSet);
	}

	/**
	 *
	 * @param participantComponent
	 * @param workItemSet
	 */
	public void updateProducedWorkItems(SoftwareComponent participantComponent, Set<TaskWorkItemManifestType> workItemSet){
		getLogger().debug(".updateProducedWorkItems(): Entry, participantComponent->{}", participantComponent);
		if(participantComponent == null || workItemSet == null){
			getLogger().debug(".updateProducedWorkItems(): Exit, participantComponent or publishedTopics is null, not registering anything");
			return;
		}
		if(workItemSet.isEmpty()){
			getLogger().debug(".updateProducedWorkItems(): Exit, publishedTopics is empty, not registering anything");
			return;
		}
		PetasosParticipant cachedPetasosParticipant = getParticipantCacheDM().getPetasosParticipant(participantComponent.getComponentID());
		for(TaskWorkItemManifestType additionalWorkItem: workItemSet) {
			boolean alreadyInSet = false;
			for (TaskWorkItemManifestType currentWorkItem : cachedPetasosParticipant.getPublishedWorkItemManifests()){
				if(currentWorkItem.equals(additionalWorkItem)){
					alreadyInSet = true;
					break;
				}
			}
			if(!alreadyInSet){
				cachedPetasosParticipant.getPublishedWorkItemManifests().add(additionalWorkItem);
			}
		}
		PetasosParticipantRegistration registration = getTaskPathwayManagementService().updatePetasosParticipant(cachedPetasosParticipant);
		getParticipantCacheDM().updatePetasosParticipant(registration.getParticipant());
		if(registration.getParticipant().getComponentID().equals(myProcessingPlant.getMeAsASoftwareComponent().getComponentID())){
			participantHolder.setMyProcessingPlantPetasosParticipant(registration.getParticipant());
		}
		getLogger().debug(".updateProducedWorkItems(): Exit");
	}

	/**
	 *
	 * @param participant
	 */
	public PetasosParticipantRegistration updatePetasosParticipant(PetasosParticipant participant){
		getLogger().debug(".updatePetasosParticipant(): Entry, participant->{}", participant);
		if(participant == null ){
			getLogger().debug(".updatePetasosParticipant(): Exit, participant is null, not updating anything");
			return(null);
		}
		//
		// Update with global Pathway Service Manager
		PetasosParticipantRegistration registration = getTaskPathwayManagementService().updatePetasosParticipant(participant);
		//
		// Update local DM with Global details
		synchroniseLocalWithCentralCacheDetail(registration);
		if(registration.getParticipant().getComponentID().equals(myProcessingPlant.getMeAsASoftwareComponent().getComponentID())){
			participantHolder.setMyProcessingPlantPetasosParticipant(registration.getParticipant());
		}
		getLogger().debug(".updatePetasosParticipant(): Exit, registration->{}", registration);
		return(registration);
	}

	/**
	 *
	 * @param participantId
	 */
	public void synchroniseLocalWithCentralCache(ComponentId participantId){
		getLogger().debug(".synchroniseLocalWithCentralCache(): Entry, participantId->{}", participantId);
		if(participantId == null){
			getLogger().debug(".synchroniseLocalWithCentralCache(): Exit, participantId is null, not doing anything");
			return;
		}
		//
		// Get Registration from Global Pathway Service Manager
		PetasosParticipantRegistration registration = getTaskPathwayManagementService().getPetasosParticipantRegistration(participantId);
		if(registration != null) {
			synchroniseLocalWithCentralCacheDetail(registration.getParticipant());
		}
		if(registration.getParticipant().getComponentID().equals(myProcessingPlant.getMeAsASoftwareComponent().getComponentID())){
			participantHolder.setMyProcessingPlantPetasosParticipant(registration.getParticipant());
		}
		getLogger().debug(".synchroniseLocalWithCentralCache(): Exit");
	}

	public void synchroniseLocalWithCentralCacheDetail(PetasosParticipant participant){
		getLogger().debug(".synchroniseLocalWithCentralCacheDetail(): Entry, participant->{}", participant);
		if(participant != null) {
			//
			// Update local DM with Gobal details
			getLogger().trace(".synchroniseLocalWithCentralCacheDetail(): [Synchronise Local Cache] Start");
			getParticipantCacheDM().updatePetasosParticipant(participant);
			getLogger().trace(".synchroniseLocalWithCentralCacheDetail(): [Synchronise Local Cache] Finish");
			//
			// Now update local subscription map
			getLogger().trace(".synchroniseLocalWithCentralCacheDetail(): [Update Local Subscription Cache] Start");
			if(participant.getComponentType().equals(DricatsSoftwareComponentTypeEnum.PROCESSING_PLANT)) {
				getLogger().trace(".synchroniseLocalWithCentralCacheDetail(): [Update Local Subscription Cache] is processing plant...");
				if (!participant.getSubscriptions().isEmpty()) {
					getLogger().trace(".synchroniseLocalWithCentralCacheDetail(): [Update Local Subscription Cache] subscribed manifest set is NOT empty");
					for (DataParcelManifestSubscriptionMask currentSubscribedManifest : participant.getSubscriptions()) {
						getLogger().trace(".synchroniseLocalWithCentralCacheDetail(): [Update Local Subscription Cache] currentSubscribedManifest->{}", currentSubscribedManifest);
						getParticipantSubscriptionMapDM().addSubscriber(currentSubscribedManifest, participant);
					}
				}
			}
			getLogger().trace(".synchroniseLocalWithCentralCacheDetail(): [Update Local Subscription Cache] finish");
		}
		getLogger().debug(".synchroniseLocalWithCentralCacheDetail(): Exit");
	}

	public void synchroniseLocalWithCentralCacheDetail(PetasosParticipantRegistration registration){
		getLogger().debug(".synchroniseLocalWithCentralCacheDetail(): Entry, registration->{}", registration);
		if(registration != null) {
			//
			// Update local DM with Gobal details
			getParticipantCacheDM().updatePetasosParticipantRegistration(registration);
			//
			// Now update local subscription map
			PetasosParticipant participant = registration.getParticipant();
			synchroniseLocalWithCentralCacheDetail(participant);
		}
		getLogger().debug(".synchroniseLocalWithCentralCacheDetail(): Exit");
	}

	/**
	 *
	 * @param participantId
	 * @return
	 */
	public PetasosParticipant getPetasosParticipant(ComponentId participantId){
		getLogger().debug(".getPetasosParticipant(): Entry, participantId->{}", participantId);
		if(participantId == null){
			getLogger().debug(".getPetasosParticipant(): Exit, participantId is null, not doing anything");
			return(null);
		}
		PetasosParticipant petasosParticipant = getParticipantCacheDM().getPetasosParticipant(participantId);
		getLogger().debug(".getPetasosParticipant(): Exit, petasosParticipant->{}", petasosParticipant);
		return(petasosParticipant);
	}

	/**
	 *
	 * @param publisherId
	 * @return
	 */
	public Set<DataParcelManifest> getPublishedParcels(ComponentId publisherId) {
		getLogger().debug(".getPublishedParcels(): Entry, publisherId->{}", publisherId);
		if (publisherId == null) {
			getLogger().debug(".getPublishedParcels(): Exit, publisherId is null, returning empty set");
			return (new HashSet<>());
		}
		PetasosParticipant petasosParticipant = getPetasosParticipant(publisherId);
		Set<DataParcelManifest> publishedWorkItems = new HashSet<>();
		if (petasosParticipant != null) {
			for (TaskWorkItemManifestType currentItemManifest : petasosParticipant.getPublishedWorkItemManifests()) {
				publishedWorkItems.add(currentItemManifest);
			}
		}
		getLogger().debug(".getPublishedParcels(): Exit");
		return(publishedWorkItems);
	}

	/**
	 *
	 * @param publisherId
	 * @return
	 */
	public Set<TaskWorkItemManifestType> getProducedWorkItemManifests(ComponentId publisherId){
		getLogger().debug(".getPublishedParcels(): Entry, publisherId->{}", publisherId);
		if(publisherId == null){
			getLogger().debug(".getPublishedParcels(): Exit, publisherId is null, returning empty set");
			return(new HashSet<>());
		}
		PetasosParticipant petasosParticipant = getPetasosParticipant(publisherId);
		Set<TaskWorkItemManifestType> publishedWorkItems = new HashSet<>();
		if(petasosParticipant != null) {
			publishedWorkItems.addAll(petasosParticipant.getPublishedWorkItemManifests());
		}
		getLogger().debug(".getPublishedParcels(): Exit");
		return(publishedWorkItems);
	}

	public Set<PetasosParticipant> getParticipantSetForSubsystem(String serviceName) {
		getLogger().debug(".getParticipantSetForSerivce(): Entry, serviceName->{}", serviceName);
		if(StringUtils.isEmpty(serviceName)) {
			getLogger().debug(".getParticipantSetForSerivce(): Exit, serviceName is null, returning an empty set");
			return (new HashSet<>());
		}
		Set<PetasosParticipant> participantSet = getParticipantCacheDM().getParticipantSetForSerivce(serviceName);
		if(getLogger().isDebugEnabled()) {
			getLogger().debug(".getParticipantSetForSerivce(): Exit, participantSet size->{}", participantSet.size());
		}
		return(participantSet);
	}


	public void removePetasosParticipantFromLocalCache(PetasosParticipant removedPetasosParticipant){
		if(removedPetasosParticipant != null){
			boolean noCandidateTargetEndpointForService = false;
			Set<PetasosParticipant> myDownstreamPerformerSets = getDownstreamParticipants();
			if(!myDownstreamPerformerSets.isEmpty()){
				for(PetasosParticipant currentParticipant: myDownstreamPerformerSets){
					if(currentParticipant.getComponentID().equals(removedPetasosParticipant.getComponentID())){
						Set<PetasosParticipant> serviceParticipantSet = getParticipantSetForSubsystem(currentParticipant.getSubsystemParticipantName());
						if(serviceParticipantSet.isEmpty()) {
							noCandidateTargetEndpointForService = true;
						} else {
							noCandidateTargetEndpointForService = true;
							for(PetasosParticipant currentServiceParticipant: serviceParticipantSet){
								if(!currentParticipant.getComponentID().equals(removedPetasosParticipant.getComponentID())){
									noCandidateTargetEndpointForService = false;
									break;
								}
							}
						}
					}
					break;
				}
			}
			if(noCandidateTargetEndpointForService) {
				// TODO DO SOMETHING
				getLogger().error(".removePetasosParticipantFromLocalCache(): There is no longer a subscriber instance for->{}", removedPetasosParticipant.getSubsystemParticipantName());
				removedPetasosParticipant.setComponentExecutionControl(SoftwareComponentExecutionControlEnum.SOFTWARE_COMPONENT_PAUSE_EXECUTION);
				removedPetasosParticipant.setComponentStatus(SoftwareComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
			} else {
				getParticipantCacheDM().removePetasosParticipant(removedPetasosParticipant);
			}
		}
	}

	//
	// Getters and Setters
	//

	protected Logger getLogger(){
		return(LOG);
	}

	protected LocalPetasosParticipantCacheDM getParticipantCacheDM() {
		return participantCacheDM;
	}

	protected TaskPathwayManagementServiceInterface getTaskPathwayManagementService(){
		return(this.taskPathwayManagementService);
	}

	protected LocalPetasosParticipantSubscriptionMapDM getParticipantSubscriptionMapDM(){
		return(this.participantSubscriptionMapDM);
	}
}
