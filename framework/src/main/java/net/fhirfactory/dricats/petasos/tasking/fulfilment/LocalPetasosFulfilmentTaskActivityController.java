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

package net.fhirfactory.dricats.petasos.tasking.fulfilment;

import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.dricats.petasos.tasking.audit.brokers.PetasosFulfillmentTaskAuditServicesBroker;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstanceAccessorFactory;
import net.fhirfactory.dricats.petasos.tasking.caches.local.LocalFulfillmentTaskCache;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.pegacorn.core.model.petasos.wup.PetasosTaskJobCard;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.PetasosTaskExecutionStatusEnum;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.ParticipantSharedTaskJobCardCache;
import net.fhirfactory.dricats.petasos.tasking.factories.PetasosTaskJobCardFactory;
import net.fhirfactory.dricats.petasos.tasking.oversight.ParticipantTaskExecutionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

/**
 * @author Mark A. Hunter
 */
@ApplicationScoped
public class LocalPetasosFulfilmentTaskActivityController {
    private static final Logger LOG = LoggerFactory.getLogger(LocalPetasosFulfilmentTaskActivityController.class);

    @Inject
    private LocalFulfillmentTaskCache fulfillmentTaskDM;

    @Inject
    private ParticipantSharedTaskJobCardCache sharedTaskJobCardDM;

    @Inject
    private PetasosFulfillmentTaskAuditServicesBroker auditServicesBroker;

    @Inject
    private ProcessingPlantInterface processingPlant;

    @Inject
    private ParticipantTaskExecutionController taskExecutionController;

    @Inject
    private PetasosTaskJobCardFactory jobCardFactory;

    @Inject
    private PetasosFulfillmentTaskSharedInstanceAccessorFactory fulfillmentTaskSharedInstanceAccessorFactory;

    //
    // PetasosFulfillmentTask Registration
    //

