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

import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosFulfillmentTask;
import net.fhirfactory.dricats.petasos.tasking.orchestration.helpers.ActionableTaskFulfillmentTaskTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PetasosLocalTaskOrchestrator {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosLocalTaskOrchestrator.class);

    private ConcurrentHashMap<String, PetasosFulfillmentTask> processedTasks;

    private boolean initialised;
    private boolean active;
    private Instant startupTime;
    private Instant lastExecutionTime;

    private static final Long ORCHESTRATOR_STARTUP_DELAY = 30000L;
    private static final Long ORCHESTRATOR_ACTIVITY_PERIOD = 15000L;

    @Inject
    private PetasosLocalParticipantTaskCache localParticipantTaskCache;

    @Inject
    private ActionableTaskFulfillmentTaskTransformer taskTransformer;


    //
    // Constructor(s)
    //

    public PetasosLocalTaskOrchestrator(){
        this.initialised = false;
        this.active = false;
        this.startupTime = Instant.now();
        this.lastExecutionTime = Instant.now();
    }

    //
    // Post Construct
    //

    //
    // Getters (and Setters)
    //

    protected PetasosLocalParticipantTaskCache getParticipantTaskCache(){
        return(this.localParticipantTaskCache);
    }

    protected ActionableTaskFulfillmentTaskTransformer getTaskTransformer(){
        return(taskTransformer);
    }

    protected Logger getLogger(){
        return(LOG);
    }

    protected boolean isInitialised() {
        return initialised;
    }

    protected void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    protected boolean isActive() {
        return active;
    }

    protected void setActive(boolean active) {
        this.active = active;
    }

    protected Instant getStartupTime() {
        return startupTime;
    }

    protected void setStartupTime(Instant startupTime) {
        this.startupTime = startupTime;
    }

    protected Instant getLastExecutionTime() {
        return lastExecutionTime;
    }

    protected void setLastExecutionTime(Instant lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    //
    // Business Methods (Interacting with Ponos)
    //

    public void announceTaskFulfillment(PetasosActionableTask actionableTask){

    }

    public void announceTaskCreation(PetasosActionableTask actionableTask){

    }

    public Instant queueTaskFulfillment(PetasosActionableTask actionableTask){

        return(Instant.now());
    }

    public Instant cancelTaskFulfillment(PetasosActionableTask actionableTask){

        return(Instant.now());
    }

    //
    // Business Methods (Orchestrating Fulfillment)
    //

    public void sendTaskToFulfiller(PetasosActionableTask task){

    }

    public void receiveTaskFromFulfiller(PetasosFulfillmentTask fulfillmentTask){

    }

    //
    // Core Task Orchestrator
    //

    protected void localTaskOrchestratorScheduler(){

    }

    protected void localTaskOrchestratorDaemon(){

        // Check for Tasks Completed
        { /* loop */
            // Check for Completed Task

            // Add to Local Cache (if appropriate)

            // Forward to Ponos

            // Clear our Local Cache(s)

        }

        // Check for Tasks to Route
        { /* loop */
            // Checking for new Tasks

            // Transform Task

            // Route Task

            // Add to "Active" Cache

        }
    }
}
