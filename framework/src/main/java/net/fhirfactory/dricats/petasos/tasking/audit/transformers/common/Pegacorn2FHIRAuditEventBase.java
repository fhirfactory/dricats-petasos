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
package net.fhirfactory.dricats.petasos.tasking.audit.transformers.common;

import net.fhirfactory.pegacorn.core.model.componentid.PegacornSystemComponentTypeTypeEnum;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeFDN;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeRDN;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.topology.nodes.DefaultWorkshopSetEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventSubTypeEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventTypeEnum;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Period;

import java.util.Date;

public abstract class Pegacorn2FHIRAuditEventBase {

    protected String stripEscapeCharacters(String incomingString){
        String outgoingString0 = incomingString.replaceAll("\\\\", "");
        String outgoingString1 = outgoingString0.replaceAll("\\\"","\"");
        return(outgoingString1);
    }

    protected Period extractProcessingPeriod(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask == null){
            return(null);
        }
        Date startDate = null;
        Date endDate = null;
        if(fulfillmentTask.hasTaskFulfillment()) {
            if (fulfillmentTask.getTaskFulfillment().hasStartDate()) {
                startDate = fulfillmentTask.getTaskFulfillment().getStartDate();
            }
            if (fulfillmentTask.getTaskFulfillment().hasFinishedDate()) {
                endDate = fulfillmentTask.getTaskFulfillment().getFinishedDate();
            }
            if (fulfillmentTask.getTaskFulfillment().hasFinalisationDate()) {
                endDate = fulfillmentTask.getTaskFulfillment().getFinalisationDate();
            }
            if (fulfillmentTask.getTaskFulfillment().hasCancellationDate()) {
                if (endDate == null) {
                    endDate = fulfillmentTask.getTaskFulfillment().getCancellationDate();
                }
            }
        }
        if(startDate == null){
            startDate = endDate;
        }
        Period period = new Period();
        if(startDate != null) {
            period.setStart(startDate);
        }
        if(endDate != null) {
            period.setEnd(endDate);
        }
        return(period);
    }

    protected AuditEvent.AuditEventOutcome extractAuditEventOutcome(PetasosFulfillmentTask fulfillmentTask, UoW uow){
        if(fulfillmentTask == null){
            return(AuditEvent.AuditEventOutcome._8);
        }
        if(uow == null) {
            switch (fulfillmentTask.getTaskFulfillment().getStatus()) {
                case FULFILLMENT_EXECUTION_STATUS_UNREGISTERED:
                case FULFILLMENT_EXECUTION_STATUS_REGISTERED:
                case FULFILLMENT_EXECUTION_STATUS_ACTIVE:
                case FULFILLMENT_EXECUTION_STATUS_INITIATED:
                    return (null);
                case FULFILLMENT_EXECUTION_STATUS_FINALISED:
                case FULFILLMENT_EXECUTION_STATUS_FINISHED:
                    return (AuditEvent.AuditEventOutcome._0);
                case FULFILLMENT_EXECUTION_STATUS_CANCELLED:
                    return (AuditEvent.AuditEventOutcome._4);
                case FULFILLMENT_EXECUTION_STATUS_FAILED:
                default:
                    return (AuditEvent.AuditEventOutcome._8);
            }
        } else {
            if(uow.hasProcessingOutcome()) {
                switch (uow.getProcessingOutcome()) {
                    case UOW_OUTCOME_FAILED:
                        return (AuditEvent.AuditEventOutcome._8);
                    case UOW_OUTCOME_SUCCESS:
                        return (AuditEvent.AuditEventOutcome._0);
                    case UOW_OUTCOME_INCOMPLETE:
                    case UOW_OUTCOME_NOTSTARTED:
                    case UOW_OUTCOME_NO_PROCESSING_REQUIRED:
                    default:
                        return (null);
                }
            } else {
                return (AuditEvent.AuditEventOutcome._0);
            }
        }
    }

    protected AuditEvent.AuditEventOutcome extractAuditEventOutcome(PetasosFulfillmentTask fulfillmentTask){
        AuditEvent.AuditEventOutcome outcome = extractAuditEventOutcome(fulfillmentTask, null);
        return(outcome);
    }

    protected AuditEventTypeEnum extractAuditEventType(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask == null){
            return(AuditEventTypeEnum.DICOM_APPLICATION_ACTIVITY);
        }
        TopologyNodeFDN nodeFDN = fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentFDN();
        TopologyNodeRDN topologyNodeRDN = nodeFDN.extractRDNForNodeType(DricatsSoftwareComponentTypeEnum.WORKSHOP);
        if(topologyNodeRDN == null){
            return(AuditEventTypeEnum.DICOM_APPLICATION_ACTIVITY);
        }
        if(topologyNodeRDN.getNodeName().contentEquals(DefaultWorkshopSetEnum.TRANSFORM_WORKSHOP.getWorkshop())){
            return(AuditEventTypeEnum.HL7_TERMINOLOGY_TRANSFORM);
        } else {
            return(AuditEventTypeEnum.HL7_TERMINOLOGY_TRANSMIT);
        }
    }

    protected AuditEventSubTypeEnum extractAuditEventSubType(PetasosFulfillmentTask fulfillmentTask){
        AuditEventSubTypeEnum outcomeAuditEventSubType = extractAuditEventSubType(fulfillmentTask, false);
        return(outcomeAuditEventSubType);
    }

    protected AuditEventSubTypeEnum extractAuditEventSubType(PetasosFulfillmentTask fulfillmentTask, boolean isInteractDone){
        if(fulfillmentTask == null){
            return(AuditEventSubTypeEnum.DICOM_APPLICATION_LOCAL_SERVICE_OPERATION_STOPPED) ;
        }
        if(isInteractDone){
            return(AuditEventSubTypeEnum.DICOM_APPLICATION_LOCAL_SERVICE_OPERATION_STOPPED);
        }
        switch(fulfillmentTask.getTaskFulfillment().getStatus()) {
            case FULFILLMENT_EXECUTION_STATUS_REGISTERED:
            case FULFILLMENT_EXECUTION_STATUS_ACTIVE:
            case FULFILLMENT_EXECUTION_STATUS_UNREGISTERED:
                return(AuditEventSubTypeEnum.DICOM_APPLICATION_LOCAL_SERVICE_OPERATION_STARTED);
            case FULFILLMENT_EXECUTION_STATUS_FINISHED:
            case FULFILLMENT_EXECUTION_STATUS_FINALISED:
            default:
                return(AuditEventSubTypeEnum.DICOM_APPLICATION_LOCAL_SERVICE_OPERATION_STOPPED);
        }
    }

    protected String extractAuditEventEntityNameFromTask(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask == null){
            return(null);
        }
        UoW uow = fulfillmentTask.getTaskWorkItem();
        if(uow == null){
            return(null);
        }
        DataParcelManifest dataParcelManifest = null;
        switch(fulfillmentTask.getTaskFulfillment().getStatus()) {
            case FULFILLMENT_EXECUTION_STATUS_UNREGISTERED:
            case FULFILLMENT_EXECUTION_STATUS_REGISTERED:
            case FULFILLMENT_EXECUTION_STATUS_ACTIVE:
            case FULFILLMENT_EXECUTION_STATUS_INITIATED:
            case FULFILLMENT_EXECUTION_STATUS_FAILED:
            case FULFILLMENT_EXECUTION_STATUS_CANCELLED:
            case FULFILLMENT_EXECUTION_STATUS_NO_ACTION_REQUIRED:{
                if (uow.hasIngresContent()) {
                    dataParcelManifest = uow.getIngresContent().getPayloadManifest();
                }
                break;
            }
            case FULFILLMENT_EXECUTION_STATUS_FINISHED:
            case FULFILLMENT_EXECUTION_STATUS_FINALISED:{
                if (uow.hasEgressContent()) {
                    if (!uow.getEgressContent().getPayloadElements().isEmpty()) {
                        dataParcelManifest = uow.getEgressContent().getPayloadElements().iterator().next().getPayloadManifest();
                    }
                }
                break;
            }
            default:
        }
        if(dataParcelManifest == null){
            return(null);
        }
        String bestValue = null;
        if(dataParcelManifest.hasContentDescriptor()){
            bestValue = extractBestDescriptorValue(dataParcelManifest.getContentDescriptor());
        }
        if(bestValue == null){
            if(dataParcelManifest.hasContainerDescriptor()){
                bestValue = extractBestDescriptorValue(dataParcelManifest.getContainerDescriptor());
            }
        }
        return(bestValue);
    }

    protected String extractBestDescriptorValue(DataParcelTypeDescriptor descriptor){
        if(descriptor == null){
            return(null);
        }
        String value = new String();
        if(descriptor.hasDataParcelResource()){
            value = descriptor.getDataParcelResource();
        }
        if(descriptor.hasDataParcelSubCategory()){
            value = descriptor.getDataParcelSubCategory() + "." + value;
        }
        if(descriptor.hasDataParcelCategory()){
            value = descriptor.getDataParcelCategory() + "." + value;
        }
        if(descriptor.hasDataParcelDefiner()){
            value = descriptor.getDataParcelDefiner() + "." + value;
        }
        if(descriptor.hasVersion()){
            value = value + "(" + descriptor.getVersion() + ")";
        }
        return(value);
    }

    protected String extractNiceNodeName(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask == null){
            return(null);
        }
        TopologyNodeFDN wupFDN = fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentFDN();
        TopologyNodeRDN processingPlantRDN = wupFDN.extractRDNForNodeType(DricatsSoftwareComponentTypeEnum.PROCESSING_PLANT);
        TopologyNodeRDN workshopRDN = wupFDN.extractRDNForNodeType(DricatsSoftwareComponentTypeEnum.WORKSHOP);
        TopologyNodeRDN wupRDN = wupFDN.extractRDNForNodeType(DricatsSoftwareComponentTypeEnum.WUP);

        String name = new String();
        if(processingPlantRDN != null){
            name = processingPlantRDN.getNodeName() + ".";
        }
        if(workshopRDN != null){
            name = name + workshopRDN.getNodeName() + ".";
        }
        if(wupRDN != null){
            name = name + wupRDN.getNodeName();
        }
        return(name);
    }

}