    public PetasosFulfillmentTaskSharedInstance registerFulfillmentTask(PetasosFulfillmentTask task, boolean synchronousWriteToAudit){
        getLogger().debug(".registerFulfillmentTask(): Entry, fulfillmentTask->{}", task);
        PetasosTaskJobCard petasosTaskJobCard = null;
        if(task.hasTaskJobCard()) {
            petasosTaskJobCard = task.getTaskJobCard();
        } else {
            petasosTaskJobCard = jobCardFactory.newPetasosTaskJobCard(task);
            petasosTaskJobCard.setCurrentStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_WAITING);
            petasosTaskJobCard.setGrantedStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_WAITING);
            petasosTaskJobCard.setExecutingFulfillmentTaskId(null);
            petasosTaskJobCard.setLastRequestedStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_WAITING);
            petasosTaskJobCard.setExecutingFulfillmentTaskIdAssignmentInstant(null);
            task.setTaskJobCard(petasosTaskJobCard);
        }
        //
        // Register and Create a SharedInstance Accessor for the Fulfillment Task
        PetasosFulfillmentTaskSharedInstance fulfillmentTaskSharedInstance = getFulfillmentTaskSharedInstanceAccessorFactory().newFulfillmentTaskSharedAccessor(task);
        //
        // Create an audit event
        auditServicesBroker.logActivity(fulfillmentTaskSharedInstance.getInstance(), false);
        //
        // We're done
        getLogger().debug(".registerFulfillmentTask(): Exit, fulfillmentTaskSharedInstance->{}", fulfillmentTaskSharedInstance);
        return(fulfillmentTaskSharedInstance);
    }

    public void deregisterFulfillmentTask(PetasosFulfillmentTask task){
        getLogger().debug(".deregisterFulfillmentTask(): Entry, task->{}", task);
        fulfillmentTaskDM.removeTask(task.getTaskId());
        getLogger().debug(".deregisterFulfillmentTask(): Exit");
    }

    public void deregisterFulfillmentTask(TaskIdType taskId){
        fulfillmentTaskDM.removeTask(taskId);
    }

    //
    // Requests
    //

    public PetasosTaskExecutionStatusEnum requestFulfillmentTaskExecutionPrivilege(PetasosFulfillmentTaskSharedInstance fulfillmentTask){
        getLogger().trace(".requestFulfillmentTaskExecutionPrivilege(): Entry, fulfillmentTask->{}", fulfillmentTask);
        if(getLogger().isDebugEnabled()){
            getLogger().debug(".requestFulfillmentTaskExecutionPrivilege(): Entry, WUP Component Id->{}", fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentID());
        }
        PetasosTaskExecutionStatusEnum petasosTaskExecutionStatus = taskExecutionController.requestTaskExecutionPrivilege(fulfillmentTask.getInstance());
        fulfillmentTask.getTaskJobCard().setGrantedStatus(petasosTaskExecutionStatus);
        fulfillmentTask.update();
        getLogger().debug(".requestFulfillmentTaskExecutionPrivilege(): Exit, petasosTaskExecutionStatus->{}", petasosTaskExecutionStatus);
        return(petasosTaskExecutionStatus);
    }

    //
    // Notifications
    //

    public PetasosTaskExecutionStatusEnum notifyFulfillmentTaskExecutionStart(PetasosFulfillmentTaskSharedInstance fulfillmentTask){
        getLogger().debug(".notifyFulfillmentTaskExecutionStart(): Entry, fulfillmentTask->{}", fulfillmentTask);
        PetasosTaskExecutionStatusEnum petasosTaskExecutionStatus= taskExecutionController.reportTaskExecutionStart(fulfillmentTask.getInstance());
        fulfillmentTask.getTaskJobCard().setGrantedStatus(petasosTaskExecutionStatus);
        fulfillmentTask.update();
        //
        // Create an audit event
        auditServicesBroker.logActivity(fulfillmentTask.getInstance());
        getLogger().debug(".notifyFulfillmentTaskExecutionStart(): Exit, petasosTaskExecutionStatus->{}", petasosTaskExecutionStatus);
        return(petasosTaskExecutionStatus);
    }

    public PetasosTaskExecutionStatusEnum notifyFulfillmentTaskExecutionFinish(PetasosFulfillmentTaskSharedInstance fulfillmentTask){
        getLogger().debug(".notifyFulfillmentTaskExecutionFinish(): Entry, fulfillmentTask->{}", fulfillmentTask);
        PetasosTaskExecutionStatusEnum petasosTaskExecutionStatus= taskExecutionController.reportTaskExecutionFinish(fulfillmentTask.getInstance());
        fulfillmentTask.getTaskJobCard().setGrantedStatus(petasosTaskExecutionStatus);
        //
        // Create an audit event
        auditServicesBroker.logActivity(fulfillmentTask.getInstance(), false);
        getLogger().debug(".notifyFulfillmentTaskExecutionFinish(): Exit, petasosTaskExecutionStatus->{}", petasosTaskExecutionStatus);
        return(petasosTaskExecutionStatus);
    }

    public PetasosTaskExecutionStatusEnum notifyFulfillmentTaskExecutionFailure(PetasosFulfillmentTaskSharedInstance fulfillmentTask){
        getLogger().debug(".notifyFulfillmentTaskExecutionFailure(): Entry, fulfillmentTask->{}", fulfillmentTask);
        PetasosTaskExecutionStatusEnum petasosTaskExecutionStatus= taskExecutionController.reportTaskExecutionFailure(fulfillmentTask.getInstance());
        fulfillmentTask.getTaskJobCard().setGrantedStatus(petasosTaskExecutionStatus);
        //
        // Create an audit event
        auditServicesBroker.logActivity(fulfillmentTask.getInstance(), false);
        getLogger().debug(".notifyFulfillmentTaskExecutionFailure(): Exit, petasosTaskExecutionStatus->{}", petasosTaskExecutionStatus);
        return(petasosTaskExecutionStatus);
    }

    public PetasosTaskExecutionStatusEnum notifyFulfillmentTaskExecutionCancellation(PetasosFulfillmentTaskSharedInstance fulfillmentTask){
        getLogger().debug(".notifyFulfillmentTaskExecutionCancellation(): Entry, fulfillmentTask->{}", fulfillmentTask);
        PetasosTaskExecutionStatusEnum petasosTaskExecutionStatus= taskExecutionController.reportTaskCancellation(fulfillmentTask.getInstance());
        fulfillmentTask.getTaskJobCard().setGrantedStatus(petasosTaskExecutionStatus);
        //
        // Create an audit event
        auditServicesBroker.logActivity(fulfillmentTask.getInstance(), false);
        getLogger().debug(".notifyFulfillmentTaskExecutionCancellation(): Exit, petasosTaskExecutionStatus->{}", petasosTaskExecutionStatus);
        return(petasosTaskExecutionStatus);
    }

    public Instant notifyFulfillmentTaskExecutionNoActionRequired(PetasosFulfillmentTaskSharedInstance fulfillmentTask){

        Instant updateInstant = Instant.now();
        return(updateInstant);
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected PetasosFulfillmentTaskSharedInstanceAccessorFactory getFulfillmentTaskSharedInstanceAccessorFactory(){
        return(this.fulfillmentTaskSharedInstanceAccessorFactory);
    }
}
