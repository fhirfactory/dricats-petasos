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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventEntityLifecycleEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventEntityRoleEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventEntityTypeEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.AuditEventSourceTypeEnum;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.factories.AuditEventEntityFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.factories.AuditEventFactory;
import net.fhirfactory.dricats.petasos.tasking.audit.transformers.common.Pegacorn2FHIRAuditEventBase;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosFulfillmentTask2FHIRAuditEvent extends Pegacorn2FHIRAuditEventBase {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosFulfillmentTask2FHIRAuditEvent.class);

    ObjectMapper jsonMapper = null;

    @Inject
    private AuditEventFactory auditEventFactory;

    @Inject
    private AuditEventEntityFactory auditEventEntityFactory;

    @Inject
    private ProcessingPlantInterface processingPlant;

    public PetasosFulfillmentTask2FHIRAuditEvent(){
        this.jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }


    public AuditEvent transform(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask == null){
            return(null);
        }

        UoW uow = fulfillmentTask.getTaskWorkItem();
        String auditEventEntityName = extractAuditEventEntityNameFromTask(fulfillmentTask);

        String parcelAsString = null;

        try {
            parcelAsString = jsonMapper.writeValueAsString(fulfillmentTask);
        } catch (JsonProcessingException e) {
            LOG.error(".transform(): Cannot convert UoW to string!!!",e);
            return(null);
        }

        AuditEvent.AuditEventEntityComponent auditEventEntityComponent = auditEventEntityFactory.newAuditEventEntity(
                AuditEventEntityTypeEnum.HL7_SYSTEM_OBJECT,
                AuditEventEntityRoleEnum.HL7_JOB,
                AuditEventEntityLifecycleEnum.HL7_TRANSMIT,
                fulfillmentTask.getClass().getSimpleName(),
                fulfillmentTask.getClass().getSimpleName() + "(" + auditEventEntityName + " @ " + extractNiceNodeName(fulfillmentTask) + ")",
                fulfillmentTask.getClass().getName(),
                parcelAsString);


        AuditEvent auditEvent = auditEventFactory.newAuditEvent(
                null,
                processingPlant.getSimpleInstanceName(),
                processingPlant.getHostName(),
                fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentID().getDisplayName(),
                null,
                AuditEventSourceTypeEnum.HL7_APPLICATION_SERVER,
                extractAuditEventType(fulfillmentTask),
                extractAuditEventSubType(fulfillmentTask),
                AuditEvent.AuditEventAction.C,
                extractAuditEventOutcome(fulfillmentTask),
                null,
                extractProcessingPeriod(fulfillmentTask),
                auditEventEntityComponent);

        return(auditEvent);
    }
}
