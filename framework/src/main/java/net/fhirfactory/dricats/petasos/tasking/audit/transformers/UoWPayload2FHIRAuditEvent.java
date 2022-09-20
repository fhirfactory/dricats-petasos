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
package net.fhirfactory.dricats.petasos.tasking.audit.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventEntityLifecycleEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventEntityRoleEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventEntityTypeEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventSourceTypeEnum;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.componentid.PegacornSystemComponentTypeTypeEnum;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.mllp.MLLPServerEndpoint;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.factories.AuditEventEntityFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.factories.AuditEventFactory;
import net.fhirfactory.dricats.petasos.tasking.audit.transformers.common.Pegacorn2FHIRAuditEventBase;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UoWPayload2FHIRAuditEvent extends Pegacorn2FHIRAuditEventBase {
    private static final Logger LOG = LoggerFactory.getLogger(UoWPayload2FHIRAuditEvent.class);

    ObjectMapper jsonMapper = null;

    @Inject
    private AuditEventFactory auditEventFactory;

    @Inject
    private AuditEventEntityFactory auditEventEntityFactory;

    @Inject
    private ProcessingPlantInterface processingPlant;

    public UoWPayload2FHIRAuditEvent(){
        jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
//        jsonMapper = new ObjectMapper();
    }

    public AuditEvent transform(PetasosFulfillmentTask fulfillmentTask){
        AuditEvent outcomeAuditEvent = transform(fulfillmentTask, false);
        return(outcomeAuditEvent);
    }

    public AuditEvent transform(PetasosFulfillmentTask fulfillmentTask, boolean isInteractDone){
        AuditEvent outcomeAuditEvent = transform(fulfillmentTask, "false", false);
        return(outcomeAuditEvent);
    }

    public AuditEvent transform(PetasosFulfillmentTask fulfillmentTask, String filteredState, boolean isInteractDone){
        if(fulfillmentTask == null){
            return(null);
        }

        String auditEventEntityName = extractAuditEventEntityNameFromTask(fulfillmentTask);

        List<AuditEvent.AuditEventEntityDetailComponent> detailList = new ArrayList<>();
        AuditEvent.AuditEventEntityDetailComponent ingresDetailComponent = auditEventEntityFactory.newAuditEventEntityDetailComponent("UoW.Ingress.Payload", fulfillmentTask.getTaskWorkItem().getIngresContent().getPayload());
        detailList.add(ingresDetailComponent);
        if(StringUtils.isNotEmpty(filteredState)){
            if(filteredState.equalsIgnoreCase("false")) {
                if (fulfillmentTask.getTaskWorkItem().hasEgressContent()) {
                    int counter = 0;
                    for (UoWPayload currentPayload : fulfillmentTask.getTaskWorkItem().getEgressContent().getPayloadElements()) {
                        AuditEvent.AuditEventEntityDetailComponent currentEgressDetail = auditEventEntityFactory.newAuditEventEntityDetailComponent("UoW.Egress.Payload[" + counter + "]", currentPayload.getPayload());
                        detailList.add(currentEgressDetail);
                        counter += 1;
                    }
                }
            }
        } else {
            AuditEvent.AuditEventEntityDetailComponent currentEgressDetail = auditEventEntityFactory.newAuditEventEntityDetailComponent("UoW.Egress.Payload[0]", "Content Not Sent (Filtered)");
            detailList.add(currentEgressDetail);
        }

        String descriptionText = getDescription(fulfillmentTask);

        if(StringUtils.isEmpty(descriptionText)){
            descriptionText = "Ingres and Egress content fromm UoW (Unit of Work) Processors";
        }

        AuditEvent.AuditEventEntityComponent auditEventEntityComponent = auditEventEntityFactory.newAuditEventEntity(
                AuditEventEntityTypeEnum.PEGACORN_MLLP_MSG,
                AuditEventEntityRoleEnum.HL7_JOB,
                AuditEventEntityLifecycleEnum.HL7_TRANSMIT,
                auditEventEntityName,
                descriptionText,
                detailList);

        String sourceSite = getSourceSite(fulfillmentTask);

        AuditEvent.AuditEventOutcome auditEventOutcome = extractAuditEventOutcome(fulfillmentTask);
        String outcomeString = getOutcomeDescription(auditEventOutcome, fulfillmentTask.getTaskWorkItem());

        AuditEvent auditEvent = auditEventFactory.newAuditEvent(
                null,
                processingPlant.getSimpleInstanceName(),
                processingPlant.getHostName(),
                sourceSite,
                null,
                AuditEventSourceTypeEnum.HL7_APPLICATION_SERVER,
                extractAuditEventType(fulfillmentTask),
                extractAuditEventSubType(fulfillmentTask, isInteractDone),
                AuditEvent.AuditEventAction.C,
                auditEventOutcome,
                outcomeString,
                extractProcessingPeriod(fulfillmentTask),
                auditEventEntityComponent);

        return(auditEvent);
    }

    protected String getDescription(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask == null){
            return(null);
        }
        if(fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentType().equals(DricatsSoftwareComponentTypeEnum.WUP)) {
            WorkUnitProcessorSoftwareComponent wup = (WorkUnitProcessorSoftwareComponent) fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor();
            switch (wup.getComponentSystemRole()) {
                case COMPONENT_ROLE_INTERACT_INGRES: {
                    if (wup.getIngresEndpoint() != null) {
                        switch (wup.getIngresEndpoint().getEndpointType()) {
                            case MLLP_SERVER: {
                                String descriptionText = "Interact.Ingres: MLLP Message Reception";
                                return (descriptionText);
                            }
                            default: {
                                return (null);
                            }
                        }
                    }
                }
                case COMPONENT_ROLE_INTERACT_EGRESS: {
                    if (wup.getEgressEndpoint() != null) {
                        switch (wup.getEgressEndpoint().getEndpointType()) {
                            case MLLP_CLIENT: {
                                String descriptionText = "Interact.Egress: MLLP Message Forwarding";
                                return (descriptionText);
                            }
                            default: {
                                return (null);
                            }
                        }
                    }
                }
                case COMPONENT_ROLE_SUBSYSTEM_TASK_DISTRIBUTION:
                case COMPONENT_ROLE_SUBSYSTEM_INTERNAL:
                case COMPONENT_ROLE_SUBSYSTEM_EDGE:
                default: {
                    return (null);
                }
            }
        } else {
            return(null);
        }
    }

    protected String getSourceSite(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask == null){
            return(null);
        }
        if(fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentType().equals(DricatsSoftwareComponentTypeEnum.WUP)) {
            WorkUnitProcessorSoftwareComponent wup = (WorkUnitProcessorSoftwareComponent) fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor();
            switch (wup.getComponentSystemRole()) {
                case COMPONENT_ROLE_INTERACT_INGRES: {
                    if (wup.getIngresEndpoint() != null) {
                        switch (wup.getIngresEndpoint().getEndpointType()) {
                            case MLLP_SERVER: {
                                MLLPServerEndpoint mllpServerEndpoint = (MLLPServerEndpoint) wup.getIngresEndpoint();
                                String source = processingPlant.getSubsystemParticipantName();
                                String port = mllpServerEndpoint.getMLLPServerAdapter().getPortNumber().toString();
                                String sourceSite = source + ":" + port;
                                return(sourceSite);
                            }
                            default: {
                                return (null);
                            }
                        }
                    }
                }
                case COMPONENT_ROLE_INTERACT_EGRESS:
                case COMPONENT_ROLE_SUBSYSTEM_TASK_DISTRIBUTION:
                case COMPONENT_ROLE_SUBSYSTEM_INTERNAL:
                case COMPONENT_ROLE_SUBSYSTEM_EDGE:
                default: {
                    return (null);
                }
            }
        } else {
            return(null);
        }
    }

    protected String getOutcomeDescription(AuditEvent.AuditEventOutcome outcome, UoW uow){
        if(outcome == null){
            return(null);
        }
        String addedText = null;
        if(uow != null){
            if(uow.hasFailureDescription()){
                addedText = uow.getFailureDescription();
            }
        }
        String outcomeString = outcome.getDisplay();
        if(StringUtils.isNotEmpty(addedText)){
            outcomeString = outcomeString + " (" + addedText + ")";
        }
        return(outcomeString);
    }
}
