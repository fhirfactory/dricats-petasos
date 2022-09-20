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
package net.fhirfactory.dricats.model.transforms.tofhir.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.internals.fhir.r4.resources.identifier.PegacornIdentifierFactory;
import net.fhirfactory.dricats.model.transforms.tofhir.task.factories.TaskPerformerTypeFactory;
import net.fhirfactory.dricats.model.transforms.tofhir.task.factories.TaskStatusReasonFactory;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosAggregateTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.status.valuesets.ActionableTaskOutcomeStatusEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.tasktype.valuesets.TaskTypeTypeEnum;

@ApplicationScoped
public class FHIRTaskFromPetasosAggregateTask extends FHIRTaskFromPetasosTask {
    private static final Logger LOG = LoggerFactory.getLogger(FHIRTaskFromPetasosAggregateTask.class);

    @Inject
    private TaskStatusReasonFactory statusReasonFactory;

    @Inject
    TaskPerformerTypeFactory performerTypeFactory;

    @Inject
    private PegacornIdentifierFactory identifierFactory;

    //
    // Constructor(s)
    //

    public FHIRTaskFromPetasosAggregateTask(){
        super();
    }

    //
    // Implemented Abstract (Business) Methods
    //

    @Override
    protected Reference specifyBasedOn(PetasosTask petasosTask) {
        getLogger().debug(".specifyBasedOn(): Entry");
        PetasosAggregateTask aggregateTask = (PetasosAggregateTask) petasosTask;
        //
        // Get the Trigger Event/Task ID
        TaskIdType triggerTaskId = null;
        if(aggregateTask.hasTaskContext()){
            if(aggregateTask.getTaskContext().hasTaskTriggerSummary()){
                if(aggregateTask.getTaskContext().getTaskTriggerSummary().hasTriggerTaskId()){
                    triggerTaskId = aggregateTask.getTaskContext().getTaskTriggerSummary().getTriggerTaskId();
                }
            }
        }
        if(triggerTaskId == null){
            getLogger().debug(".specifyBasedOn(): Exit, returning null (no trigger task id found");
            return(null);
        }
        Identifier triggerTaskIdentifier = getTaskIdentifierFactory().newTaskIdentifier(TaskTypeTypeEnum.PETASOS_ACTIONABLE_TASK_TYPE, triggerTaskId);
        Reference triggerTaskReference = new Reference();
        triggerTaskReference.setIdentifier(triggerTaskIdentifier);
        triggerTaskReference.setDisplay(triggerTaskId.getLocalId());
        triggerTaskReference.setType(ResourceType.Task.toString());
        getLogger().debug(".specifyBasedOn(): Exit, triggerTaskReference->{}", triggerTaskReference);
        return (triggerTaskReference);
    }

    @Override
    protected Identifier specifyGroupIdentifier(PetasosTask petasosTask) {
        // TODO Implement Group Identifier functionality for PetasosAggregateTasks
        return null;
    }

    @Override
    protected List<Reference> specifyPartOf(PetasosTask petasosTask) {
        getLogger().debug(".specifyPartOf(): Entry");
        PetasosAggregateTask aggregateTask = (PetasosAggregateTask) petasosTask;
        //
        // Create List<Reference> for Part Of (Aggregate Task) Membership
        List<Reference> membershipList = new ArrayList<>();
        //
        // Get the Aggregate Task Id if it exists
        TaskIdType aggregateTaskId = null;
        if(aggregateTask.hasAggregateTaskMembership()){
            if(!aggregateTask.getAggregateTaskMembership().isEmpty()){
                for(TaskIdType currentAggregateTaskId: aggregateTask.getAggregateTaskMembership()){
                    Identifier aggregateTaskIdentifer = getTaskIdentifierFactory().newTaskIdentifier(TaskTypeTypeEnum.PETASOS_AGGREGATE_TASK_TYPE, currentAggregateTaskId);
                    Reference aggregateTaskReference = new Reference();
                    aggregateTaskReference.setIdentifier(aggregateTaskIdentifer);
                    aggregateTaskReference.setDisplay(aggregateTaskId.getLocalId());
                    aggregateTaskReference.setType(ResourceType.Task.toString());
                    membershipList.add(aggregateTaskReference);
                }
            }
        }
        getLogger().debug(".specifyPartOf(): Exit");
        return (membershipList);
    }

