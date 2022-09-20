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

import net.fhirfactory.pegacorn.core.interfaces.tasks.PetasosTaskBrokerInterface;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.ParticipantSharedTaskJobCardCache;
import net.fhirfactory.dricats.petasos.tasking.orchestration.PetasosLocalParticipantTaskCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class GlobalPetasosTaskRecoveryWatchdog {

    private Instant taskRecoveryCheckInstant;
    private boolean initialised;

    @Inject
    private PetasosTaskBrokerInterface taskBroker;

    @Inject
    private PetasosLocalParticipantTaskCache actionableTaskDM;

    @Inject
    private ParticipantSharedTaskJobCardCache taskJobCardDM;

    //
    // Constructor(s)
    //

    public GlobalPetasosTaskRecoveryWatchdog(){
        this.taskRecoveryCheckInstant = Instant.EPOCH;
        this.initialised = false;
    }

    //
    // PostConstruct
    //

    @PostConstruct
    public void initialise(){

    }

    //
    // Scheduler
    //


    //
    // Petasos Task Recovery Watchdog
    //

    protected void petasosTaskRecoveryWatchdog(){

    }

    //
    // Getters and Setters
    //

    public Instant getTaskRecoveryCheckInstant() {
        return taskRecoveryCheckInstant;
    }

    public void setTaskRecoveryCheckInstant(Instant taskRecoveryCheckInstant) {
        this.taskRecoveryCheckInstant = taskRecoveryCheckInstant;
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    protected PetasosTaskBrokerInterface getTaskBroker() {
        return taskBroker;
    }

    protected PetasosLocalParticipantTaskCache getActionableTaskDM() {
        return actionableTaskDM;
    }

    protected ParticipantSharedTaskJobCardCache getTaskJobCardDM() {
        return taskJobCardDM;
    }
}
