/*
 * Copyright (c) 2021 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.tasking.oversight.watchdogs;

import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.dricats.petasos.tasking.caches.local.LocalFulfillmentTaskCache;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.ParticipantSharedTaskJobCardCache;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.ProcessingPlantMetricsAgentAccessor;
import net.fhirfactory.dricats.petasos.tasking.orchestration.PetasosLocalParticipantTaskCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class LocalPetasosTaskCleanupWatchdog {
    private static final Logger LOG = LoggerFactory.getLogger(LocalPetasosTaskCleanupWatchdog.class);

    private Long TASK_CONTINUITY_CHECK_INITIAL_DELAY = 60000L; // milliseconds
    private Long TASK_CONTINUITY_CHECK_PERIOD = 15000L; // milliseconds
    private Long MINIMUM_TASK_AGE_FOR_RETIREMENT = 15L; // Seconds

    private Instant actionableTaskCheckInstant;
    private Instant taskJobCardCheckInstant;
    private boolean initialised;

    @Inject
    private PetasosLocalParticipantTaskCache actionableTaskDM;

    @Inject
    private ParticipantSharedTaskJobCardCache taskJobCardDM;

    @Inject
    private ProcessingPlantMetricsAgentAccessor processingPlantMetricsAgentAccessor;

    @Inject
    private LocalFulfillmentTaskCache fulfillmentTaskCache;

    //
    // Constructor(s)
    //

    public LocalPetasosTaskCleanupWatchdog(){
        this.actionableTaskCheckInstant = Instant.EPOCH;
        this.taskJobCardCheckInstant = Instant.EPOCH;
        this.initialised = false;
    }

    //
    // Post Constrct
    //

    @PostConstruct
    public void initialise(){
        getLogger().debug(".initialise(): Entry");
        if(this.initialised){
            getLogger().debug(".initialise(): Exit, already initialised, nothing to do!");
            return;
        } else {
            getLogger().info("GlobalPetasosTaskContinuityWatchdog::initialise(): Starting initialisation");
            scheduleTaskContinuityWatchdog();
            getLogger().info("GlobalPetasosTaskContinuityWatchdog::initialise(): Finished initialisation");
            this.initialised = true;
            getLogger().debug(".initialise(): Exit");
        }
    }

    //
    // Scheduling & Initialisation
    //

    public void scheduleTaskContinuityWatchdog() {
        getLogger().debug(".scheduleTaskContinuityWatchdog(): Entry");
        TimerTask startupWatchdogTask = new TimerTask() {
            public void run() {
                getLogger().debug(".taskContinuityWatchdog(): Entry");
                taskContinuityWatchdog();
                getLogger().debug(".taskContinuityWatchdog(): Exit");
            }
        };
        Timer timer = new Timer("taskContinuityWatchdogTimer");
        timer.schedule(startupWatchdogTask, TASK_CONTINUITY_CHECK_INITIAL_DELAY, TASK_CONTINUITY_CHECK_PERIOD);
        getLogger().debug(".scheduleTaskContinuityWatchdog(): Exit");
    }


    //
    // Actionable Task Controller / Watchdog
    //

    protected void taskContinuityWatchdog(){
        getLogger().debug(".taskContinuityWatchdog(): Entry");
        Set<TaskIdType> allTaskIds = actionableTaskDM.getAllTaskIds();
        processingPlantMetricsAgentAccessor.getMetricsAgent().updateLocalCacheStatus("ActionableTaskCacheSharedCache", allTaskIds.size());
        for(TaskIdType currentTaskId: allTaskIds){
            if(getLogger().isInfoEnabled()){
                getLogger().debug(".taskContinuityWatchdog(): Checking task {}", currentTaskId);
            }
            boolean unregisterTask = false;
            synchronized (actionableTaskDM.getTaskLock(currentTaskId)){
                PetasosActionableTask currentActionableTask = actionableTaskDM.getTask(currentTaskId);
                if(currentActionableTask.hasTaskCompletionSummary()){
                    if (currentActionableTask.getTaskCompletionSummary().isFinalised()) {
                        unregisterTask = true;
                    }
                }
                if(!unregisterTask) {
                    if (currentActionableTask.hasTaskFulfillment()) {
                        if (currentActionableTask.getTaskFulfillment().hasStatus()) {
                            switch (currentActionableTask.getTaskFulfillment().getStatus()) {
                                case FULFILLMENT_EXECUTION_STATUS_UNREGISTERED:
                                case FULFILLMENT_EXECUTION_STATUS_REGISTERED:
                                case FULFILLMENT_EXECUTION_STATUS_INITIATED:
                                case FULFILLMENT_EXECUTION_STATUS_ACTIVE:
                                case FULFILLMENT_EXECUTION_STATUS_ACTIVE_ELSEWHERE:

                                    break;
                                case FULFILLMENT_EXECUTION_STATUS_CANCELLED:
                                case FULFILLMENT_EXECUTION_STATUS_NO_ACTION_REQUIRED:
                                case FULFILLMENT_EXECUTION_STATUS_FINISHED:
                                case FULFILLMENT_EXECUTION_STATUS_FAILED:
                                case FULFILLMENT_EXECUTION_STATUS_FINISHED_ELSEWHERE:
                                    Long age = Instant.now().getEpochSecond() - currentActionableTask.getCreationInstant().getEpochSecond();
                                    if (age > MINIMUM_TASK_AGE_FOR_RETIREMENT) {
                                        unregisterTask = true;
                                    }
                                    break;
                                case FULFILLMENT_EXECUTION_STATUS_FINALISED:
                                case FULFILLMENT_EXECUTION_STATUS_FINALISED_ELSEWHERE:
                                    unregisterTask = true;
                                    break;
                            }
                        }
                    }
                }
            }
            if(unregisterTask){
                getLogger().debug(".taskContinuityWatchdog(): Task {} is finalised, removing from shared cache... start", currentTaskId);
                PetasosActionableTask unregisteredActionableTask = actionableTaskDM.removeTask(currentTaskId);
                getLogger().debug(".taskContinuityWatchdog(): Task {} is finalised, removing from shared cache... done...");
            }
            if(getLogger().isDebugEnabled()){
                getLogger().debug(".taskContinuityWatchdog(): Shared ActionableTaskCache size->{}", actionableTaskDM.getAllTaskIds().size());
            }
        }
        // Some metrics for the FulfillmentCache
        Integer taskCacheSize = fulfillmentTaskCache.getTaskCacheSize();
        processingPlantMetricsAgentAccessor.getMetricsAgent().updateLocalCacheStatus("LocalFulfillmentCache", taskCacheSize);
        getLogger().debug(".taskContinuityWatchdog(): Exit");
    }

    //
    // Task Job Card Controller / Watchdog
    //

    protected void taskJobCardWatchdog(){

    }

    //
    // Getters and Setters
    //

    public Instant getActionableTaskCheckInstant() {
        return actionableTaskCheckInstant;
    }

    public void setActionableTaskCheckInstant(Instant actionableTaskCheckInstant) {
        this.actionableTaskCheckInstant = actionableTaskCheckInstant;
    }

    public Instant getTaskJobCardCheckInstant() {
        return taskJobCardCheckInstant;
    }

    public void setTaskJobCardCheckInstant(Instant taskJobCardCheckInstant) {
        this.taskJobCardCheckInstant = taskJobCardCheckInstant;
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    public PetasosLocalParticipantTaskCache getActionableTaskDM() {
        return actionableTaskDM;
    }

    public ParticipantSharedTaskJobCardCache getTaskJobCardDM() {
        return taskJobCardDM;
    }

    protected Logger getLogger(){
        return(LOG);
    }
}