    @Override
    protected Task.TaskStatus specifyStatus(PetasosTask petasosTask) {
        getLogger().debug(".specifyStatus(): Entry");
        PetasosAggregateTask aggregateTask = (PetasosAggregateTask) petasosTask;

        if(aggregateTask.hasTaskOutcomeStatus()) {
            ActionableTaskOutcomeStatusEnum outcomeStatus = aggregateTask.getTaskOutcomeStatus().getOutcomeStatus();
            Task.TaskStatus outcome = null;
            switch(outcomeStatus){
                case ACTIONABLE_TASK_OUTCOME_STATUS_UNKNOWN:
                    outcome = Task.TaskStatus.NULL;
                    break;
                case ACTIONABLE_TASK_OUTCOME_STATUS_CANCELLED:
                    outcome = Task.TaskStatus.CANCELLED;
                    break;
                case ACTIONABLE_TASK_OUTCOME_STATUS_ACTIVE:
                    outcome = Task.TaskStatus.INPROGRESS;
                    break;
                case ACTIONABLE_TASK_OUTCOME_STATUS_FINISHED:
                    outcome = Task.TaskStatus.COMPLETED;
                    break;
                case ACTIONABLE_TASK_OUTCOME_STATUS_FINALISED:
                    outcome = Task.TaskStatus.COMPLETED;
                    break;
                case ACTIONABLE_TASK_OUTCOME_STATUS_FAILED:
                    outcome = Task.TaskStatus.FAILED;
                    break;
            }
            getLogger().debug(".specifyStatus(): Exit, outcome->{}", outcome);
            return(outcome);
        }
        getLogger().debug(".specifyStatus(): Exit, outcome->{}", Task.TaskStatus.NULL);
        return(Task.TaskStatus.NULL);
    }

    @Override
    protected CodeableConcept specifyStatusReason(PetasosTask petasosTask) {
        return(null);
    }

    @Override
    protected CodeableConcept specifyBusinessStatus(PetasosTask petasosTask) {
        return null;
    }

    @Override
    protected Task.TaskIntent specifyIntent(PetasosTask petasosTask) {
        getLogger().debug(".specifyIntent(): Entry");
        Task.TaskIntent intent = Task.TaskIntent.INSTANCEORDER;
        getLogger().debug(".specifyIntent(): Exit, intent->{}", intent);
        return (intent);
    }

    @Override
    protected Task.TaskPriority specifyPriority(PetasosTask petasosTask) {
        getLogger().debug(".specifyPriority(): Entry");
        Task.TaskPriority priority = Task.TaskPriority.ROUTINE;
        getLogger().debug(".specifyPriority(): Exit, priority->{}", priority);
        return (priority);
    }

    @Override
    protected CodeableConcept specifyCode(PetasosTask petasosTask) {

        return null;
    }

    @Override
    protected String specifyDescription(PetasosTask petasosTask) {
        return null;
    }

    @Override
    protected Reference specifyFocus(PetasosTask petasosTask) {
        return null;
    }

    @Override
    protected Reference specifyFor(PetasosTask petasosTask) {
        getLogger().debug(".specifyFor(): Entry");
        PetasosAggregateTask aggregateTask = (PetasosAggregateTask) petasosTask;

        Reference benificiaryResource = null;
        if(aggregateTask.hasTaskContext()){
            if(aggregateTask.getTaskContext().hasTaskBeneficiary()){
                benificiaryResource = aggregateTask.getTaskContext().getTaskBeneficiary();
            }
        }

        getLogger().debug(".specifyFor(): Exit, benficiary->{}", benificiaryResource);
        return (benificiaryResource);
    }

    @Override
    protected Reference specifyEncounter(PetasosTask petasosTask) {
        getLogger().debug(".specifyEncounter(): Entry");

        PetasosAggregateTask aggregateTask = (PetasosAggregateTask) petasosTask;

        Reference encounter = null;
        if(aggregateTask.hasTaskContext()){
            if(aggregateTask.getTaskContext().hasTaskEncounter()){
                encounter = aggregateTask.getTaskContext().getTaskEncounter();
            }
        }

        getLogger().debug(".specifyEncounter(): Exit, encounter->{}", encounter);
        return (encounter);
    }

    @Override
    protected Period specifyExecutionPeriod(PetasosTask petasosTask){
        return(null);
    }

