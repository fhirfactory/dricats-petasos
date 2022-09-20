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
package net.fhirfactory.dricats.petasos.tasking.caches.local;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.petasos.tasking.caches.common.PetasosTaskSharedInstanceBase;
import net.fhirfactory.pegacorn.core.interfaces.tasks.PetasosTaskCacheServiceInterface;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.fulfillment.datatypes.TaskFulfillmentType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.interception.TaskInterceptionType;
import net.fhirfactory.pegacorn.core.model.petasos.wup.PetasosTaskJobCard;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetasosFulfillmentTaskSharedInstance extends PetasosTaskSharedInstanceBase {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosFulfillmentTaskSharedInstance.class);

    //
    // Constructor(s)
    //

    public PetasosFulfillmentTaskSharedInstance(TaskIdType taskId, PetasosTaskCacheServiceInterface cache){
        super(taskId, cache);
        if(getInstance() == null){
            PetasosFulfillmentTask newTask = new PetasosFulfillmentTask();
            newTask.setTaskId(taskId);
            PetasosFulfillmentTask cacheTask = (PetasosFulfillmentTask)getTaskCache().registerTask(newTask);
            PetasosFulfillmentTask clonedCacheTask = SerializationUtils.clone(cacheTask);
            setLocalInstance(clonedCacheTask);
        } else {
            PetasosFulfillmentTask clonedTask = SerializationUtils.clone(getInstance());
            setLocalInstance(clonedTask);
        }
    }

    public PetasosFulfillmentTaskSharedInstance(PetasosFulfillmentTask task, PetasosTaskCacheServiceInterface cache){
        super(task, cache);
    }

    //
    // Business Methods
    //

    @Override
    public void refresh(){
        synchronized (getTaskCache().getTaskLock(getLocalInstance().getTaskId())){
            setLocalInstance(getTaskCache().getTask(getLocalInstance().getTaskId()));
        }
    }

    @Override
    public void update(){
        synchronized (getTaskCache().getTaskLock(getLocalInstance().getTaskId())) {
            PetasosFulfillmentTask cacheInstance = (PetasosFulfillmentTask) getTaskCache().getTask(getLocalInstance().getTaskId());
            cacheInstance.update(getLocalInstance());
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
    public boolean hasActionableTaskId(){
        boolean hasValue = getInstance().getActionableTaskId() != null;
        return(hasValue);
    }

    public TaskIdType getActionableTaskId() {
        return (getInstance().getActionableTaskId());
    }

    public void setActionableTaskId(TaskIdType fulfilledTaskPetasosTaskIdentity) {
        getInstance().setActionableTaskId(fulfilledTaskPetasosTaskIdentity);
    }

    @JsonIgnore
    public boolean hasTaskJobCard(){
        boolean hasValue = getInstance().getTaskJobCard() != null;
        return(hasValue);
    }

    public PetasosTaskJobCard getTaskJobCard() {
        return (getInstance().getTaskJobCard());
    }

    public void setTaskJobCard(PetasosTaskJobCard taskJobCard) {
        getInstance().setTaskJobCard(taskJobCard);
    }


    public boolean isaRetry() {
        return (getInstance().isaRetry());
    }

    public void setaRetry(boolean aRetry) {
        getInstance().setaRetry(aRetry);
    }

    @JsonIgnore
    public boolean hasTaskInterception(){
        boolean hasValue = getInstance().hasTaskInterception();
        return(hasValue);
    }

    public TaskInterceptionType getTaskInterception() {
        return (getTaskInterception());
    }

    public void setTaskInterception(TaskInterceptionType taskInterception) {
        getInstance().setTaskInterception(taskInterception);
    }



    //
    // Getters and Setters
    //

    @Override
    protected Logger getLogger(){
        return(LOG);
    }

    public PetasosFulfillmentTask getInstance() {
        return (PetasosFulfillmentTask) getLocalInstance();
    }

    //
    // To String
    //

    @Override
    public String toString() {
        return "PetasosFulfillmentTaskSharedInstance{" +
                "taskFulfillment=" + getTaskFulfillment() +
                ", actionableTaskId=" + getActionableTaskId() +
                ", taskJobCard=" + getTaskJobCard() +
                ", aRetry=" + isaRetry() +
                ", instance=" + getInstance() +
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
                ", taskInterception=" + getTaskInterception() +
                '}';
    }
}
