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

package net.fhirfactory.dricats.petasos.tasking.fulfilment.outcomes;

import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.petasos.participant.PetasosParticipant;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.completion.datatypes.TaskCompletionSummaryType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.performer.datatypes.TaskPerformerTypeType;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.PetasosTaskExecutionStatusEnum;
import net.fhirfactory.pegacorn.deployment.topology.manager.TopologyIM;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.PetasosActionableTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.PetasosActionableTaskSharedInstanceAccessorFactory;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.PetasosTaskJobCardSharedInstanceAccessorFactory;
import net.fhirfactory.dricats.petasos.tasking.factories.PetasosActionableTaskFactory;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.work.datatypes.ParcelOfWorkType;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayloadSet;
import net.fhirfactory.dricats.petasos.tasking.factories.PetasosTaskJobCardFactory;
import net.fhirfactory.dricats.petasos.tasking.oversight.LocalPetasosActionableTaskActivityController;
import net.fhirfactory.dricats.petasos.tasking.orchestration.helpers.LocalTaskDistributionDecisionEngine;
import net.fhirfactory.dricats.petasos.tasking.observations.reporting.tasks.agents.WorkUnitProcessorTaskReportAgent;
import net.fhirfactory.dricats.petasos.tasking.orchestration.PetasosLocalParticipantTaskCache;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Dependent
public class TaskOutcome2NewTasksBean {
    private static final Logger LOG = LoggerFactory.getLogger(TaskOutcome2NewTasksBean.class);
    protected Logger getLogger(){
        return(LOG);
    }

    @Inject
    TopologyIM topologyProxy;

    @Inject
    private PetasosLocalParticipantTaskCache actionableTaskCache;

    @Inject
    private PetasosActionableTaskFactory actionableTaskFactory;

    @Inject
    private LocalPetasosActionableTaskActivityController actionableTaskActivityController;

    @Inject
    private LocalTaskDistributionDecisionEngine distributionDecisionEngine;

    @Inject
    private PetasosActionableTaskSharedInstanceAccessorFactory actionableTaskSharedInstanceFactory;

    @Inject
    private PetasosTaskJobCardSharedInstanceAccessorFactory jobCardInstanceFactory;

    @Inject
    private PetasosTaskJobCardFactory jobCardFactory;

    /**
     * This method performs tree key tasks:
     *
     * 1. It extracts each UoWPayload from the egressPayloadSet within the incomingUoW and creates a
     * new UoW (and, subsequently, a new WorkUnitTransportPacket) based on the content of those egress
     * UoWPayload elements.
     * 2. As part of the WorkUnitTransportPacket creation, it embeds the current ActivityID.
     * 3. It then returns a List<> of these new WorkUnitTransportPackets for distribution.
     *
     * It generates the
     * @param actionableTask
     * @param camelExchange
     * @return A List<> of WorkUnitTransportPackets - one for each egress UoWPayload element within the incoming UoW.
     */

