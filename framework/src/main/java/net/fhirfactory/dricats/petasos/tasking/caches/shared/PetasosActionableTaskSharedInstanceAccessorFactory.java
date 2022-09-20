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

import net.fhirfactory.dricats.petasos.tasking.orchestration.PetasosLocalParticipantTaskCache;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosActionableTaskSharedInstanceAccessorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosActionableTaskSharedInstanceAccessorFactory.class);

    @Inject
    private PetasosLocalParticipantTaskCache taskCache;

    //
    // Constructor(s)
    //

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //

    public PetasosActionableTaskSharedInstance newActionableTaskSharedInstance(TaskIdType taskId){
        if(taskId == null){
            return(null);
        }
        PetasosActionableTask task = taskCache.getTask(taskId);
        PetasosActionableTaskSharedInstance actionableTaskSharedInstance = null;
        if(task == null){
            PetasosActionableTask newActionableTask = new PetasosActionableTask();
            newActionableTask.setTaskId(taskId);
            actionableTaskSharedInstance = new PetasosActionableTaskSharedInstance(newActionableTask, taskCache);
        }
        PetasosActionableTaskSharedInstance actionableTaskAccessor = new PetasosActionableTaskSharedInstance(taskId, taskCache);
        return(actionableTaskAccessor);
    }

    public PetasosActionableTaskSharedInstance newActionableTaskSharedInstance(PetasosActionableTask task){
        getLogger().debug(".newActionableTaskSharedInstance(), Entry, task->{}", task);
        if(task == null){
            getLogger().warn(".newActionableTaskSharedInstance(), Exit, task is null, returning null");
            return(null);
        }
        PetasosActionableTaskSharedInstance actionableTaskAccessor = new PetasosActionableTaskSharedInstance(task, taskCache);
        if(actionableTaskAccessor == null){
            PetasosActionableTask newActionableTask = new PetasosActionableTask();
            actionableTaskAccessor = new PetasosActionableTaskSharedInstance(newActionableTask, taskCache);
        }
        getLogger().debug(".newActionableTaskSharedInstance(), Exit, actionableTaskAccessor->{}", actionableTaskAccessor);
        return(actionableTaskAccessor);
    }

    public PetasosActionableTaskSharedInstance getActionableTaskSharedInstance(TaskIdType taskId){
        if(taskId == null){
            return(null);
        }
        PetasosActionableTask task = taskCache.getTask(taskId);
        if(task == null){
            return(null);
        }
        PetasosActionableTask clonedTask = SerializationUtils.clone(task);
        PetasosActionableTaskSharedInstance actionableTask = new PetasosActionableTaskSharedInstance(clonedTask, taskCache);
        return(actionableTask);
    }
}
