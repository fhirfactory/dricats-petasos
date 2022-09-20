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
package net.fhirfactory.dricats.petasos.tasking.caches.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.petasos.tasking.caches.common.PetasosTaskSharedInstanceBase;
import net.fhirfactory.dricats.petasos.tasking.orchestration.PetasosLocalParticipantTaskCache;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.completion.datatypes.TaskCompletionSummaryType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.fulfillment.datatypes.TaskFulfillmentType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetasosActionableTaskSharedInstance extends PetasosTaskSharedInstanceBase {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosActionableTaskSharedInstance.class);

    //
    // Constructor(s)
    //

    public PetasosActionableTaskSharedInstance(TaskIdType taskId, PetasosLocalParticipantTaskCache actionableTaskDM){
        super(taskId, actionableTaskDM);
        if(getInstance() == null){
            PetasosActionableTask newTask = new PetasosActionableTask();
            newTask.setTaskId(taskId);
            PetasosActionableTask cacheTask = (PetasosActionableTask)getTaskCache().registerTask(newTask);
            PetasosActionableTask clonedCacheTask = SerializationUtils.clone(cacheTask);
            setLocalInstance(clonedCacheTask);
        }
    }

    public PetasosActionableTaskSharedInstance(PetasosActionableTask actionableTask, PetasosLocalParticipantTaskCache actionableTaskDM){
        super(actionableTask, actionableTaskDM);
    }

    //
    // Business Methods
    //

    @Override
    public void refresh(){
        synchronized (getTaskCache().getTaskLock(getLocalInstance().getTaskId())){
            setLocalInstance(getTaskCache().refreshTask(getInstance()));
        }
    }

    @Override
    public void update(){
        getLogger().debug(".update(): getInstance()->{}", getInstance());
        if(getInstance() == null){
            getLogger().error("WARNING WARNING WARNING --> Cannot update ActionableTask instance, it is null");
        } else {
            if (getInstance().hasTaskId()) {
                if (getTaskCache().getTaskLock(getInstance().getTaskId()) == null) {
                    getLogger().error("WARNING WARNING WARNING --> Cannot obtain an object lock for ActionableTask instance");
                    if (getTaskCache().getTask(getInstance().getTaskId()) != null) {
                        getTaskCache().addTaskLock(getInstance().getTaskId());
                    }
                }
                synchronized (getTaskCache().getTaskLock(getInstance().getTaskId())) {
                    PetasosActionableTask clonedCacheInstance = (PetasosActionableTask) getTaskCache().synchroniseTask(getInstance());
                    setLocalInstance(clonedCacheInstance);
                }
            }
        }
    }

    @JsonIgnore
    public boolean hasTaskFulfillment(){
        boolean hasValue = getInstance().getTaskFulfillment() != null;
        return(hasValue);
    }

    public TaskFulfillmentType getTaskFulfillment() {
        return (getInstance().getTaskFulfillment());
    }

    public void setTaskFulfillment(TaskFulfillmentType taskFulfillment) {
        getInstance().setTaskFulfillment(taskFulfillment);
    }

    @JsonIgnore
    public boolean hasTaskCompletionSummary(){
        boolean hasValue = getInstance().getTaskCompletionSummary() != null;
        return(hasValue);
    }

    public TaskCompletionSummaryType getTaskCompletionSummary() {
        return (getInstance().getTaskCompletionSummary());
    }

    public void setTaskCompletionSummary(TaskCompletionSummaryType taskCompletion) {
        getInstance().setTaskCompletionSummary(taskCompletion);
    }

    //
    // Get Lock
    //

    public Object getLockObject(TaskIdType taskId){
        Object taskLockObject = getTaskCache().getTaskLock(taskId);
        return(taskLockObject);
    }

    //
    // Getters and Setters
    //

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    public PetasosActionableTask getInstance() {
        return (PetasosActionableTask) getLocalInstance();
    }

    //
    // To String
    //

    @Override
    public String toString() {
        return "PetasosActionableTaskSharedInstance{" +
                "taskFulfillment=" + getTaskFulfillment() +
                ", taskCompletionSummary=" + getTaskCompletionSummary() +
                ", executionStatus=" + getExecutionStatus() +
                ", sourceResourceId=" + getSourceResourceId() +
                ", creationInstant=" + getCreationInstant() +
                ", updateInstant=" + getUpdateInstant() +
                ", taskId=" + getTaskId() +
                ", taskType=" + getTaskType() +
                ", taskWorkItem=" + getTaskWorkItem() +
                ", taskTraceability=" + getTaskTraceability() +
                ", taskOutcomeStatus=" + getTaskOutcomeStatus() +
                ", registered=" + isRegistered() +
                ", taskPerformerTypes=" + getTaskPerformerTypes() +
                ", taskReason=" + getTaskReason() +
                ", taskNodeAffinity=" + getTaskNodeAffinity() +
                ", aggregateTaskMembership=" + getAggregateTaskMembership() +
                ", taskContext=" + getTaskContext() +
                '}';
    }
}
