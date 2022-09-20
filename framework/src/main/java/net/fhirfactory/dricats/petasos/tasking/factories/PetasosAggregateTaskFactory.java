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

import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosAggregateTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.factories.TaskIdTypeFactory;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.reason.datatypes.TaskReasonType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.traceability.datatypes.TaskTraceabilityType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.work.datatypes.ParcelOfWorkType;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PetasosAggregateTaskFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PetasosAggregateTaskFactory.class);

    @Inject
    private TaskIdTypeFactory taskIdFactory;

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

    public PetasosAggregateTask newAggregateTask(PetasosActionableTask actionableTask, WorkUnitProcessorSoftwareComponent wupNode) {
        getLogger().debug(".newFulfillmentTask(): Enter, actionableTask->{}, wupNode->{}", actionableTask, wupNode );

        // Get the core Items from the actionableTask (PetasosActionableTask)
        ParcelOfWorkType taskWorkItem = SerializationUtils.clone(actionableTask.getTaskWorkItem());
        TaskTraceabilityType taskTraceability = SerializationUtils.clone(actionableTask.getTaskTraceability());
        TaskIdType actionableTaskId = SerializationUtils.clone(actionableTask.getTaskId());
        TaskReasonType taskReason = actionableTask.getTaskReason();

        // Construct the PetasosOversightTask using the ActionableTask details

        PetasosAggregateTask oversightTask = new PetasosAggregateTask();
        oversightTask.setTaskId(taskIdFactory.newTaskId());

        oversightTask.setTaskWorkItem(taskWorkItem);
        oversightTask.setTaskTraceability(taskTraceability);
        oversightTask.setActionableTaskId(actionableTaskId);
        oversightTask.setTaskReason(taskReason);

        //

        return(oversightTask);
    }


    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
