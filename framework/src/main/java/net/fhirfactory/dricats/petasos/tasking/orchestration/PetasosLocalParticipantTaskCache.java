/*
 * Copyright (c) 2022 Mark A. Hunter
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

package net.fhirfactory.dricats.petasos.tasking.orchestration;

import net.fhirfactory.dricats.interfaces.petasos.ipc.services.agents.tasks.fulfillment.PetasosTaskLocalProcessorInterface;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.status.datatypes.TaskOutcomeStatusType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.status.valuesets.ActionableTaskOutcomeStatusEnum;
import net.fhirfactory.dricats.model.petasos.tasking.fulfillment.execctrl.PetasosTaskJobCard;
import net.fhirfactory.dricats.petasos.tasking.orchestration.interfaces.PetasosTaskCacheUpdateCallbackInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is the Cache Data Manager (CacheDM) for the ServiceModule WorkUnitActivity Episode ID
 * finalisation map. This map essentially allows for registration of WUPs that have registered interest
 * in the output UoW from a particular Episode. It then tracks when those "downstream" WUPs register a
 * new Episode ID for the processing out the output UoW from this "upstream" WorkUnitAcitivity Episode.
 * <p>
 * It uses a ConcurrentHasMap to store a full list of all downstream WUP Registered instances:
 * ConcurrentHashMap<FDNToken, WUAEpisodeFinalisationRegistrationStatus> downstreamRegistrationStatusSet
 where the FDNToken is the WUPInstanceID and the WUAEpisodeFinalisationRegistrationStatus is their
 registration status.
 <p>
 * It also uses a ConcurrentHashMap to store a list of WUPs that have registered to consume the specific
 * UoW of the current WUAEpisodeID
 * ConcurrentHashMap<FDNToken, FDNTokenSet> downstreamWUPRegistrationMap
 where the FDNToken is the EpisodeID and the FDNTokenSet is the list of WUPInstanceIDs for the downstream WUPS.
 *
 * @author Mark A. Hunter
 * @since 2020.07.01
 */

