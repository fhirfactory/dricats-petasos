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
package net.fhirfactory.dricats.petasos.tasking.caches.local;

import net.fhirfactory.pegacorn.core.interfaces.tasks.PetasosTaskCacheServiceInterface;
import net.fhirfactory.pegacorn.core.model.componentid.ComponentIdType;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.fulfillment.valuesets.FulfillmentExecutionStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class acts as the Data Manager for the PetasosFulfillmentTask set within the local
 * ProcessingPlant. That is, it is the single management point for the
 * task element set itself. It does not implement business logic associated
 * with the surrounding activity associated with each Parcel beyond provision
 * of helper methods associated with search-set and status-set collection
 * methods.
 *
 * @author Mark A. Hunter
 * @since 2020-06-01
 */
@ApplicationScoped
public class LocalFulfillmentTaskCache implements PetasosTaskCacheServiceInterface {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFulfillmentTaskCache.class);

    private ConcurrentHashMap<TaskIdType, PetasosFulfillmentTask> fulfillmentTaskCache;
    private ConcurrentHashMap<TaskIdType, Object> fulfillmentTaskCacheLockMap;
    private Object cacheLock;

    //
    // Constructor(s)
    //

    public LocalFulfillmentTaskCache() {
        fulfillmentTaskCache = new ConcurrentHashMap<TaskIdType, PetasosFulfillmentTask>();
        fulfillmentTaskCacheLockMap = new ConcurrentHashMap<>();
        this.cacheLock = new Object();
    }

    //
    // Business Methods
    //

    public Integer getTaskCacheSize(){
        int size = fulfillmentTaskCache.size();
        return(size);
    }


    @Override
    public PetasosTask registerTask(PetasosTask task) {
        getLogger().debug(".registerTask(): Entry, task --> {}", task);
        if (task == null) {
            getLogger().debug(".registerTask(): Exit, task is null, returning null");
            return (null);
        }
        if (!task.hasTaskId()) {
            getLogger().debug(".registerTask(): Exit, task as no id, returning null");
            return (null);
        }
        PetasosFulfillmentTask fulfillmentTask = (PetasosFulfillmentTask) task;
        TaskIdType taskId = task.getTaskId();
        PetasosFulfillmentTask clonedTask = null;
        synchronized (getCacheLock()) {
            if (!getFulfillmentTaskCacheLockMap().containsKey(taskId)) {
                getFulfillmentTaskCacheLockMap().put(task.getTaskId(), new Object());
            }
            synchronized (getFulfillmentTaskCacheLockMap().get(taskId)) {
                if (getFulfillmentTaskCache().containsKey(taskId)) {
                    getFulfillmentTaskCache().remove(taskId);
                }
                getFulfillmentTaskCache().put(taskId, fulfillmentTask);
                fulfillmentTask.getTaskFulfillment().setRegistrationInstant(Instant.now());
                fulfillmentTask.getTaskFulfillment().setStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_REGISTERED);
                clonedTask = SerializationUtils.clone(fulfillmentTask);
            }
        }

        getLogger().debug(".registerTask(): Exit, clonedTask->{}", clonedTask);
        return(clonedTask);
    }

    @Override
    public PetasosTask getTask(TaskIdType taskId) {
        getLogger().debug(".getTask(): Entry, taskId --> {}", taskId);
        PetasosFulfillmentTask task = null;
        if (getFulfillmentTaskCache().containsKey(taskId)) {
            task = getFulfillmentTaskCache().get(taskId);
        }
        getLogger().debug(".getTask(): Exit, task->{}", task);
        return (task);
    }

    @Override
    public Object getTaskLock(TaskIdType taskId) {
        getLogger().debug(".getTaskLock(): Entry, taskId --> {}", taskId);
        Object lockObject = null;
        if (getFulfillmentTaskCacheLockMap().containsKey(taskId)) {
            lockObject = getFulfillmentTaskCacheLockMap().get(taskId);
        }
        getLogger().debug(".getTaskLock(): Exit, lockObject->{}", lockObject);
        return (lockObject);
    }

    @Override
    public void addTaskLock(TaskIdType taskId){
        if(taskId != null){
            Object lockObject = getFulfillmentTaskCacheLockMap().get(taskId);
            if(lockObject == null){
                getFulfillmentTaskCacheLockMap().put(taskId, new Object());
            }
        }
    }

    @Override
    public PetasosTask removeTask(TaskIdType taskId) {
        getLogger().debug(".removeTask(): Entry, taskId->{}", taskId);
        if (taskId == null) {
            return(null);
        }
        PetasosFulfillmentTask task = null;
        synchronized (getCacheLock()) {
            if (this.getFulfillmentTaskCache().containsKey(taskId)) {
                synchronized (getFulfillmentTaskCacheLockMap().get(taskId)) {
                    task = getFulfillmentTaskCache().get(taskId);
                    getLogger().trace(".removeTask(): Removing task from map/cache");
                    this.getFulfillmentTaskCache().remove(taskId);
                }
                this.getFulfillmentTaskCacheLockMap().remove(taskId);
            } else {
                getLogger().trace(".removeTask(): taks is not in map/cache, cannot remove it!");
            }
        }
        getLogger().debug(".removeTask(): Exit");
        return(task);
    }

    @Override
    public PetasosTask removeTask(PetasosTask task) {
        getLogger().debug(".removeTask(): Entry, task->{}", task);
        if (task == null) {
            return (null);
        }
        PetasosFulfillmentTask fulfillmentTask = (PetasosFulfillmentTask) removeTask(task.getTaskId());
        getLogger().debug(".removeTask(): Exit");
        return(fulfillmentTask);
    }

    @Override
    public PetasosTask synchroniseTask(PetasosTask task) {
        getLogger().debug(".synchroniseTask() Entry, task --> {}", task);
        if (task == null) {
            throw (new IllegalArgumentException(".synchroniseTask(): fulfillmentTask is null"));
        }
        PetasosFulfillmentTask fulfillmentTask = (PetasosFulfillmentTask) task;
        if(!getFulfillmentTaskCacheLockMap().containsKey(fulfillmentTask.getTaskId())){
            getFulfillmentTaskCacheLockMap().put(fulfillmentTask.getTaskId(), new Object());
        }
        PetasosFulfillmentTask clonedTask = null;
        synchronized (getFulfillmentTaskCacheLockMap().get(fulfillmentTask.getTaskId())) {
            if (fulfillmentTaskCache.containsKey(fulfillmentTask.getTaskId())) {
                fulfillmentTaskCache.remove(fulfillmentTask.getTaskId());
            }
            fulfillmentTaskCache.put(fulfillmentTask.getTaskId(), fulfillmentTask);
            clonedTask = SerializationUtils.clone(fulfillmentTask);
        }
        getLogger().debug(".synchroniseTask(): Exit");
        return(clonedTask);
    }

    @Override
    public PetasosTask refreshTask(PetasosTask task) {
        getLogger().debug(".refreshTask() Entry, task --> {}", task);
        if (task == null) {
            throw (new IllegalArgumentException(".refreshTask(): fulfillmentTask is null"));
        }
        PetasosFulfillmentTask fulfillmentTask = (PetasosFulfillmentTask) task;
        if(!fulfillmentTaskCacheLockMap.containsKey(fulfillmentTask.getTaskId())){
            fulfillmentTaskCacheLockMap.put(fulfillmentTask.getTaskId(), new Object());
            fulfillmentTaskCache.put(fulfillmentTask.getTaskId(), fulfillmentTask);
        }
        PetasosFulfillmentTask cacheFulfillmentTask = null;
        PetasosTask cacheTask = getTask(task.getTaskId());
        if(cacheTask != null) {
            cacheFulfillmentTask = SerializationUtils.clone((PetasosFulfillmentTask) getTask(task.getTaskId()));
        }
        getLogger().debug(".refreshTask(): Exit");
        return(cacheFulfillmentTask);
    }

    public List<PetasosFulfillmentTask> getFulfillmentTaskList() {
        getLogger().debug(".getFulfillmentTaskList(): Entry");
        List<PetasosFulfillmentTask> parcelList = new LinkedList<PetasosFulfillmentTask>();
        synchronized (getCacheLock()) {
            fulfillmentTaskCache.entrySet().forEach(entry -> parcelList.add(entry.getValue()));
        }
        getLogger().debug(".getFulfillmentTaskList(): Exit");
        return (parcelList);
    }

    public List<PetasosFulfillmentTask> getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum status) {
        getLogger().debug(".getFulfillmentTaskByStatus(): Entry, status->{}", status);
        List<PetasosFulfillmentTask> taskList = new LinkedList<>();
        Iterator<PetasosFulfillmentTask> parcelListIterator = getFulfillmentTaskList().iterator();
        synchronized (getCacheLock()) {
            while (parcelListIterator.hasNext()) {
                PetasosFulfillmentTask currentParcel = parcelListIterator.next();
                if (currentParcel.hasTaskFulfillment()) {
                    if (currentParcel.getTaskFulfillment().hasStatus()) {
                        if (currentParcel.getTaskFulfillment().getStatus().equals(status)) {
                            taskList.add(currentParcel);
                        }
                    }
                }
            }
        }
        getLogger().debug(".getFulfillmentTaskByStatus(): Exit");
        return (taskList);
    }

    public List<PetasosFulfillmentTask> getActiveFulfillmentTasks() {
        List<PetasosFulfillmentTask> taskList = getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_ACTIVE);
        return (taskList);
    }

    public List<PetasosFulfillmentTask> getFinishedFulfillmentTasks() {
        List<PetasosFulfillmentTask> taskList = getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_FINISHED);
        return (taskList);
    }

    public List<PetasosFulfillmentTask> getFailedFulfillmentTasks() {
        List<PetasosFulfillmentTask> taskList = getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_FAILED);
        return (taskList);
    }

    public List<PetasosFulfillmentTask> getCancelledFulfillmentTasks() {
        List<PetasosFulfillmentTask> taskList = getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_CANCELLED);
        return (taskList);
    }

    public List<PetasosFulfillmentTask> getLocalInProgressFulfillmentTasks() {
        getLogger().debug(".getInProgressParcelSet(): Entry");
        List<PetasosFulfillmentTask> parcelList = new LinkedList<PetasosFulfillmentTask>();
        parcelList.addAll(getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_ACTIVE));
        parcelList.addAll(getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_INITIATED));
        parcelList.addAll(getFulfillmentTaskByExecutionStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_REGISTERED));
        getLogger().debug(".getInProgressParcelSet(): Exit");
        return (parcelList);
    }

    public List<PetasosFulfillmentTask> getFulfillmentTaskByActionableTaskId(TaskIdType taskId) {
        getLogger().debug(".getFulfillmentTaskByActionableTaskId(): Entry, taskId --> {}" + taskId);
        List<PetasosFulfillmentTask> parcelList = new LinkedList<PetasosFulfillmentTask>();
        synchronized (getCacheLock()) {
            Iterator<PetasosFulfillmentTask> parcelListIterator = getFulfillmentTaskList().iterator();
            while (parcelListIterator.hasNext()) {
                PetasosFulfillmentTask currentParcel = parcelListIterator.next();
                if (currentParcel.hasActionableTaskId()) {
                    if (currentParcel.getActionableTaskId().equals(taskId)) {
                        parcelList.add(currentParcel);
                    }
                }
            }
        }
        getLogger().debug(".getFulfillmentTaskByActionableTaskId(): Exit");
        return (parcelList);
    }

    public PetasosFulfillmentTask getCurrentFulfillmetTaskForWUP(ComponentIdType wupComponentId) {
        getLogger().debug(".getCurrentFulfillmetTaskForWUP(): Entry, wupComponentId->{}",wupComponentId);
        List<PetasosFulfillmentTask> taskList = new LinkedList<>();
        synchronized (getCacheLock()) {
            Iterator<PetasosFulfillmentTask> taskListIterator = getFulfillmentTaskList().iterator();
            while (taskListIterator.hasNext()) {
                PetasosFulfillmentTask currentParcel = taskListIterator.next();
                if (currentParcel.hasTaskFulfillment()) {
                    if (currentParcel.getTaskFulfillment().hasFulfillerWorkUnitProcessor()) {
                        if (currentParcel.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentID().equals(wupComponentId)) {
                            return (currentParcel);
                        }
                    }
                }
            }
        }
        getLogger().debug(".getCurrentFulfillmetTaskForWUP(): Exit");
        return (null);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected ConcurrentHashMap<TaskIdType, PetasosFulfillmentTask> getFulfillmentTaskCache() {
        return fulfillmentTaskCache;
    }

    protected ConcurrentHashMap<TaskIdType, Object> getFulfillmentTaskCacheLockMap(){
        return(this.fulfillmentTaskCacheLockMap);
    }

    protected Object getCacheLock(){
        return(this.cacheLock);
    }
}
