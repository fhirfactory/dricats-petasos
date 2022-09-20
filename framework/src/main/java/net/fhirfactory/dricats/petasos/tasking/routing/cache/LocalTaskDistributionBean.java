/*
 * Copyright (c) 2020 Mark A. Hunter
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.fhirfactory.dricats.petasos.tasking.orchestration.helpers.LocalTaskDistributionDecisionEngine;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.configuration.defaults.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.edge.PetasosEdgeMessageForwarderService;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.constants.DeploymentPropertyConstants;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosFulfillmentTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.performer.datatypes.TaskPerformerTypeType;
import net.fhirfactory.dricats.model.petasos.tasking.fulfillment.execctrl.PetasosTaskJobCard;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.model.topology.nodes.datatypes.TopologyNodeFunctionFDNToken;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstanceAccessorFactory;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.PetasosActionableTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.PetasosTaskJobCardSharedInstanceAccessorFactory;
import net.fhirfactory.dricats.petasos.tasking.factories.PetasosFulfillmentTaskFactory;
import net.fhirfactory.dricats.petasos.tasking.factories.PetasosTaskJobCardFactory;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.LocalPetasosFulfilmentTaskActivityController;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import net.fhirfactory.dricats.petasos.tasking.oversight.LocalPetasosActionableTaskActivityController;
import net.fhirfactory.pegacorn.core.model.componentid.ComponentIdType;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.dricats.petasos.participant.solution.LocalSolution;

/**
 * @author Mark A. Hunter
 */

