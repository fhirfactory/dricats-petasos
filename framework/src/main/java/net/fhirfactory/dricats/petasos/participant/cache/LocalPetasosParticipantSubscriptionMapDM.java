/*
 * Copyright (c) 2020 MAHun
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

package net.fhirfactory.dricats.petasos.participant.cache;

import net.fhirfactory.dricats.interfaces.petasos.participant.processingplant.ProcessingPlantInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.participant.topology.ProcessingPlantPetasosParticipantNameHolder;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.TaskWorkItemSubscriptionRegistration;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class LocalPetasosParticipantSubscriptionMapDM {
	private static final Logger LOG = LoggerFactory.getLogger(LocalPetasosParticipantSubscriptionMapDM.class);

	private ConcurrentHashMap<DataParcelManifestSubscriptionMask, List<TaskWorkItemSubscriptionRegistration>> dataParcelSubscriptionMap;
	private Object dataParcelSubscriptionMapLock;

	@Inject
	private ProcessingPlantInterface processingPlant;

	@Inject
	private ProcessingPlantPetasosParticipantNameHolder participantNameHolder;

	//
	// Constructor(s)
	//

    public LocalPetasosParticipantSubscriptionMapDM(){
        this.dataParcelSubscriptionMap = new ConcurrentHashMap<DataParcelManifestSubscriptionMask, List<TaskWorkItemSubscriptionRegistration>>();
        this.dataParcelSubscriptionMapLock = new Object();
    }

	//
	// Getters (and Setters)
	//

	protected Map<DataParcelManifestSubscriptionMask, List<TaskWorkItemSubscriptionRegistration>> getDataParcelSubscriptionMap(){
		return(this.dataParcelSubscriptionMap);
	}

	protected Object getDataParcelSubscriptionMapLock(){
		return(this.dataParcelSubscriptionMapLock);
	}

	protected Logger getLogger(){
		return(LOG);
	}

	//
	// Business Methods
	//

    /**
     * This function retrieves the list (TaskWorkItemSubscriptionRegistration) of WUPs that are interested in
     * receiving the identified parcelDescriptor (DataParcelTypeDescriptor).
     *
     * @param parcelDescriptor The FDNToken representing the UoW (Ingres) Payload Topic that we want to know which WUPs are interested in
     * @return The set of WUPs wanting to receive this payload type.
     */

    public List<TaskWorkItemSubscriptionRegistration> getSubscriberList(DataParcelManifestSubscriptionMask parcelDescriptor){
		getLogger().debug(".getSubscriberList(): Entry, parcelDescriptor->{}", parcelDescriptor);
		List<TaskWorkItemSubscriptionRegistration> subscriptionList = new ArrayList<>();
		synchronized (getDataParcelSubscriptionMapLock()){
			subscriptionList.addAll(getDataParcelSubscriptionMap().get(parcelDescriptor));
		}
		if(subscriptionList == null ) {
			getLogger().debug(".getSubscriberList(): Couldn't find any associated PubSubSubscriber elements (i.e. couldn't find any interested WUPs), returning an empty set");
			return (new ArrayList<>());
		}
		if(getLogger().isDebugEnabled()) {
			getLogger().debug(".getSubscriberList(): Exit, returning associated FDNSet of the WUPs interested:");
			int count = 0;
			for(TaskWorkItemSubscriptionRegistration currentSubscription : subscriptionList){
				ComponentId currentSubscriber = currentSubscription.getParticipant().getParticipantId();
				getLogger().debug(".getSubscriberList(): Subscriber[{}]->{}", count, currentSubscriber);
				count++;
			}
		}
		getLogger().debug(".getSubscriberList(): Exit, subscriptionList->{}", subscriptionList);
		return (subscriptionList);

	}

    /**
     * This function establishes a link between a Payload Type and a WUP that is interested in
     * processing/using it.
     *
     * @param workItemSubscription The contentTopicID (FDNToken) of the payload we have received from a WUP
     * @param subscriber The NodeElement of the WUP that is interested in the payload type.
     */
    public void addSubscriber(DataParcelManifestSubscriptionMaskType workItemSubscription, PetasosParticipant subscriber) {
    	getLogger().debug(".addSubscriber(): Entry, workItemSubscription->{}, subscriber->{}", workItemSubscription, subscriber);
    	if((workItemSubscription==null) || (subscriber==null)) {
    		throw(new IllegalArgumentException(".addSubscriber(): workItemSubscription or subscriberInstanceID is null"));
    	}
		DataParcelTypeDescriptorSubscriptionMaskType contentDescriptor = workItemSubscription.getContentDescriptorMask();
		DataParcelTypeDescriptorSubscriptionMaskType containerDescriptor = workItemSubscription.getContainerDescriptorMask();
		PetasosParticipant clonedSubscriber = SerializationUtils.clone(subscriber);
    	DataParcelTypeDescriptorSubscriptionMaskType descriptorToRegister = null;
    	if(contentDescriptor != null) {
			getLogger().trace(".addSubscriber(): contentDescriptor is not null");
			descriptorToRegister = contentDescriptor;
		}
		if((descriptorToRegister == null) && (containerDescriptor != null)){
			getLogger().trace(".addSubscriber(): contentDescriptor was null and containerDescriptor is not null");
			descriptorToRegister = containerDescriptor;
		}
    	if(descriptorToRegister == null){
			throw(new IllegalArgumentException(".addSubscriber(): workItemSubscription does not contain suitable contentDescriptor or containerDescriptor"));
		}
		boolean newSubscriberAdded = false;
    	synchronized (this.dataParcelSubscriptionMapLock) {
			List<TaskWorkItemSubscriptionRegistration> subscriptionList = getDataParcelSubscriptionMap().get(descriptorToRegister);
			if (subscriptionList != null) {
				getLogger().trace(".addSubscriber(): Topic Subscription Map: Adding subscriber to existing map for workItemSubscription --> {}", workItemSubscription);
				TaskWorkItemSubscriptionRegistration existingSubscription = null;
				for(TaskWorkItemSubscriptionRegistration currentSubscription: subscriptionList){
					if(currentSubscription.getParticipant().getComponentID().equals(clonedSubscriber.getComponentID())){
						if(isSameRemoteEndpointSubscriber(currentSubscription.getParticipant(), clonedSubscriber)){
							existingSubscription = currentSubscription;
							break;
						}
					}
				}
				if(existingSubscription == null) {
					TaskWorkItemSubscriptionRegistration newSubscription = new TaskWorkItemSubscriptionRegistration(workItemSubscription, clonedSubscriber);
					getDataParcelSubscriptionMap().get(descriptorToRegister).add(newSubscription);
					newSubscriberAdded = true;
				}
//				if(clonedSubscriber.hasSubsystemParticipantName()) {
//					if(!clonedSubscriber.getSubsystemParticipantName().equals(participantNameHolder.getSubsystemParticipantName())) {
//						clonedSubscriber.setParticipantStatus(subscriber.getParticipantStatus());
//					}
//				}
			} else {
				getLogger().trace(".addSubscriber(): Topic Subscription Map: Create a new Distribution List and Add Subscriber");
				TaskWorkItemSubscriptionRegistration newSubscription = new TaskWorkItemSubscriptionRegistration(workItemSubscription, clonedSubscriber);
				getLogger().trace(".addSubscriber(): Topic Subscription Map: Created new PubSubSubscription, adding to a newly created List");
				subscriptionList = new ArrayList<TaskWorkItemSubscriptionRegistration>();
				subscriptionList.add(newSubscription);
				getLogger().trace(".addSubscriber(): Topic Subscription Map: PubSubSubscription List created, adding it to the distribution map");
				this.dataParcelSubscriptionMap.put(descriptorToRegister, subscriptionList);
				getLogger().trace(".addSubscriber(): Topic Subscription Map: Added PubSubSubscription List to the distribution map");
//				if(subscriber.hasSubsystemParticipantName()) {
//					if(!subscriber.getSubsystemParticipantName().equals(participantNameHolder.getSubsystemParticipantName())) {
//						subscriber.setParticipantStatus(subscriber.getParticipantStatus());
//					}
//				}
				newSubscriberAdded = true;
			}
		}
		if (getLogger().isDebugEnabled()) {
			if(newSubscriberAdded) {
				List<TaskWorkItemSubscriptionRegistration> subscriptionList = new ArrayList<>();
				synchronized (getDataParcelSubscriptionMapLock()){
					subscriptionList.addAll(getDataParcelSubscriptionMap().get(descriptorToRegister));
				}
				int count = 0;
				getLogger().debug(".addSubscriber(): New Subscriber Added for Topic->{}", workItemSubscription);
				for (TaskWorkItemSubscriptionRegistration currentSubscription : subscriptionList) {
					PetasosParticipant currentSubscriber = currentSubscription.getParticipant();
					getLogger().debug(".addSubscriber(): Subscriber[{}]->{}", count, currentSubscriber.getComponentID() + "\n");
					count++;
				}
			}
		}
		printAllSubscriptionSets();
    }

    private boolean isSameRemoteEndpointSubscriber(PetasosParticipant currentRegisteredParticipant, PetasosParticipant testParticipant){
		boolean currentRegisteredParticipantHasServiceName = currentRegisteredParticipant.hasSubsystemParticipantName();
		boolean testParticipantHasServiceName = testParticipant.hasSubsystemParticipantName();
		if(!currentRegisteredParticipantHasServiceName && testParticipantHasServiceName){
			return(false);
		}
		if(currentRegisteredParticipantHasServiceName && !testParticipantHasServiceName){
			return(false);
		}
		if(!currentRegisteredParticipantHasServiceName && !testParticipantHasServiceName){
			return(true);
		}
		boolean sameServiceName = currentRegisteredParticipant.getSubsystemParticipantName().equals(testParticipant.getSubsystemParticipantName());
		return(sameServiceName);
	}

    public void addSubscriber(DataParcelManifestSubscriptionMaskType contentDescriptor, SoftwareComponent localSubscriberWUP){
		getLogger().debug(".addSubscriber(): Entry, contentDescriptor->{}, localSubscriberWUP->{}", contentDescriptor, localSubscriberWUP);
		if((contentDescriptor==null) || (localSubscriberWUP==null)) {
			throw(new IllegalArgumentException(".addSubscriber(): payloadTopic or localSubscriberWUP is null"));
		}
		DataParcelManifestSubscriptionMaskType descriptor = new DataParcelManifestSubscriptionMaskType(contentDescriptor);
		PetasosParticipant participant = new PetasosParticipant(localSubscriberWUP);
		addSubscriber(descriptor, participant);
	}

    /**
     * Remove a Subscriber from the Topic Subscription list
     *
     * @param parcelManifest The DataParcelManifest of the Topic we want to unsubscribe from.
     * @param subscriberInstanceID  The subscriber we are removing from the subscription list.
     */
    public void removeSubscriber(DataParcelManifestSubscriptionMaskType parcelManifest, PubSubParticipant subscriberInstanceID) {
    	getLogger().debug(".removeSubscriber(): Entry, parcelManifest --> {}, subscriberInstanceID --> {}", parcelManifest, subscriberInstanceID);
    	if((parcelManifest==null) || (subscriberInstanceID==null)) {
    		throw(new IllegalArgumentException(".removeSubscriber(): topic or subscriberInstanceID is null"));
    	}
		boolean found = false;
		DataParcelTypeDescriptorSubscriptionMaskType currentToken = null;
		DataParcelTypeDescriptorSubscriptionMaskType contentDescriptor = parcelManifest.getContentDescriptorMask();
		DataParcelTypeDescriptorSubscriptionMaskType containerDescriptor = parcelManifest.getContainerDescriptorMask();
		DataParcelTypeDescriptorSubscriptionMaskType descriptorToTest = null;
		if(contentDescriptor != null) {
			descriptorToTest = contentDescriptor;
		}
		if((descriptorToTest == null) && (containerDescriptor != null)){
			descriptorToTest = containerDescriptor;
		}
		if(descriptorToTest == null){
			throw(new IllegalArgumentException(".addSubscriber(): parcelManifest does not contain suitable contentDescriptor or containerDescriptor"));
		}
		synchronized (getDataParcelSubscriptionMapLock()) {
			Enumeration<DataParcelTypeDescriptorSubscriptionMaskType> topicEnumerator = dataParcelSubscriptionMap.keys();
			while (topicEnumerator.hasMoreElements()) {
				currentToken = topicEnumerator.nextElement();
				if (currentToken.equals(descriptorToTest)) {
					getLogger().trace(".removeSubscriber(): Found Topic in Subscription Cache");
					found = true;
					break;
				}
			}
		}
		if(found) {
    		getLogger().trace(".removeSubscriber(): Removing Subscriber from contentDescriptor --> {}", contentDescriptor);
    		synchronized (this.dataParcelSubscriptionMapLock) {
				List<TaskWorkItemSubscriptionRegistration> subscriptionList = this.dataParcelSubscriptionMap.get(currentToken);
				for(TaskWorkItemSubscriptionRegistration currentSubscription: subscriptionList){
					boolean sameSubscriber = currentSubscription.getParticipant().equals(subscriberInstanceID);
					boolean sameParcelManifest = currentSubscription.getWorkItemSubscription().equals(parcelManifest);
					if (sameParcelManifest && sameSubscriber) {
						getLogger().trace(".removeSubscriber(): Found Subscriber in Subscription List, removing");
						subscriptionList.remove(currentSubscription);
						getLogger().debug(".removeSubscriber(): Exit, removed the subscriberInstanceID from the topic");
						getLogger().trace("Topic Subscription Map: (Remove Subscriber) Topic [{}] <-- Subscriber [{}]", currentToken, subscriberInstanceID);
						break;
					}
				}
			}
    	} else {
    		getLogger().debug(".removeSubscriber(): Exit, Could not find Subscriber in Subscriber Cache for Topic");
    		return;
    	}
		getLogger().debug(".removeSubscriber(): Exit, Could not find Topic in Subscriber Cache");
    }

    public void printAllSubscriptionSets(){
    	if(!(getLogger().isDebugEnabled() || getLogger().isTraceEnabled())){
    		return;
		}
    	Enumeration<DataParcelTypeDescriptorSubscriptionMaskType> topicEnumerator = dataParcelSubscriptionMap.keys();
    	getLogger().debug(".printAllSubscriptionSets(): Printing ALL Subscription Lists");
    	while(topicEnumerator.hasMoreElements()){
			DataParcelTypeDescriptorSubscriptionMaskType currentToken = topicEnumerator.nextElement();
    		getLogger().debug(".printAllSubscriptionSets(): Topic (TopicToken) --> {}", currentToken);
			List<TaskWorkItemSubscriptionRegistration> subscriptionList = getSubscriberList(currentToken);
			if(subscriptionList != null){
				for(TaskWorkItemSubscriptionRegistration currentSubscription: subscriptionList){
					PetasosParticipant currentSubscriber = currentSubscription.getParticipant();
					getLogger().debug(".printAllSubscriptionSets(): Subscriber --> {}", currentSubscriber.getComponentID().getDisplayName());
				}
			}

		}
	}

	public List<TaskWorkItemSubscriptionRegistration> getPossibleSubscriptionMatches(DataParcelTypeDescriptor testDescriptor){
    	List<DataParcelTypeDescriptorSubscriptionMaskType> possibleList = new ArrayList<>();
    	List<TaskWorkItemSubscriptionRegistration> possibleSubscriptionList = new ArrayList<>();
    	if(this.dataParcelSubscriptionMap.isEmpty()){
    		return(possibleSubscriptionList);
		}
		synchronized (getDataParcelSubscriptionMapLock()) {
			for (DataParcelTypeDescriptorSubscriptionMaskType currentSubscribedDescriptor : getDataParcelSubscriptionMap().keySet()) {
				if (currentSubscribedDescriptor.applyMask(testDescriptor)) {
					possibleList.add(currentSubscribedDescriptor);
				}
			}
		}
    	if(!possibleList.isEmpty()){
			synchronized (getDataParcelSubscriptionMapLock()) {
				for (DataParcelTypeDescriptorSubscriptionMaskType possibleDescriptor : possibleList) {
					List<TaskWorkItemSubscriptionRegistration> currentSubscriptionSet = getDataParcelSubscriptionMap().get(possibleDescriptor);
					if (currentSubscriptionSet != null) {
						possibleSubscriptionList.addAll(currentSubscriptionSet);
					}
				}
			}
		}
    	return(possibleSubscriptionList);
	}



	public List<TaskWorkItemSubscriptionRegistration> getAllSubscriptions(){
		getLogger().debug(".getAllSubscriptions(): Entry");

		List<TaskWorkItemSubscriptionRegistration> subscriptionList = new ArrayList<>();
		synchronized (getDataParcelSubscriptionMapLock()){
			for(DataParcelTypeDescriptorSubscriptionMaskType currentDescriptor: getDataParcelSubscriptionMap().keySet()) {
				subscriptionList.addAll(getDataParcelSubscriptionMap().get(currentDescriptor));
			}
		}
		return(subscriptionList);
	}
}
