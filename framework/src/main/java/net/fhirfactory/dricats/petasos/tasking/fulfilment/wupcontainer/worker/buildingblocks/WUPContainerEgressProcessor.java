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

package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks;

import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.interfaces.oam.notifications.PetasosITOpsNotificationAgentInterface;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.ITOpsNotificationContent;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.work.datatypes.TaskWorkItemType;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.LocalPetasosFulfilmentTaskActivityController;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.notifications.PetasosITOpsNotificationContentFactory;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class WUPContainerEgressProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(WUPContainerEgressProcessor.class);
    protected Logger getLogger(){
        return(LOG);
    }

    @Inject
    LocalPetasosFulfilmentTaskActivityController fulfilmentTaskActivityController;

    @Inject
    private PetasosITOpsNotificationAgentInterface notificationAgent;

    @Inject
    private PetasosITOpsNotificationContentFactory notificationContentFactory;


    public PetasosFulfillmentTaskSharedInstance egressContentProcessor(PetasosFulfillmentTaskSharedInstance fulfillmentTask, Exchange camelExchange) {
        if(getLogger().isDebugEnabled()){
            getLogger().debug(".egressContentProcessor(): Entry, fulfillmentTaskId/ActionableTaskId->{}/{}", fulfillmentTask.getTaskId(), fulfillmentTask.getActionableTaskId());
        }
      	getLogger().trace(".egressContentProcessor(): Entry, fulfillmentTask->{}", fulfillmentTask);

        //
        // Get out metricsAgent & do add some metrics
        WorkUnitProcessorMetricsAgent metricsAgent = camelExchange.getProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, WorkUnitProcessorMetricsAgent.class);
        boolean createFailureNotification = false;
        boolean createSoftFailureNotification = false;
        switch (fulfillmentTask.getTaskFulfillment().getStatus()) {
            case FULFILLMENT_EXECUTION_STATUS_FINISHED:
                if(fulfillmentTask.hasTaskWorkItem()){
                    if(fulfillmentTask.getTaskWorkItem().hasProcessingOutcome()){
                        if(fulfillmentTask.getTaskWorkItem().getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_SOFTFAILURE)){
                            createSoftFailureNotification = true;
                            fulfillmentTask.getTaskWorkItem().setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
                        }
                    }
                }
                fulfilmentTaskActivityController.notifyFulfillmentTaskExecutionFinish(fulfillmentTask);
                metricsAgent.touchLastActivityFinishInstant();
                metricsAgent.incrementFinishedTasks();
                break;
            case FULFILLMENT_EXECUTION_STATUS_CANCELLED:
                fulfilmentTaskActivityController.notifyFulfillmentTaskExecutionCancellation(fulfillmentTask);
                metricsAgent.incrementCancelledTasks();
                metricsAgent.touchLastActivityFinishInstant();
                break;
            case FULFILLMENT_EXECUTION_STATUS_NO_ACTION_REQUIRED:
                fulfilmentTaskActivityController.notifyFulfillmentTaskExecutionNoActionRequired(fulfillmentTask);
                metricsAgent.incrementFinishedTasks();
                metricsAgent.touchLastActivityFinishInstant();
                break;
            default:
                fulfilmentTaskActivityController.notifyFulfillmentTaskExecutionFailure(fulfillmentTask);
                metricsAgent.incrementFailedTasks();
                metricsAgent.touchLastActivityFinishInstant();
                createFailureNotification = true;
        }
        fulfillmentTask.update();
        metricsAgent.touchLastActivityInstant();

        //
        // Add some notifications (for failures)

        // 1st, for general failure
        if(createFailureNotification) {
            String notificationContent = null;
            if (fulfillmentTask.hasTaskWorkItem()) {
                if (fulfillmentTask.getTaskWorkItem().hasEgressContent()) {
                    for (UoWPayload payload : fulfillmentTask.getTaskWorkItem().getEgressContent().getPayloadElements()) {
                        String notification = notificationContentFactory.newNotificationContentFromUoWPayload(fulfillmentTask.getTaskFulfillment().getStatus(), payload);
                        metricsAgent.sendITOpsNotification(notification);
                    }
                }
            }
        }

        // Now, for soft-failure
        if(createSoftFailureNotification){
            TaskWorkItemType workItem = fulfillmentTask.getTaskWorkItem();
            sendSoftFailureNotification(metricsAgent, workItem);
        }

        if(getLogger().isDebugEnabled()){
            getLogger().debug(".egressContentProcessor(): Exit, fulfillmentTaskId/ActionableTaskId->{}/{}: Status->{}", fulfillmentTask.getTaskId(), fulfillmentTask.getActionableTaskId(),fulfillmentTask.getTaskFulfillment().getStatus() );
        }

        //
        // And we're done!
        return (fulfillmentTask);
    }

    private void sendSoftFailureNotification(WorkUnitProcessorMetricsAgent metricsAgent, TaskWorkItemType workItem){
        getLogger().debug(".sendSoftFailureNotification(): Entry, workItem->{}", workItem);

        if(!workItem.hasEgressContent()){
            getLogger().debug(".sendSoftFailureNotification(): Exit, workItem has no egressContent!");
        }

        ITOpsNotificationContent notificationContent = null;
        for(UoWPayload currentPayload: workItem.getEgressContent().getPayloadElements()){
            if(currentPayload.getPayloadManifest().hasContentDescriptor()){
                if(currentPayload.getPayloadManifest().getContentDescriptor().hasDataParcelSubCategory()){
                    if(currentPayload.getPayloadManifest().getContentDescriptor().getDataParcelSubCategory().contentEquals("Exception")){
                        notificationContent = notificationContentFactory.newNotificationForSoftFailure(currentPayload.getPayload());
                        break;
                    }
                }
            }
        }

        if(notificationContent != null){
            metricsAgent.sendITOpsNotification(notificationContent.getContent(), notificationContent.getFormattedContent());
        }
    }
}
