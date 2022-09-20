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
package net.fhirfactory.dricats.petasos.tasking.caches.common;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.fhirfactory.dricats.interfaces.petasos.tasking.routing.PetasosTaskCacheServiceInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.datagrid.datatypes.DatagridElementSourceResourceIdType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.TaskContextType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.performer.datatypes.TaskPerformerTypeType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.reason.datatypes.TaskReasonType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.status.datatypes.TaskOutcomeStatusType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.status.valuesets.PetasosTaskExecutionStatusEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.tasktype.TaskTypeType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.traceability.datatypes.TaskTraceabilityType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.TaskWorkItem;

public abstract class PetasosTaskSharedInstanceBase {

    private PetasosTaskCacheServiceInterface taskCache;
    private PetasosTask localInstance;

    //
    // Constructor(s)
    //

    public PetasosTaskSharedInstanceBase(TaskIdType taskId, PetasosTaskCacheServiceInterface taskCache){
        this.taskCache = taskCache;
        if(getTaskCache().getTaskLock(taskId) != null){
            setLocalInstance(getTaskCache().getTask(taskId));
        }
    }

    public PetasosTaskSharedInstanceBase(PetasosTask task, PetasosTaskCacheServiceInterface taskCache){
        this.taskCache = taskCache;
        if(taskCache.getTaskLock(task.getTaskId()) == null) {
            PetasosTask clonedTask = SerializationUtils.clone(task);
            taskCache.registerTask(clonedTask);
        }
        setLocalInstance(task);
    }

    //
    // Abstract Methods
    //

    abstract protected Logger getLogger();
    abstract public void refresh();
    abstract public void update();

    //
    // Business Methods
    //

    @JsonIgnore
    public boolean hasExecutionStatus(){
        boolean hasValue = getLocalInstance().hasExecutionStatus();
        return(hasValue);
    }

    public void setExecutionStatus(PetasosTaskExecutionStatusEnum status){
        getLocalInstance().setExecutionStatus(status);
    }

    public PetasosTaskExecutionStatusEnum getExecutionStatus(){
        return(getLocalInstance().getExecutionStatus());
    }

    @JsonIgnore
    public boolean hasSourceResourceId(){
        boolean hasValue = getLocalInstance().getSourceResourceId() != null;
        return(hasValue);
    }

    public DatagridElementSourceResourceIdType getSourceResourceId() {
        return (getLocalInstance().getSourceResourceId());
    }

    public void setSourceResourceId(DatagridElementSourceResourceIdType sourceResourceId) {
        getLocalInstance().setSourceResourceId(sourceResourceId);
    }

    public Instant getCreationInstant() {
        return (getLocalInstance().getCreationInstant());
    }

    public void setCreationInstant(Instant creationInstant) {
        getLocalInstance().setCreationInstant(creationInstant);
    }

    public Instant getUpdateInstant() {
        return (getLocalInstance().getUpdateInstant());
    }

    public void setUpdateInstant(Instant updateInstant) {
        getLocalInstance().setUpdateInstant(updateInstant);
    }

    @JsonIgnore
    public boolean hasTaskId(){
        boolean hasValue = getLocalInstance().getTaskId() != null;
        return(hasValue);
    }

    public TaskIdType getTaskId() {
        return (getLocalInstance().getTaskId());
    }

    public void setTaskId(TaskIdType localTaskId) {
        getLocalInstance().setTaskId(localTaskId);
    }


    @JsonIgnore
    public boolean hasTaskType(){
        boolean hasValue = getLocalInstance().getTaskType() != null;
        return(hasValue);
    }

    public TaskTypeType getTaskType() {
        return (getLocalInstance().getTaskType());
    }

    public void setTaskType(TaskTypeType taskType) {
        getLocalInstance().setTaskType(taskType);
    }


    @JsonIgnore
    public boolean hasTaskWorkItem(){
        boolean hasValue = getLocalInstance().getTaskWorkItem() != null;
        return(hasValue);
    }

