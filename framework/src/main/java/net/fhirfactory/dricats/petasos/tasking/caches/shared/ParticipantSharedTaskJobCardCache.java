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
package net.fhirfactory.dricats.petasos.tasking.caches.shared;

import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.pegacorn.core.model.petasos.wup.PetasosTaskJobCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ParticipantSharedTaskJobCardCache {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantSharedTaskJobCardCache.class);

    // ActionableTaskId, FulfillmentTask JobCard Set (will be across ALL processing plants) Map
    private ConcurrentHashMap<TaskIdType, PetasosTaskJobCard> actionableTaskJobCardMap;
    private ConcurrentHashMap<TaskIdType, Object> actionableTaskJobCardLockMap;
    //
    // Whole cache lock
    private Object cacheLock;

    //
    // Constructor(s)
    //

    public ParticipantSharedTaskJobCardCache(){
        this.actionableTaskJobCardMap = new ConcurrentHashMap<>();
        this.actionableTaskJobCardLockMap = new ConcurrentHashMap<>();
        this.cacheLock = new Object();
    }

    //
    // Business methods
    //

    public Object getJobCardLock(TaskIdType actionableTaskId){
        Object jobCardLock = getActionableTaskJobCardLockMap().get(actionableTaskId);
        return(jobCardLock);
    }

    public void registerJobCard(PetasosTaskJobCard jobCard){
        getLogger().debug(".registerJobCard(): Entry, jobCard->{}", jobCard);

        //
        // Defensive Programming
        if(jobCard == null){
            getLogger().debug(".registerJobCard(): Exit, jobCard is null, exiting");
        }
        if(!jobCard.hasActionableTaskId()){
            getLogger().debug(".registerJobCard(): Exit, jobCard has no Actionable Task Id, exiting");
        }

        //
        // Grab the main mapping values
        TaskIdType actionableTaskId = jobCard.getActionableTaskId();

        //
        // If it is already there - we are going to clobber is... use this method with discretion
        synchronized (getCacheLock()) {
            if (getActionableTaskJobCardMap().containsKey(actionableTaskId)) {
                getActionableTaskJobCardMap().remove(actionableTaskId);
            }
            if(!getActionableTaskJobCardLockMap().containsKey(actionableTaskId)){
                getActionableTaskJobCardLockMap().put(actionableTaskId, new Object());
            }
            jobCard.setLastActivityCheckInstant(Instant.now());
            getActionableTaskJobCardMap().put(jobCard.getActionableTaskId(), jobCard);
        }
        getLogger().debug(".registerJobCard(): Exit");
    }

    public void removeJobCard(PetasosTaskJobCard jobCard){
        getLogger().debug(".removeJobCard(): Entry, jobCard->{}", jobCard);

        //
        // Defensive Programming
        if(jobCard == null){
            getLogger().debug(".removeJobCard(): Exit, jobCard is null, exiting");
        }
        if(!jobCard.hasActionableTaskId()){
            getLogger().debug(".removeJobCard(): Exit, jobCard has no Actionable Task Id, exiting");
        }

        //
        // Delete the values
        synchronized (getCacheLock()) {
            if (getActionableTaskJobCardMap().containsKey(jobCard.getActionableTaskId())) {
                getActionableTaskJobCardLockMap().remove(jobCard.getActionableTaskId());
                getActionableTaskJobCardMap().remove(jobCard.getActionableTaskId());
            }
        }

        getLogger().debug(".removeJobCard(): Exit");
    }

    public PetasosTaskJobCard getJobCard(TaskIdType taskId){
        getLogger().debug(".getJobCard(): Entry, taskId->{}", taskId);

        //
        // Defensive Programming
        if(taskId == null){
            getLogger().debug(".getJobCard(): Exit, taskId is null, returning null");
            return(null);
        }

        //
        // Try and retrieve the Job Card
        PetasosTaskJobCard petasosTaskJobCard = getActionableTaskJobCardMap().get(taskId);

        getLogger().debug(".getJobCard(): Exit, retrieved JobCard ->{}", petasosTaskJobCard);
        return(petasosTaskJobCard);
    }

    //
    // Getters (and Setters)
    //

    protected Map<TaskIdType, PetasosTaskJobCard> getActionableTaskJobCardMap(){
        return(actionableTaskJobCardMap);
    }

    private ConcurrentHashMap<TaskIdType, Object> getActionableTaskJobCardLockMap(){
        return(this.actionableTaskJobCardLockMap);
    }

    protected Logger getLogger(){
        return(LOG);
    }

    public Object getCacheLock(){
        return(this.cacheLock);
    }
}
