/*
 * Copyright (c) 2021 Mark A. Hunter
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
package net.fhirfactory.dricats.petasos.tasking.factories;

import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosFulfillmentTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.fulfillment.datatypes.FulfillmentTrackingIdType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.fulfillment.datatypes.TaskFulfillmentType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.fulfillment.valuesets.FulfillmentExecutionStatusEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.identity.factories.TaskIdTypeFactory;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.performer.datatypes.TaskPerformerTypeType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.reason.datatypes.TaskReasonType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.tasktype.TaskTypeType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.traceability.datatypes.TaskTraceabilityType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.TaskWorkItem;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PetasosFulfillmentTaskFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosFulfillmentTaskFactory.class);

    @Inject
    private TaskIdTypeFactory taskIdFactory;

    @Inject
    private PetasosTaskJobCardFactory jobCardFactory;

    //
    // Constructor(s)
    //

    /* none */

    //
    // Post Construct Initialisation(s)
    //

    /* none */

    //
    // Business Methods
    //

    public PetasosFulfillmentTask newFulfillmentTask(PetasosActionableTask actionableTask, ComponentId componentId) {
        getLogger().debug(".newFulfillmentTask(): Enter, actionableTask->{}, wupNode->{}", actionableTask, componentId );

        //
        // Create Empty PetasosFulfillmentTask
        PetasosFulfillmentTask fulfillmentTask = new PetasosFulfillmentTask();

        //
        // Create a TaskId (is local, so simple UUID is ok) and add to our Task
        TaskIdType fulfillmentTaskId = new TaskIdType();
        fulfillmentTaskId.setLocalId(UUID.randomUUID().toString());
        fulfillmentTask.setTaskId(fulfillmentTaskId);
        //
        // Get the Task Type, clone it and add it to our Task
        TaskTypeType taskType = SerializationUtils.clone(actionableTask.getTaskType());
        fulfillmentTask.setTaskType(taskType);
        //
        // Get the TaskWorkItem from the actionableTask, clone it and add it to our Task.
        TaskWorkItem taskWorkItem = SerializationUtils.clone(actionableTask.getTaskWorkItem());
        fulfillmentTask.setTaskWorkItem(taskWorkItem);
        //
        // Get the TaskTraceability Detail from the actionableTask, clone it and add it to our Task
        TaskTraceabilityType taskTraceability = SerializationUtils.clone(actionableTask.getTaskTraceability());
        fulfillmentTask.setTaskTraceability(taskTraceability);
        //
        // Get the ActionableTask's Id, clone it and add it to our Task
        TaskIdType actionableTaskId = SerializationUtils.clone(actionableTask.getTaskId());
        fulfillmentTask.setActionableTaskId(actionableTaskId);
        //
        // Get the ActionableTask's Task Reason (we don't need to clone it, it's an enum) and add it to our Task
        TaskReasonType taskReason = actionableTask.getTaskReason();
        fulfillmentTask.setTaskReason(taskReason);
        //
        // Get the Task Performer Types from the Actionable Task, clone them and add them to our task
        List<TaskPerformerTypeType> taskPerformers = new ArrayList<>();
        if(actionableTask.hasTaskPerformerTypes()){
            for(TaskPerformerTypeType currentPerformerType: actionableTask.getTaskPerformerTypes()){
                TaskPerformerTypeType clonedPerformerType = SerializationUtils.clone(currentPerformerType);
                taskPerformers.add(clonedPerformerType);
            }
        }
        fulfillmentTask.setTaskPerformerTypes(taskPerformers);
        //
        // Assign the node affinity of the fulfillment task (from the actionable task)
        fulfillmentTask.setTaskNodeAffinity(actionableTask.getTaskNodeAffinity());
        //
        // Now to add Fulfillment details
        TaskFulfillmentType fulfillment = new TaskFulfillmentType();
        fulfillment.setFulfillerComponentId(componentId);
        fulfillment.setStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_UNREGISTERED);
        fulfillment.setResilientActivity(true);
        FulfillmentTrackingIdType trackingId = new FulfillmentTrackingIdType(fulfillmentTask.getTaskId());
        fulfillment.setTrackingID(trackingId);
        fulfillment.setLastCheckedInstant(Instant.now());
        fulfillmentTask.setTaskFulfillment(fulfillment);
        //
        // Done! :)
        getLogger().debug(".newFulfillmentTask(): Exit, fulfillmentTask->{}", fulfillmentTask);
        return(fulfillmentTask);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