    @Override
    protected Period specifyExecutionPeriod(PetasosTask petasosTask, Set<PetasosTask> subTaskSet) {
        getLogger().debug(".specifyEncounter(): Entry");

        PetasosAggregateTask aggregateTask = (PetasosAggregateTask) petasosTask;

        Period executionPeriod = null;
        Date lastActivityDate = null;
        Date firstActivityDate = null;

        for(PetasosTask currentSubTask: subTaskSet){
            Date testFirstActivityDate = null;
            Date testLastActivityDate = null;
            if(currentSubTask instanceof  PetasosAggregateTask){
                PetasosAggregateTask currentAggregateSubTask = (PetasosAggregateTask) currentSubTask;

                if(currentAggregateSubTask.hasTaskReporting()){
                    if(currentAggregateSubTask.getTaskReporting().hasFinalisationInstant()){
                        testLastActivityDate = Date.from(currentAggregateSubTask.getTaskReporting().getFinalisationInstant());
                    } else if(currentAggregateSubTask.getTaskReporting().hasFinishInstant()){
                        testLastActivityDate = Date.from(currentAggregateSubTask.getTaskReporting().getFinishInstant());
                    }
                    if(testLastActivityDate != null){
                        if(lastActivityDate == null){
                            lastActivityDate = testLastActivityDate;
                        } else {
                            if(testLastActivityDate.after(lastActivityDate)){
                                lastActivityDate = testLastActivityDate;
                            }
                        }
                    }

                    if(currentAggregateSubTask.getTaskReporting().hasRegistrationInstant()){
                        testFirstActivityDate = Date.from(currentAggregateSubTask.getTaskReporting().getRegistrationInstant());
                        if(firstActivityDate == null){
                            firstActivityDate = testFirstActivityDate;
                        } else {
                            if(testFirstActivityDate.before(firstActivityDate)){
                                firstActivityDate = testFirstActivityDate;
                            }
                        }
                    }
                }
                if(currentSubTask instanceof PetasosActionableTask){
                    PetasosActionableTask currentActionableTask = (PetasosActionableTask) currentSubTask;
                    if(currentActionableTask.hasTaskFulfillment()) {
                        if (currentActionableTask.getTaskFulfillment().hasRegistrationDate()) {
                            testFirstActivityDate = currentActionableTask.getTaskFulfillment().getRegistrationDate();
                            if (firstActivityDate == null) {
                                firstActivityDate = testFirstActivityDate;
                            } else if (testFirstActivityDate.before(firstActivityDate)) {
                                firstActivityDate = testFirstActivityDate;
                            }
                        }
                        if (currentActionableTask.getTaskFulfillment().hasFinalisationInstant()){
                            testLastActivityDate = currentActionableTask.getTaskFulfillment().getFinalisationDate();
                            if(lastActivityDate == null){
                                lastActivityDate = testLastActivityDate;
                            } else if(testLastActivityDate.after(lastActivityDate)){
                                lastActivityDate = testLastActivityDate;
                            }
                        }
                    }
                }
            }
            boolean hasUsefulValue = false;
            if(firstActivityDate != null){
                executionPeriod.setStart(firstActivityDate);
                hasUsefulValue = true;
            }
            if(lastActivityDate != null){
                executionPeriod.setEnd(lastActivityDate);
                hasUsefulValue = true;
            }
            if(hasUsefulValue){
                return(executionPeriod);
            }
        }
        getLogger().debug(".specifyEncounter(): Exit, executionPeriod->{}", executionPeriod);
        return (null);
    }

    @Override
    protected Reference specifyRequester(PetasosTask petasosTask) {
        return null;
    }

    @Override
    protected List<CodeableConcept> specifyPerformerType(PetasosTask petasosTask) {
        getLogger().debug(".specifyPerformerType(): Entry");

        List<CodeableConcept> performerTypes = null;

        getLogger().debug(".specifyPerformerType(): Exit, performerTypes->{}", performerTypes);
        return (performerTypes);
    }

    @Override
    protected Reference specifyOwner(PetasosTask petasosTask) {
        getLogger().debug(".specifyOwner(): Entry");

        Reference owner = null;

        getLogger().debug(".specifyOwner(): Exit, owner->{}", owner);
        return(owner);
    }

    @Override
    protected CodeableConcept specifyReasonCode(PetasosTask petasosTask) {
        return null;
    }

    @Override
    protected Reference specifyReasonReference(PetasosTask petasosTask) {
        return null;
    }


    //
    // Getters (and Setters)
    //

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    protected TaskStatusReasonFactory getStatusReasonFactory(){
        return(this.statusReasonFactory);
    }

    protected TaskPerformerTypeFactory getPerformerTypeFactory(){
        return(this.performerTypeFactory);
    }

    protected PegacornIdentifierFactory getIdentifierFactory(){
        return(this.identifierFactory);
    }
}