    public TaskWorkItem getTaskWorkItem() {
        return (getLocalInstance().getTaskWorkItem());
    }

    public void setTaskWorkItem(TaskWorkItem taskWorkItem) {
        setTaskWorkItem(taskWorkItem);
    }

    @JsonIgnore
    public boolean hasTaskTraceability(){
        boolean hasValue = getLocalInstance().getTaskTraceability() != null;
        return(hasValue);
    }

    public TaskTraceabilityType getTaskTraceability() {
        return (getLocalInstance().getTaskTraceability());
    }

    public void setTaskTraceability(TaskTraceabilityType taskTraceability) {
        getLocalInstance().setTaskTraceability(taskTraceability);
    }


    @JsonIgnore
    public boolean hasTaskOutcomeStatus(){
        boolean hasValue = getLocalInstance().getTaskOutcomeStatus() != null;
        return(hasValue);
    }

    public TaskOutcomeStatusType getTaskOutcomeStatus() {
        return (getLocalInstance().getTaskOutcomeStatus());
    }

    public void setTaskOutcomeStatus(TaskOutcomeStatusType taskOutcomeStatus) {
        getLocalInstance().setTaskOutcomeStatus(taskOutcomeStatus);
    }

    public boolean isRegistered() {
        return (getLocalInstance().isRegistered());
    }

    public void setRegistered(boolean registered) {
        getLocalInstance().setRegistered(registered);
    }

    @JsonIgnore
    public boolean hasTaskPerformerTypes(){
        boolean hasValue = getLocalInstance().getTaskPerformerTypes() != null;
        return(hasValue);
    }

    public List<TaskPerformerTypeType> getTaskPerformerTypes() {
        return (getLocalInstance().getTaskPerformerTypes());
    }

    public void setTaskPerformerTypes(List<TaskPerformerTypeType> taskPerformerTypes) {
        getLocalInstance().setTaskPerformerTypes(taskPerformerTypes);
    }

    @JsonIgnore
    public boolean hasTaskReason(){
        boolean hasValue = getLocalInstance().getTaskReason() != null;
        return(hasValue);
    }

    public TaskReasonType getTaskReason() {
        return (getLocalInstance().getTaskReason());
    }

    public void setTaskReason(TaskReasonType taskReason) {
        getLocalInstance().setTaskReason(taskReason);
    }

    @JsonIgnore
    public boolean hasTaskNodeAffinity(){
        boolean hasValue = getLocalInstance().getTaskNodeAffinity() != null;
        return(hasValue);
    }

    public ComponentId getTaskNodeAffinity() {
        return (getLocalInstance().getTaskNodeAffinity());
    }

    public void setTaskNodeAffinity(ComponentId taskNodeAffinity) {
        getLocalInstance().setTaskNodeAffinity(taskNodeAffinity);
    }

    @JsonIgnore
    public boolean hasAggregateTaskMembership(){
        boolean hasValue = getLocalInstance().getAggregateTaskMembership() != null;
        return(hasValue);
    }

    public Set<TaskIdType> getAggregateTaskMembership() {
        return (getLocalInstance().getAggregateTaskMembership());
    }

    public void setAggregateTaskMembership(Set<TaskIdType> aggregateTaskMembership) {
        getLocalInstance().setAggregateTaskMembership(aggregateTaskMembership);
    }

    @JsonIgnore
    public boolean hasTaskContext(){
        boolean hasValue = getLocalInstance().getTaskContext() != null;
        return(hasValue);
    }

    public TaskContextType getTaskContext() {
        return (getLocalInstance().getTaskContext());
    }

    public void setTaskContext(TaskContextType taskContext) {
        getLocalInstance().setTaskContext(taskContext);
    }

    //
    // Getters and Setters
    //

    public PetasosTask getLocalInstance() {
        return localInstance;
    }

    public void setLocalInstance(PetasosTask localInstance) {
        this.localInstance = localInstance;
    }

    protected PetasosTaskCacheServiceInterface getTaskCache(){
        return(this.taskCache);
    }
}
