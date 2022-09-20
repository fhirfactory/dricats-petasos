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
package net.fhirfactory.dricats.petasos.tasking.fulfilment.outcomes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.PetasosTaskExecutionStatusEnum;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.PetasosActionableTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.caches.shared.PetasosActionableTaskSharedInstanceAccessorFactory;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.oversight.LocalPetasosActionableTaskActivityController;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.LocalPetasosFulfilmentTaskActivityController;
import net.fhirfactory.dricats.petasos.tasking.observations.reporting.tasks.agents.WorkUnitProcessorTaskReportAgent;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskOutcomeCaptureBean {
    private static final Logger LOG = LoggerFactory.getLogger(TaskOutcomeCaptureBean.class);

    private ObjectMapper jsonMapper;

    @Inject
    private LocalPetasosFulfilmentTaskActivityController fulfilmentTaskActivityController;

    @Inject
    private LocalPetasosActionableTaskActivityController actionableTaskActivityController;

    @Inject
    private PetasosActionableTaskSharedInstanceAccessorFactory actionableTaskSharedInstanceAccessorFactory;

    //
    // Constructor
    //

    public TaskOutcomeCaptureBean() {
        jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);;
        JavaTimeModule module = new JavaTimeModule();
        jsonMapper.registerModule(module);
    }

    //
    // Post Construct
    //


    //
    // Business Logic
    //

    public PetasosActionableTaskSharedInstance captureAndRegisterOutcome(PetasosFulfillmentTaskSharedInstance fulfillmentTask, Exchange camelExchange){
        if(getLogger().isDebugEnabled()) {
            getLogger().debug(".captureAndRegisterOutcome(): Entry, fulfillmentTask->{}", convertToString(fulfillmentTask.getInstance()));
        }
        TaskIdType actionableTaskId = fulfillmentTask.getActionableTaskId();
        PetasosTaskExecutionStatusEnum petasosTaskExecutionStatus = null;
        switch(fulfillmentTask.getTaskFulfillment().getStatus()){
            case FULFILLMENT_EXECUTION_STATUS_UNREGISTERED:
            case FULFILLMENT_EXECUTION_STATUS_REGISTERED:
            case FULFILLMENT_EXECUTION_STATUS_INITIATED:
            case FULFILLMENT_EXECUTION_STATUS_ACTIVE_ELSEWHERE:
            case FULFILLMENT_EXECUTION_STATUS_FINISHED_ELSEWHERE:
            case FULFILLMENT_EXECUTION_STATUS_FINALISED_ELSEWHERE:
            case FULFILLMENT_EXECUTION_STATUS_FAILED:
                getLogger().debug(".captureAndRegisterOutcome(): fulfillmentTask.getTaskFulfillment().getStatus() Implies failure...");
                petasosTaskExecutionStatus = actionableTaskActivityController.notifyTaskFailure(actionableTaskId, fulfillmentTask.getInstance());
                break;
            case FULFILLMENT_EXECUTION_STATUS_CANCELLED:
                getLogger().debug(".captureAndRegisterOutcome(): fulfillmentTask.getTaskFulfillment().getStatus() Implies cancellation...");
                petasosTaskExecutionStatus = actionableTaskActivityController.notifyTaskCancellation(actionableTaskId, fulfillmentTask.getInstance());
                break;
            case FULFILLMENT_EXECUTION_STATUS_NO_ACTION_REQUIRED:
            case FULFILLMENT_EXECUTION_STATUS_FINISHED:
                getLogger().debug(".captureAndRegisterOutcome(): fulfillmentTask.getTaskFulfillment().getStatus() Implies (good) finish...");
                petasosTaskExecutionStatus = actionableTaskActivityController.notifyTaskFinish(actionableTaskId, fulfillmentTask.getInstance());
                break;
        }

        //
        // We can retire the FulfillmentTask now
        getFulfilmentTaskActivityController().deregisterFulfillmentTask(fulfillmentTask.getTaskId());

        //
        // Get the updated the ActionableTaskSharedInstance so we can do some metrics
        PetasosActionableTaskSharedInstance actionableTaskSharedInstance = getActionableTaskSharedInstanceFactory().getActionableTaskSharedInstance(actionableTaskId);

        //
        // Get out metricsAgent for the WUP that sent the task & do add some metrics
        WorkUnitProcessorTaskReportAgent taskReportAgent = camelExchange.getProperty(PetasosPropertyConstants.ENDPOINT_TASK_REPORT_AGENT_EXCHANGE_PROPERTY, WorkUnitProcessorTaskReportAgent.class);
        if(taskReportAgent != null){
            taskReportAgent.sendITOpsTaskReport(actionableTaskSharedInstance.getInstance());
        }

        if(getLogger().isDebugEnabled()) {
            getLogger().debug(".captureAndRegisterOutcome(): Exit, actionableTask->{}", convertToString(actionableTaskSharedInstance.getLocalInstance()));
        }
        return(actionableTaskSharedInstance);
    }



    protected String convertToString(PetasosTask petasosTask){
        try {
            String jsonString = jsonMapper.writeValueAsString(petasosTask);
            return(jsonString);
        } catch (JsonProcessingException e) {
            return(ExceptionUtils.getStackTrace(e));
        }
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected LocalPetasosFulfilmentTaskActivityController getFulfilmentTaskActivityController(){
        return(this.fulfilmentTaskActivityController);
    }

    protected LocalPetasosActionableTaskActivityController getActionableTaskActivityController(){
        return(this.actionableTaskActivityController);
    }

    protected PetasosActionableTaskSharedInstanceAccessorFactory getActionableTaskSharedInstanceFactory(){
        return(this.actionableTaskSharedInstanceAccessorFactory);
    }
}
