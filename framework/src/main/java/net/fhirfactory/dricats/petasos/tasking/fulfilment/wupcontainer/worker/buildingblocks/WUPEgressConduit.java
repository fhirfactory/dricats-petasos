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

package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks;

import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeFunctionFDNToken;
import net.fhirfactory.pegacorn.core.model.petasos.participant.ProcessingPlantPetasosParticipantNameHolder;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.fulfillment.valuesets.FulfillmentExecutionStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.PetasosTaskExecutionStatusEnum;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.naming.PetasosPathwayExchangePropertyNames;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

/**
 * @author Mark A. Hunter
 * @since 2020-07-05
 */
@ApplicationScoped
public class WUPEgressConduit {
    private static final Logger LOG = LoggerFactory.getLogger(WUPEgressConduit.class);

    @Inject
    PetasosPathwayExchangePropertyNames exchangePropertyNames;

    @Inject
    private ProcessingPlantPetasosParticipantNameHolder participantNameHolder;

    //
    // Constructor(s)
    //

    public WUPEgressConduit() {

    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Logic
    //

    /**
     * This function reconstitutes the WorkUnitTransportPacket by extracting the WUPJobCard and ParcelStatusElement
     * from the Camel Exchange, and injecting them plus the UoW into it.
     *
     * @param incomingUoW   The Unit of Work (UoW) received as output from the actual Work Unit Processor (Business Logic)
     * @param camelExchange The Apache Camel Exchange object, for extracting the WUPJobCard & ParcelStatusElement from
     * @return A WorkUnitTransportPacket object for relay to the other
     */
    public PetasosFulfillmentTaskSharedInstance receiveFromWUP(UoW incomingUoW, Exchange camelExchange) {
        getLogger().debug(".receiveFromWUP(): Entry, incomingUoW->{}", incomingUoW);
        // Get my PetasosFulfillmentTask
        PetasosFulfillmentTaskSharedInstance fulfillmentTask = camelExchange.getProperty(PetasosPropertyConstants.WUP_PETASOS_FULFILLMENT_TASK_EXCHANGE_PROPERTY, PetasosFulfillmentTaskSharedInstance.class);
        TopologyNodeFunctionFDNToken wupFunctionToken = fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getNodeFunctionFDN().getFunctionToken();
        getLogger().trace(".receiveFromWUP(): wupFunctionToken (NodeElementFunctionToken) for this activity --> {}", wupFunctionToken);
        //
        // Now, continue with business logic
        WUPComponentNames elementNames = new WUPComponentNames(wupFunctionToken);
        //
        // Now process incoming content
        getLogger().trace(".receiveFromWUP(): We only want to check if the UoW was successful and modify the JobCard/StatusElement accordingly.");
        getLogger().trace(".receiveFromWUP(): All detailed checking of the Cluster/SiteWide details is done in the WUPContainerEgressProcessor");
        //
        // Ensure assignment of the ParticipantName
        if(incomingUoW.hasEgressContent()){
            for(UoWPayload currentEgressPayload: incomingUoW.getEgressContent().getPayloadElements()){
                if(currentEgressPayload.getPayloadManifest() != null){
                    if(!currentEgressPayload.getPayloadManifest().hasSourceProcessingPlantParticipantName()){
                        currentEgressPayload.getPayloadManifest().setSourceProcessingPlantParticipantName(participantNameHolder.getSubsystemParticipantName());
                    }
                }
            }
        }
        //
        // Merge the content
        fulfillmentTask.getTaskWorkItem().setProcessingOutcome(incomingUoW.getProcessingOutcome());
        if(incomingUoW.hasFailureDescription()) {
            fulfillmentTask.getTaskWorkItem().setFailureDescription(incomingUoW.getFailureDescription());
        }
        fulfillmentTask.getTaskWorkItem().setEgressContent(incomingUoW.getEgressContent());
        switch (incomingUoW.getProcessingOutcome()) {
            case UOW_OUTCOME_SUCCESS:
            case UOW_OUTCOME_SOFTFAILURE:
                getLogger().trace(".receiveFromWUP(): UoW was processed successfully - updating JobCard/StatusElement to FINISHED!");
                fulfillmentTask.setExecutionStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_FINISHED);
                fulfillmentTask.getTaskFulfillment().setStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_FINISHED);
                fulfillmentTask.getTaskFulfillment().setFinishInstant(Instant.now());
                break;
            case UOW_OUTCOME_NO_PROCESSING_REQUIRED:
                getLogger().trace(".receiveFromWUP(): UoW was processed with no actions required - updating JobCard/StatusElement to FINISHED!");
                fulfillmentTask.setExecutionStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_FINISHED);
                fulfillmentTask.getTaskFulfillment().setStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_NO_ACTION_REQUIRED);
                fulfillmentTask.getTaskFulfillment().setFinishInstant(Instant.now());
                break;
            case UOW_OUTCOME_DISCARD:
            case UOW_OUTCOME_FILTERED:
                getLogger().trace(".receiveFromWUP(): UoW was processed with task to be discarded/filtered!");
                fulfillmentTask.setExecutionStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_FINISHED);
                fulfillmentTask.getTaskFulfillment().setStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_FINISHED);
                fulfillmentTask.getTaskFulfillment().setFinishInstant(Instant.now());
                break;
            case UOW_OUTCOME_NOTSTARTED:
            case UOW_OUTCOME_INCOMPLETE:
            case UOW_OUTCOME_FAILED:
            default:
                getLogger().trace(".receiveFromWUP(): UoW was not processed or processing failed - updating JobCard/StatusElement to FAILED!");
                fulfillmentTask.setExecutionStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_FAILED);
                fulfillmentTask.getTaskFulfillment().setStatus(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_FAILED);
                fulfillmentTask.getTaskFulfillment().setFinishInstant(Instant.now());
                break;
        }

        fulfillmentTask.update();

        if(getLogger().isDebugEnabled()){
            getLogger().debug(".receiveFromWUP(): Entry, fulfillmentTaskId/ActionableTaskId->{}/{}: Status->{}", fulfillmentTask.getTaskId(), fulfillmentTask.getActionableTaskId(),fulfillmentTask.getTaskFulfillment().getStatus() );
        }

        getLogger().trace(".receiveFromWUP(): fulfillmentTask->{}", fulfillmentTask);
        return (fulfillmentTask);
    }
}
