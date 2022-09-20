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

import net.fhirfactory.dricats.petasos.participant.cache.LocalPetasosParticipantSubscriptionMapDM;
import net.fhirfactory.dricats.petasos.tasking.orchestration.helpers.LocalTaskDistributionDecisionEngine;
import net.fhirfactory.pegacorn.core.interfaces.pathway.TaskPathwayManagementServiceInterface;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipant;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PubSubParticipant;
import net.fhirfactory.pegacorn.core.model.petasos.participant.TaskWorkItemSubscriptionRegistration;
import net.fhirfactory.pegacorn.core.model.petasos.subscription.datatypes.DataParcelManifestSubscriptionMaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * This class WILL do more in the future, but it is for now just a proxy to the
 TopicCacheDM.
 */
@ApplicationScoped
public class LocalPetasosParticipantSubscriptionMapIM {

    private static final Logger LOG = LoggerFactory.getLogger(LocalPetasosParticipantSubscriptionMapIM.class);

    @Inject
    LocalPetasosParticipantSubscriptionMapDM subscriptionCache;

    @Inject
    TaskPathwayManagementServiceInterface pathwayManagementService;

    @Inject
    LocalTaskDistributionDecisionEngine taskDistributionDecisionEngine;

    //
    // Constructor(s)
    //

    public LocalPetasosParticipantSubscriptionMapIM(){
    }

    /**
     * This function retrieves the list (FDNTokenSet) of WUPs that are
     * interested in receiving the identified uowPayloadTopicID (FDNToken).
     *
     * @param parcelManifest The FDNToken representing the UoW (Ingres) Payload Topic
 that we want to know which WUPs are interested in
     * @return The set of WUPs wanting to receive this payload type.
     */
    public List<PetasosParticipant> getSubscriberSet(DataParcelManifest parcelManifest) {
        LOG.debug(".getSubscriptionSetForUOWContentTopic(): Entry, parcelManifest --> {}", parcelManifest);
        List<PetasosParticipant> subscribedTopicSet = taskDistributionDecisionEngine.deriveSubscriberList(parcelManifest);
        LOG.debug(".getSubscriptionSetForUOWContentTopic(): Exit");
        return (subscribedTopicSet);
    }

    /**
     * This function establishes a link between a Payload Type and a WUP that is interested in
     * processing/using it.
     * 
     * @param contentTopicID The contentTopicID (FDNToken) of the payload we have received from a WUP
     * @param subscriber The ID of the (Topology) Node that is interested in the payload type.
     */
    public void addTopicSubscriber(DataParcelManifest contentTopicID, PetasosParticipant subscriber) {
        LOG.info(".addTopicSubscriber(): Entry, contentTopicID --> {}, subscriber --> {}", contentTopicID, subscriber);
        if(contentTopicID == null || subscriber == null){
            LOG.debug(".addTopicSubscriber(): Exit, Either contentTopicID or subscriber is null!");
            return;
        }
        DataParcelManifestSubscriptionMaskType newFilter = new DataParcelManifestSubscriptionMaskType(contentTopicID);
        subscriptionCache.addSubscriber(newFilter, subscriber);
        LOG.debug(".addTopicSubscriber(): Exit");
    }

    public void removeSubscriber(DataParcelManifestSubscriptionMaskType contentTopicID, PubSubParticipant interestedNode) {
        LOG.debug(".removeSubscriber(): Entry, contentTopicID --> {}, interestedNode --> {}", contentTopicID, interestedNode);
        subscriptionCache.removeSubscriber(contentTopicID, interestedNode);
        LOG.debug(".removeSubscriber(): Exit");
    }

    public List<TaskWorkItemSubscriptionRegistration> getAllSubscriptions(){
        LOG.debug(".getAllSubscriptions(): Entry");
        List<TaskWorkItemSubscriptionRegistration> subscriptions = subscriptionCache.getAllSubscriptions();
        LOG.debug(".getAllSubscriptions(): Exit");
        return(subscriptions);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