@ApplicationScoped
public class PetasosLocalParticipantTaskCache implements PetasosTaskLocalProcessorInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosLocalParticipantTaskCache.class);

    //
    // A per-participant queue of Tasks (their identifiers)
    private ConcurrentHashMap<PetasosParticipantId, Queue<String>> participantQueue;

    //
    // A complete set of tasks current cached or active within this ProcessingPlant
    private ConcurrentHashMap<String, PetasosActionableTask> taskCache;

    //
    // A cache of the "Actively Being Fulfilled" Tasks (their identifiers)
    private ConcurrentHashMap<PetasosParticipantId, Set<String>> activeTaskMap;

    //
    // Lock Object
    private Object taskCacheLock;

    //
    // Constructor(s)
    //
    public PetasosLocalParticipantTaskCache() {
    	participantQueue = new ConcurrentHashMap<>();
        taskCache = new ConcurrentHashMap<>();
        activeTaskMap = new ConcurrentHashMap<>();
        taskCacheLock = new Object();
    }

    //
    // Post Construct
    //


    //
    // Business Methods
    //
    
    // 
    // Participant Queue(s) Methods

    /**
     * This method does three things:
     *  1. It adds the ActionableTask to the taskCache (if it isn't already there)
     *  2. It creates an TaskQueue for the given Participant (if one isn't already created)
     *  3. It adds the task to the TaskQueue for the given Participant (if it isn't already in the queue)
     *
     * Note that there will only ever be 1 participant for which the Task will be queued for.
     *
     * @param participantId The participantId of the WUP which is meant fulfill the task completion activity
     * @param actionableTask The ActionableTask which needs to be fulfilled by participant
     *
     */
    public void offer(PetasosParticipantId participantId, PetasosActionableTask actionableTask) {
    	getLogger().debug(".queueTask(): Entry, participantId->{}, actionableTask->{}", participantId, actionableTask);

        if(participantId != null && actionableTask != null){
            synchronized (getTaskCacheLock()){
                //
                // If the task isn't already in the task cache, add it
                if(!getTaskCache().containsKey(actionableTask.getTaskId().getId())){
                    getTaskCache().put(actionableTask.getTaskId().getId(), actionableTask);
                }
                //
                // If there is no existing queue for this participant, create one!
                if(!getParticipantQueue().containsKey(participantId)){
                    Queue participantInstanceQueue = new ConcurrentLinkedQueue<>();
                    getParticipantQueue().put(participantId, participantInstanceQueue);
                }
                //
                // Only add the task to the queue if it isn't already there
                Queue<String> queue = getParticipantQueue().get(participantId);
                if(!queue.contains(actionableTask.getTaskId().getId())){
                    queue.offer(actionableTask.getTaskId().getId());
                }
            }
        } else {
            if(getLogger().isDebugEnabled()) {
                if (participantId == null) {
                    getLogger().debug(".queueTask(): participantId is null, not adding to queue");
                }
                if (actionableTask == null) {
                    getLogger().debug(".queueTask(): actionableTask is null, not adding to queue");
                }
            }
        }
        getLogger().debug(".queueTask(): Exit");
    }

    /**
     *
     * @param participantId
     * @return
     */
    public PetasosActionableTask poll(PetasosParticipantId participantId){
        getLogger().debug(".getNext(): Entry, participantId->{}", participantId);
        PetasosActionableTask actionableTask = null;
        synchronized(getTaskCacheLock()) {
            if (getParticipantQueue().containsKey(participantId)) {
                String actionableTaskId = getParticipantQueue().get(participantId).poll();
                actionableTask = getTaskCache().get(actionableTaskId);
                if (actionableTask.getTaskOutcomeStatus().getOutcomeStatus().equals(ActionableTaskOutcomeStatusEnum.ACTIONABLE_TASK_OUTCOME_STATUS_CANCELLED)) {
                    getTaskCache().remove(actionableTask.getTaskId().getId());
                    actionableTask = null;
                }
            }
            if (actionableTask != null) {
                Set<String> taskIds = null;
                if (getActiveTaskMap().containsKey(participantId)) {
                    taskIds = getActiveTaskMap().get(participantId);
                } else {
                    taskIds = new HashSet<>();
                    getActiveTaskMap().put(participantId, taskIds);
                }
                if (!taskIds.contains(actionableTask.getTaskId().getId())) {
                    taskIds.add(actionableTask.getTaskId().getId());
                }
            }
        }
        getLogger().debug(".getNext(): Exit, actionableTask->{}", actionableTask);
        return(actionableTask);
    }

    /**
     *
     * @param participantId
     * @return
     */
    public PetasosActionableTask peek(PetasosParticipantId participantId){
        getLogger().debug(".peek(): Entry, participantId->{}", participantId);

        PetasosActionableTask actionableTask = null;

        getLogger().debug(".peek(): Exit, actionableTask->{}", actionableTask);
        return(actionableTask);
    }

    /**
     *
     * @param taskId
     */
    public void cancel(TaskIdType taskId){
        getLogger().debug(".cancel(): Entry, taskId->{}", taskId);
        if(taskId == null){
            getLogger().debug(".cancel(): Exit, taskId is null");
        }
        synchronized (getTaskCacheLock()){
            if(getTaskCache().containsKey(taskId.getId())){
                PetasosActionableTask task = getTaskCache().get(taskId.getId());
                if(task != null){
                    if(!task.hasTaskOutcomeStatus()){
                        TaskOutcomeStatusType outcome = new TaskOutcomeStatusType();
                        task.setTaskOutcomeStatus(outcome);
                    }
                    task.getTaskOutcomeStatus().setOutcomeStatus(ActionableTaskOutcomeStatusEnum.ACTIONABLE_TASK_OUTCOME_STATUS_CANCELLED);
                    task.getTaskOutcomeStatus().setEntryInstant(Instant.now());
                }
            }
        }
        getLogger().debug(".cancel(): Exit");
    }

    //
    // Global Interface (Task Receiver/Task Cancellation)
    //

    @Override
    public PetasosTaskJobCard fulfillTask(PetasosParticipantId participantId, PetasosActionableTask actionableTask) {
        return null;
    }

    @Override
    public PetasosTaskJobCard cancelTask(PetasosParticipantId participantId, TaskIdType taskId) {
        return null;
    }


    //
    // Getters (and Setters)
    //

    protected Map<String, PetasosActionableTask> getTaskCache(){
        return(this.taskCache);
    }

    protected Map<PetasosParticipantId, Set<String>> getActiveTaskMap(){
        return(this.activeTaskMap);
    }

    protected Map<PetasosParticipantId, Queue<String>> getParticipantQueue(){
        return(participantQueue);
    }

    protected Object getTaskCacheLock(){
        return(this.taskCacheLock);
    }

    protected Logger getLogger(){
        return(LOG);
    }
}
