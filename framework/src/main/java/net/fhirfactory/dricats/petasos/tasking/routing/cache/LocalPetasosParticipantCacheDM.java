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

package net.fhirfactory.dricats.petasos.tasking.routing.cache;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.participant.topology.ProcessingPlantPetasosParticipantHolder;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.component.valuesets.ComponentTypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.PetasosParticipantRegistration;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.PetasosParticipantRegistrationStatusEnum;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;

@ApplicationScoped
public class LocalPetasosParticipantCacheDM {
	private static final Logger LOG = LoggerFactory.getLogger(LocalPetasosParticipantCacheDM.class);

	private ConcurrentHashMap<PetasosParticipantId, PetasosParticipant> participantCache;
	private ConcurrentHashMap<PetasosParticipantId, ComponentId> participantMap;
	private Object participantCacheLock;

	@Inject
	private ProcessingPlantConfigurationServiceInterface myProcessingPlant;

	@Inject
	private ProcessingPlantPetasosParticipantHolder meAsPetasosParticipant;

	//
	// Constructor(s)
 	//

    public LocalPetasosParticipantCacheDM(){
        this.participantCache = new ConcurrentHashMap<>();
		this.participantMap = new ConcurrentHashMap<>();
        this.participantCacheLock = new Object();
    }

	//
	// Getters (and Setters)
	//

	protected Logger getLogger(){
		return(LOG);
	}

	protected ConcurrentHashMap<PetasosParticipantId, PetasosParticipant> getParticipantCache() {
		return participantCache;
	}

	protected ConcurrentHashMap<PetasosParticipantId, ComponentId> getParticipantMap(){
		return(participantMap);
	}

	protected Object getParticipantCacheLock() {
		return participantCacheLock;
	}

	//
	// Business Methods
	//

	/**
	 * This class adds a PetasosParticipant to the cache, mapping to the ComponentId provided.
	 * @param componentId
	 * @param participant
	 */
	public void addPetasosParticipant(PetasosParticipant participant, ComponentId componentId){
		getLogger().debug(".addPetasosParticipant(): Entry, componentId->{}, participant->{}", componentId, participant);
		if(participant == null ){
			getLogger().warn(".addPetasosParticipant(): Exit, participant is null");
		}
		if(participant.getParticipantId() == null){
			getLogger().warn(".addPetasosParticipant(): Exit, participant.getParticipantId is null");
		}
		if(componentId == null){
			getLogger().warn(".addPetasosParticipant(): Exit, componentId is null");
		}
		synchronized (getParticipantCacheLock()){
			if(getParticipantCache().containsKey(participant.getParticipantId())) {
				getParticipantMap().remove(participant.getParticipantId());
			}
			getParticipantCache().put(participant.getParticipantId(), participant);
			if(getParticipantMap().containsKey(participant.getParticipantId())) {
				getParticipantMap().remove(participant.getParticipantId());
			}
			getParticipantMap().put(participant.getParticipantId(), componentId);
		}
		getLogger().debug(".addPetasosParticipant(): Exit");
	}

	public void removePetasosParticipant(PetasosParticipant participant){
		getLogger().debug(".removePetasosParticipant(): Entry, participant->{}", participant);
		if(participant == null){
			getLogger().debug(".removePetasosParticipant(): Exit, participant is null");
		}
		synchronized (getParticipantCacheLock()){
			if(getParticipantCache().containsKey(participant.getParticipantId())){
				getParticipantCache().remove(participant.getParticipantId());
			}
			if(getParticipantMap().containsKey(participant.getParticipantId())){
				getParticipantMap().remove(participant.getParticipantId());
			}
		}
		getLogger().debug(".removePetasosParticipant(): Exit");
	}

	public void updatePetasosParticipant(PetasosParticipant participant, ComponentId componentId){
		getLogger().debug(".updatePetasosParticipant(): Entry, participant->{}", participant);
		if(participant == null){
			getLogger().debug(".updatePetasosParticipant(): Exit, participant is null");
			return;
		}
		PetasosParticipantRegistration registration = null;
		synchronized (getParticipantCacheLock()){
			boolean inCache = false;
			if(getParticipantCache().containsKey(participant.getParticipantId())){
				getParticipantCache().remove(participant.getParticipantId());
			}
			if(getParticipantMap().containsKey(participant.getParticipantId())) {
				getParticipantMap().remove(participant.getParticipantId());
			}
			getParticipantCache().put(participant.getParticipantId(), participant);
			getParticipantMap().put(participant.getParticipantId(), componentId);
		}
		getLogger().debug(".updatePetasosParticipant(): Exit");
	}

	public PetasosParticipant getPetasosParticipant(ComponentId componentId){
		getLogger().debug(".getPetasosParticipant(): Entry, participantId->{}", componentId);
		if(componentId == null){
			getLogger().debug(".getPetasosParticipant(): Exit, participantId is null");
			return(null);
		}
		PetasosParticipant participant = null;
		synchronized (getParticipantCacheLock()){
			if(getParticipantCache().containsValue(componentId)){
				for(PetasosParticipantId currentParticipantId: getParticipantMap().keySet()){
					ComponentId cacheEntry = getParticipantMap().get(currentParticipantId);
					if(cacheEntry.equals(componentId)){
						participant = getParticipantCache().get(currentParticipantId);
						break;
					}
				}
			}
		}
		getLogger().debug(".getPetasosParticipant(): Exit, participant->{}", participant);
		return(participant);
	}

	public Set<PetasosParticipant> getDownstreamParticipantSet(){
    	Set<PetasosParticipant> downstreamParticipants = new HashSet<>();
    	synchronized(getParticipantCacheLock()) {
			for (PetasosParticipant currentParticipant: getParticipantCache().values()) {
				if(!currentParticipant.getSubscriptions().isEmpty()){
					if(!currentParticipant.getParticipantId().getSubsystemName().equals(meAsPetasosParticipant.getMyProcessingPlantPetasosParticipant().getParticipantId().getSubsystemName())){
						for(DataParcelManifestSubscriptionMask currentParticipantSubscription: currentParticipant.getSubscriptions()){
							if(currentParticipantSubscription.getOriginMask().getBoundaryPointProcessingPlantParticipantNameMask().equals(meAsPetasosParticipant.getMyProcessingPlantPetasosParticipant().getParticipantId().getSubsystemName())){
								if(!downstreamParticipants.contains(currentParticipant)){
									downstreamParticipants.add(currentParticipant);
								}
							}
						}
					}
				}
			}
		}
		return(downstreamParticipants);
	}

	public Set<PetasosParticipant> getAllPetasosParticipants(){
		Set<PetasosParticipant> participants = new HashSet<>();
		synchronized (getParticipantCacheLock()){
			for(PetasosParticipant currentParticipant: getParticipantCache().values()){
				if(participants.contains(currentParticipant)){
					// do nothing
				} else {
					participants.add(currentParticipant);
				}
			}
		}
		return(participants);
	}


}