@ApplicationScoped
public class LocalTaskDistributionBean {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTaskDistributionBean.class);

    private DateTimeFormatter timeFormatter;

    @Inject
    LocalSolution localSolutionMap;

    @Produce
    private ProducerTemplate camelProducerService;

    @Inject
    private PetasosFulfillmentTaskFactory fulfillmentTaskFactory;

    @Inject
    private LocalPetasosFulfilmentTaskActivityController fulfilmentTaskBroker;

    @Inject
    private PetasosEdgeMessageForwarderService forwarderService;

    @Inject
    private ProcessingPlantConfigurationServiceInterface myProcessingPlant;

    @Inject
    private LocalTaskDistributionDecisionEngine taskDistributionDecisionEngine;

    @Inject
    private PetasosFulfillmentTaskSharedInstanceAccessorFactory fulfillmentTaskSharedInstanceFactory;

    @Inject
    private PetasosTaskJobCardSharedInstanceAccessorFactory taskJobCardInstanceFactory;

    @Inject
    private PetasosTaskJobCardFactory taskJobCardFactory;

    @Inject
    private LocalPetasosActionableTaskActivityController actionableTaskActivityController;

    @Inject
    private LocalPetasosParticipantCacheDM localPetasosParticipantCacheDM;

    //
    // Constructor(s)
    //

    public LocalTaskDistributionBean(){
        timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS").withZone(ZoneId.of(DeploymentPropertyConstants.DEFAULT_TIMEZONE));
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected DateTimeFormatter getTimeFormatter(){
        return(this.timeFormatter);
    }

    protected LocalTaskDistributionDecisionEngine getTaskDistributionDecisionEngine(){
        return(taskDistributionDecisionEngine);
    }

    protected PetasosFulfillmentTaskSharedInstanceAccessorFactory getFulfillmentTaskSharedInstanceFactory(){
        return(this.fulfillmentTaskSharedInstanceFactory);
    }

    //
    // Business Logic
    //

    /**
     * Essentially, we get the set of WUPs subscribing to a particular UoW type,
     * create a property within the CamelExchange and then we use that Property
     * as a mechanism of keeping track of who we have already forwarded the UoW
     * to. Once we've cycled through all the targets (subscribers), we return
     * null.
     *
     * @param actionableTask Incoming WorkUnitTransportPacket that will be distributed to all
     * Subscribed WUPs
     * @param camelExchange The Apache Camel Exchange instance associated with
     * this route.
     * @return An endpoint (name) for a recipient for the incoming UoW
     */

    public void distributeNewActionableTasks(PetasosActionableTaskSharedInstance actionableTask, Exchange camelExchange) {
        getLogger().debug(".distributeNewFulfillmentTasks(): Entry, actionableTask (WorkUnitTransportPacket)->{}", actionableTask);

        //
        // Defensive Programming
        if(actionableTask == null){
            getLogger().debug(".distributeNewFulfillmentTasks(): Exit, Ingres Actionable Task is null, returning an empty list for routing.");
            return;
        }
        if(!actionableTask.hasTaskWorkItem()){
            getLogger().debug(".distributeNewFulfillmentTasks(): Exit, Ingres Actionable Task has no work item, returning an empty list for routing.");
            return;
        }
        if(!actionableTask.getTaskWorkItem().hasIngresContent()){
            getLogger().debug(".distributeNewFulfillmentTasks(): Exit, Ingres Actionable Task has a work item with no ingres content, returning an empty list for routing.");
            return;
        }
        if(!actionableTask.getTaskWorkItem().getIngresContent().hasDataParcelQualityStatement()){
            getLogger().debug(".distributeNewFulfillmentTasks(): Exit, Ingres Actionable Task has a work item with no ingres content manifest, returning an empty list for routing.");
            return;
        }

        //
        // First, we need to get the target component (PerformerType) from the ActionableTask. Notionally, the
        // PerformerType is a list in the ActionableTask, but we only want the first one... :)
        if(actionableTask.getTaskPerformerTypes().isEmpty()){
            getLogger().warn(".distributeNewActionableTasks(): No Target To Deliver Task To!!!");
            return;
        }
        TaskPerformerTypeType targetComponent = actionableTask.getTaskPerformerTypes().get(0);
        ComponentId targetComponentId = targetComponent.getKnownFulfillerInstance();
        PetasosParticipant targetParticipant = localPetasosParticipantCacheDM.getPetasosParticipant(targetComponentId);
        if(targetParticipant == null){
            getLogger().warn(".distributeNewActionableTasks(): No Target To Deliver Task To!!!");
            return;
        }

        //
        // Now we can forward the task!
        if(getLogger().isDebugEnabled()){
            getLogger().debug(".distributeNewFulfillmentTasks(): Sending Task To->{}", targetComponent.getKnownFulfillerInstance());
        }
        forwardTask(targetParticipant, actionableTask, camelExchange);

        getLogger().debug(".distributeNewFulfillmentTasks(): Exiting");
        return;
    }



    private void forwardTask(PetasosParticipant subscriber, PetasosActionableTaskSharedInstance actionableTask, Exchange camelExchange){
        getLogger().debug(".forwardTask(): Subscriber --> {}", subscriber);
        // 1st check if the Subscriber is actually a remote one! If so, ensure it has a proper "IntendedTarget" entry
        boolean isRemoteTarget = false;
        String intendedTargetName = null;
        DataParcelManifest payloadTopicID = actionableTask.getTaskWorkItem().getPayloadTopicID();

        if (getTaskDistributionDecisionEngine().hasRemoteServiceName(subscriber)) {
            getLogger().trace(".forwardTask(): Has Remote Service as Target");
            boolean hasEmptyIntendedTarget = StringUtils.isEmpty(payloadTopicID.getIntendedTargetSystem());
            boolean hasWildcardTarget = false;
            if (getTaskDistributionDecisionEngine().hasIntendedTarget(payloadTopicID)) {
                hasWildcardTarget = payloadTopicID.getIntendedTargetSystem().contentEquals(DataParcelManifestSubscriptionMask.WILDCARD_CHARACTER);
            }
            boolean hasRemoteElement = getTaskDistributionDecisionEngine().hasRemoteServiceName(subscriber);
            getLogger().trace(".forwardTask(): hasEmptyIntendedTarget->{}, hasWildcardTarget->{}, hasRemoteElement->{} ", hasEmptyIntendedTarget, hasWildcardTarget, hasRemoteElement);
            if ((hasEmptyIntendedTarget || hasWildcardTarget) && hasRemoteElement) {
                intendedTargetName = subscriber.getSubsystemParticipantName();
                getLogger().trace(".forwardTask(): Setting the intendedTargetSystem->{}", subscriber.getSubsystemParticipantName());
                isRemoteTarget = true;
            }
        }

        ComponentIdType actualSubscriberId = null;
        getLogger().trace(".forwardTask(): Assigning target component id, based on whether it is a remote component or local");
        if(isRemoteTarget){
            getLogger().trace(".forwardTask(): It is a remote target, so routing via the local forwarder service");
            actualSubscriberId = forwarderService.getComponentId();
        } else {
            getLogger().trace(".forwardTask(): It is a local target, so routing directly to target");
            actualSubscriberId = subscriber.getComponentID();
        }
        getLogger().trace(".forwardTask(): The (LocalSubscriber aspect) IdentifieFHIRCommunicationToUoWr->{}", actualSubscriberId);
        WorkUnitProcessorSoftwareComponent currentNodeElement = (WorkUnitProcessorSoftwareComponent)localSolutionMap.getNode(actualSubscriberId);
        getLogger().trace(".forwardTask(): The TopologyNode for the target currentNodeElement->{}", currentNodeElement);
        TopologyNodeFunctionFDNToken targetWUPFunctionToken = currentNodeElement.getNodeFunctionFDN().getFunctionToken();
        getLogger().trace(".forwardTask(): The WUPToken for the target targetWUPFunctionToken->{}", targetWUPFunctionToken);
        WUPComponentNames routeName = new WUPComponentNames(targetWUPFunctionToken);
        // Create FulfillmentTask and Inject into Target WUP
        getLogger().trace(".forwardTask(): Create actually PetasosFulfillmentTask: Start");
        PetasosFulfillmentTask petasosFulfillmentTask = fulfillmentTaskFactory.newFulfillmentTask(actionableTask.getInstance(), currentNodeElement);
        getLogger().trace(".forwardTask(): Create actually PetasosFulfillmentTask: petasosFulfillmentTask->{}", petasosFulfillmentTask);
        getLogger().trace(".forwardTask(): Create actually PetasosFulfillmentTask: Finish");
        petasosFulfillmentTask.getTaskWorkItem().getPayloadTopicID().setTargetProcessingPlantParticipantName(subscriber.getSubsystemParticipantName());
        //
        // Register The FulfillmentTask
        getLogger().trace(".forwardTask(): Register PetasosFulfillmentTask: Start");
        PetasosFulfillmentTaskSharedInstance petasosFulfillmentSharedInstance = fulfilmentTaskBroker.registerFulfillmentTask(petasosFulfillmentTask, false);
        getLogger().trace(".forwardTask(): Register PetasosFulfillmentTask: Finish");
        getLogger().trace(".forwardTask(): Insert PetasosFulfillmentTask into Next WUP Ingress Processor: Start");
        String targetCamelEndpoint = routeName.getEndPointWUPContainerIngresProcessorIngres();
        getLogger().trace(".forwardTask(): Insert PetasosFulfillmentTask into Next WUP Ingress Processor: targetCamelEndpoint->{}", targetCamelEndpoint);
        //
        // Create The TaskJobCard
        PetasosTaskJobCard petasosTaskJobCard = taskJobCardFactory.newPetasosTaskJobCard(petasosFulfillmentSharedInstance.getInstance());
        taskJobCardInstanceFactory.newTaskJobCardSharedInstanceAccessor(petasosTaskJobCard);
        //
        // Get out metricsAgent & do add some metrics
        WorkUnitProcessorMetricsAgent metricsAgent = camelExchange.getProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, WorkUnitProcessorMetricsAgent.class);
        metricsAgent.incrementInternalMessageDistributionCount();
        metricsAgent.touchLastActivityInstant();
        StringBuilder distributionMessageBuilder = new StringBuilder();
        distributionMessageBuilder.append("--- Distributing new PetasosFulfillmentTask ---");
        distributionMessageBuilder.append(" ("+ getTimeFormatter().format(Instant.now())+ ") ---\n");
        distributionMessageBuilder.append("TaskID (FulfillmentTask) --> " + petasosFulfillmentTask.getTaskId().getId() + "\n");
        distributionMessageBuilder.append("TaskID (ActionableTask) --> " + petasosFulfillmentTask.getActionableTaskId().getId() + "\n");
        distributionMessageBuilder.append("Target --> " + subscriber.getParticipantName() + "\n");
        metricsAgent.sendITOpsNotification(distributionMessageBuilder.toString());
        //
        // Forward the Task
        if(getLogger().isDebugEnabled())
        {
            getLogger().debug(".forwardTask(): Forwarding To->{}, Task->{}",subscriber.getParticipantName(), petasosFulfillmentTask.getActionableTaskId().getId() );
        }
        camelProducerService.sendBody(targetCamelEndpoint, ExchangePattern.InOnly, petasosFulfillmentSharedInstance);
        getLogger().trace(".forwardTask(): Insert PetasosFulfillmentTask into Next WUP Ingress Processor: Finish");
    }

    private void tracePrintSubscribedWUPSet(Set<WorkUnitProcessorSoftwareComponent> wupSet) {
        getLogger().trace(".tracePrintSubscribedWUPSet(): Subscribed WUP Set --> {}", wupSet.size());
        Iterator<WorkUnitProcessorSoftwareComponent> tokenIterator = wupSet.iterator();
        while (tokenIterator.hasNext()) {
            getLogger().trace(".forwardUoW2WUPs(): Subscribed WUP Ingres Point --> {}", tokenIterator.next());
        }
    }
}