    public List<PetasosActionableTaskSharedInstance> collectOutcomesAndCreateNewTasks(PetasosActionableTaskSharedInstance actionableTask, Exchange camelExchange) {
        getLogger().debug(".collectOutcomesAndCreateNewTasks(): Entry, actionableTask->{}", actionableTask);
        ParcelOfWorkType incomingUoW = actionableTask.getTaskWorkItem();
        UoWPayloadSet egressContent = incomingUoW.getEgressContent();
        Set<UoWPayload> egressPayloadList = egressContent.getPayloadElements();
        if (getLogger().isDebugEnabled()) {
            int counter = 0;
            for(UoWPayload currentPayload: egressPayloadList){
                getLogger().debug(".collectOutcomesAndCreateNewTasks(): payload (UoWPayload).PayloadTopic --> [{}] {}", counter, currentPayload.getPayloadManifest());
                getLogger().debug(".collectOutcomesAndCreateNewTasks(): payload (UoWPayload).Payload --> [{}] {}", counter, currentPayload.getPayload());
                counter++;
            }
        }

        ArrayList<PetasosActionableTask> newActionableTaskList = new ArrayList<>();
        synchronized (actionableTask.getLockObject(actionableTask.getTaskId())) {
            //
            // We need to establish whether this actionableTask has any successor (downstream) tasks (subscribed), cause if
            // it doesn't, then we need to tag the message as being the "last-in-chain" and also "finalised". If it does
            // have
            if (!actionableTask.hasTaskCompletionSummary()) {
                actionableTask.setTaskCompletionSummary(new TaskCompletionSummaryType());
                actionableTask.getTaskCompletionSummary().setLastInChain(false);
                actionableTask.getTaskCompletionSummary().setFinalised(false);
            }

            Boolean hasADownstreamTask = false;
            for (UoWPayload currentPayload : egressPayloadList) {
                DataParcelManifest payloadManifest = currentPayload.getPayloadManifest();
                List<PetasosParticipant> subscriberList = distributionDecisionEngine.deriveSubscriberList(payloadManifest);
                if(getLogger().isDebugEnabled()){
                    getLogger().debug(".collectOutcomesAndCreateNewTasks(): number of subscribers for egressPayload->{} is count->{}", payloadManifest, subscriberList.size());
                }
                if (!subscriberList.isEmpty()) {
                    for(PetasosParticipant currentSubscriber: subscriberList) {
                        ParcelOfWorkType newWorkItem = new ParcelOfWorkType(currentPayload);
                        getLogger().trace(".collectOutcomesAndCreateNewTasks(): newWorkItem->{}", newWorkItem);
                        PetasosActionableTask newDownstreamTask = newActionableTask(actionableTask.getInstance(), newWorkItem);
                        TaskPerformerTypeType downstreamPerformerType = new TaskPerformerTypeType();
                        downstreamPerformerType.setKnownFulfillerInstance(currentSubscriber.getComponentID());
                        downstreamPerformerType.setRequiredPerformerType(currentSubscriber.getNodeFunctionFDN());
                        downstreamPerformerType.setRequiredParticipantName(currentSubscriber.getParticipantName());
                        downstreamPerformerType.setRequiredSubsystemParticipantName(currentSubscriber.getSubsystemParticipantName());
                        downstreamPerformerType.setRequiredPerformerTypeDescription(currentSubscriber.getParticipantDisplayName());
                        if(!newDownstreamTask.hasTaskPerformerTypes()){
                            newDownstreamTask.setTaskPerformerTypes(new ArrayList<>());
                        }
                        newDownstreamTask.getTaskPerformerTypes().add(downstreamPerformerType);
                        newActionableTaskList.add(newDownstreamTask);
                        actionableTask.getTaskCompletionSummary().addDownstreamTask(newDownstreamTask.getTaskId());
                    }
                    hasADownstreamTask = true;
                }
            }
            if (!hasADownstreamTask) {
                if (actionableTask.getTaskCompletionSummary().getDownstreamTaskMap().isEmpty()) {
                    actionableTask.getTaskCompletionSummary().setLastInChain(true);
                    actionableTask.getTaskCompletionSummary().setFinalised(true);
                }
            }
            actionableTask.update();
        }

        //
        // Now we are going to create a new ActionableTaskSharedInstance and register it. This will allow us to "finalise"
        // the upstream task.
        List<PetasosActionableTaskSharedInstance> newTaskList = new ArrayList<>();
        for(PetasosActionableTask currentNewActionableTask: newActionableTaskList){
            PetasosActionableTaskSharedInstance petasosActionableTaskSharedInstance = actionableTaskSharedInstanceFactory.newActionableTaskSharedInstance(currentNewActionableTask);
            newTaskList.add(petasosActionableTaskSharedInstance);
            PetasosTaskExecutionStatusEnum petasosTaskExecutionStatusEnum = actionableTaskActivityController.notifyTaskWaiting(petasosActionableTaskSharedInstance.getTaskId());
        }
        getLogger().trace(".collectOutcomesAndCreateNewTasks(): Updating actionableTask with task completion details");
        actionableTaskActivityController.notifyTaskFinalisation(actionableTask.getTaskId());

        //
        // Get out metricsAgent for the WUP that sent the task & do add some metrics
        WorkUnitProcessorTaskReportAgent taskReportAgent = camelExchange.getProperty(PetasosPropertyConstants.ENDPOINT_TASK_REPORT_AGENT_EXCHANGE_PROPERTY, WorkUnitProcessorTaskReportAgent.class);
        if(taskReportAgent != null){
            taskReportAgent.sendITOpsTaskReport(actionableTask.getInstance(), newActionableTaskList);
        }

        getLogger().debug(".collectOutcomesAndCreateNewTasks(): Exit, new PetasosActionableTasks created, number --> {} ", newTaskList.size());
        return (newTaskList);
    }

    private PetasosActionableTask newActionableTask(PetasosActionableTask previousActionableTask, ParcelOfWorkType work){
        getLogger().debug(".newActionableTask(): Entry, previousActionableTask->{}, work->{}", previousActionableTask,  work);
        if(previousActionableTask == null){
            getLogger().debug(".newActionableTask(): Exit, previousTaskFulfillmentDetail is null, returning null");
            return(null);
        }
        if(work == null){
            getLogger().debug(".newActionableTask(): Exit, No new work to be done, returning null");
            return(null);
        }
        PetasosActionableTask petasosActionableTask = actionableTaskFactory.newMessageBasedActionableTask(previousActionableTask, work);
        return(petasosActionableTask);
    }
}
