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
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.valuesets.*;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.factories.AuditEventEntityFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.auditevent.factories.AuditEventFactory;
import net.fhirfactory.dricats.petasos.tasking.audit.transformers.common.Pegacorn2FHIRAuditEventBase;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import org.apache.camel.CamelExecutionException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class Exception2FHIRAuditEvent  extends Pegacorn2FHIRAuditEventBase {
    private static final Logger LOG = LoggerFactory.getLogger(Exception2FHIRAuditEvent.class);

    private ObjectMapper jsonMapper;

    @Inject
    private AuditEventFactory auditEventFactory;

    @Inject
    private AuditEventEntityFactory auditEventEntityFactory;

    @Inject
    private ProcessingPlantInterface processingPlant;

    //
    // Constructor(s)
    //

    public Exception2FHIRAuditEvent(){
        jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    //
    // Business Methods
    //

    public AuditEvent transformCamelExecutionException(CamelExecutionException camelExecutionException, PetasosFulfillmentTask fulfillmentTask, UoW uow){
        AuditEvent event = transformCamelExecutionException(camelExecutionException);
        return(event);
    }

    public AuditEvent transformCamelExecutionException(CamelExecutionException camelExecutionException){

        String auditEventEntityName = "CamelExecutionException";

        List<AuditEvent.AuditEventEntityDetailComponent> detailList = new ArrayList<>();

        AuditEvent.AuditEventEntityComponent stackTrace = auditEventEntityFactory.newAuditEventEntity(AuditEventEntityTypeEnum.PEGACORN_PROCESSING_EXCEPTION,
                AuditEventEntityRoleEnum.HL7_JOB,
                AuditEventEntityLifecycleEnum.HL7_TRANSMIT,
                "CamelExecutionException",
                camelExecutionException.getMessage(),
                "Stack Trace",
                ExceptionUtils.getStackTrace(camelExecutionException));

        AuditEvent.AuditEventOutcome auditEventOutcome = AuditEvent.AuditEventOutcome._8;
        String outcomeString = auditEventOutcome.getDisplay();

        AuditEvent auditEvent = auditEventFactory.newAuditEvent(
                null,
                processingPlant.getSimpleInstanceName(),
                processingPlant.getHostName(),
                null,
                null,
                AuditEventSourceTypeEnum.HL7_APPLICATION_SERVER,
                AuditEventTypeEnum.DICOM_APPLICATION_ACTIVITY,
                AuditEventSubTypeEnum.DICOM_APPLICATION_LOCAL_SERVICE_OPERATION_STOPPED,
                AuditEvent.AuditEventAction.E,
                auditEventOutcome,
                outcomeString,
                aSimplePeriod(),
                stackTrace);

        return(auditEvent);

    }

    //
    // Helpers
    //

    private Period aSimplePeriod(){
        Period period = new Period();
        period.setStart(Date.from(Instant.now()));
        period.setEnd(Date.from(Instant.now()));
        return(period);
    }

    //
    // Getters (and Setters)
    //

    protected ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    protected AuditEventFactory getAuditEventFactory() {
        return auditEventFactory;
    }

    protected AuditEventEntityFactory getAuditEventEntityFactory() {
        return auditEventEntityFactory;
    }

    protected ProcessingPlantInterface getProcessingPlant() {
        return processingPlant;
    }
}
