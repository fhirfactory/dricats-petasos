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
package net.fhirfactory.dricats.petasos.tasking.audit.brokers;

import net.fhirfactory.dricats.petasos.tasking.audit.transformers.Exception2FHIRAuditEvent;
import net.fhirfactory.dricats.petasos.tasking.audit.transformers.PetasosFulfillmentTask2FHIRAuditEvent;
import net.fhirfactory.dricats.petasos.tasking.audit.transformers.UoWPayload2FHIRAuditEvent;
import net.fhirfactory.pegacorn.core.interfaces.auditing.PetasosAuditEventGranularityLevelInterface;
import net.fhirfactory.pegacorn.core.interfaces.auditing.PetasosAuditEventServiceAgentInterface;
import net.fhirfactory.pegacorn.core.model.component.valuesets.SoftwareComponentConnectivityContextEnum;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.fulfillment.valuesets.FulfillmentExecutionStatusEnum;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosFulfillmentTaskAuditServicesBroker {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosFulfillmentTaskAuditServicesBroker.class);

    @Inject
    private PetasosAuditEventServiceAgentInterface auditWriter;

    @Inject
    private PetasosFulfillmentTask2FHIRAuditEvent parcefulfillmentTask2FHIRAuditEvent2auditevent;

    @Inject
    private UoWPayload2FHIRAuditEvent uow2auditevent;

    @Inject
    private Exception2FHIRAuditEvent exception2FHIRAuditEvent;

    @Inject
    private PetasosAuditEventGranularityLevelInterface auditEventGranularityLevel;

    //
    // Constructor(s)
    //


    //
    // Post Construct
    //


    //
    // Business Methods
    //

    public Boolean logActivity(PetasosFulfillmentTask fulfillmentTask) {
        getLogger().debug(".logActivity(): Entry, fulfillmentTask->{}",fulfillmentTask);
        boolean success = false;
        if(shouldLogAuditEventForTask(fulfillmentTask)) {
            success = logActivity(fulfillmentTask, false);
        }
        getLogger().debug(".logActivity(): Exit, success->{}",success);
        return(success);
    }

    public Boolean logActivity(PetasosFulfillmentTask fulfillmentTask, boolean requiresSynchronousWrite){
        getLogger().debug(".logActivity(): Entry, fulfillmentTask->{}, requiresSynchronousWrite->{}",fulfillmentTask, requiresSynchronousWrite);
        AuditEvent fulfillmentTaskAuditEntry = null;
        boolean success = false;
        if(shouldLogAuditEventForTask(fulfillmentTask)) {
            AuditEvent auditEntry = parcefulfillmentTask2FHIRAuditEvent2auditevent.transform(fulfillmentTask);
            success = auditWriter.captureAuditEvent(fulfillmentTaskAuditEntry, requiresSynchronousWrite);
        }
        getLogger().debug(".logActivity(): Exit, success->{}",success);
        return(success);
    }

    public void logMLLPTransactions(PetasosFulfillmentTask fulfillmentTask, String filteredState, boolean requiresSynchronousWrite){
        getLogger().debug(".logMLLPTransactions(): Entry, fulfillmentTask->{}, filteredState->{}, requiresSynchronousWrite->{}",fulfillmentTask, filteredState, requiresSynchronousWrite);
        boolean isInteractEgressActivity = false;
        boolean isInteractIngresActivity = false;
        getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] Start...");
        if(fulfillmentTask.hasTaskFulfillment()){
            getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] fulfillmentTask has TaskFulfillmentType element");
            if(fulfillmentTask.getTaskFulfillment().hasFulfillerWorkUnitProcessor()) {
                getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] fulfillmentTask.getTaskFulfillmentType has FulfillmentWorkUnitProcessor");
                WorkUnitProcessorSoftwareComponent wupSoftwareComponent = (WorkUnitProcessorSoftwareComponent)fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor();
                if(wupSoftwareComponent.getIngresEndpoint() != null){
                    getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] fulfillmentTask.getTaskFulfillmentType().getFulfillmentWorkUnitProcessor has ingresEndpoint");
                    if(wupSoftwareComponent.getIngresEndpoint().getComponentSystemRole() != null){
                        getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] fulfillmentTask.getTaskFulfillmentType().getFulfillmentWorkUnitProcessor().getIngresEndpoint has ComponentSystemRole");
                        if(wupSoftwareComponent.getIngresEndpoint().getComponentSystemRole().equals(SoftwareComponentConnectivityContextEnum.COMPONENT_ROLE_INTERACT_INGRES)){
                            isInteractIngresActivity = true;
                        }
                    }
                }
                if(wupSoftwareComponent.getEgressEndpoint() != null){
                    getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] fulfillmentTask.getTaskFulfillmentType().getFulfillmentWorkUnitProcessor has egressEndpoint");
                    if(wupSoftwareComponent.getEgressEndpoint().getComponentSystemRole() != null){
                        getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] fulfillmentTask.getTaskFulfillmentType().getFulfillmentWorkUnitProcessor().getEgressEndpoint has ComponentSystemRole");
                        if(wupSoftwareComponent.getEgressEndpoint().getComponentSystemRole().equals(SoftwareComponentConnectivityContextEnum.COMPONENT_ROLE_INTERACT_EGRESS)){
                            isInteractEgressActivity = true;
                        }
                    }
                }
            }
        }
        getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] isInteractEgressActivity->{}", isInteractEgressActivity);
        getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] isInteractIngresActivity->{}", isInteractIngresActivity);
        getLogger().debug(".logMLLPTransactions(): [Derive Endpoint Role] Finish...");

        if(!(isInteractEgressActivity || isInteractIngresActivity)){
            getLogger().debug(".logMLLPTransactions(): Not an endpoint (Ingres/Egress)!");
            return;
        }
        getLogger().debug(".logMLLPTransactions(): [Capture Audit Event] Start...");
        getLogger().debug(".logMLLPTransactions(): [Capture Audit Event][Converting from PetasosFulfillmentTask to AuditEvent] Start...");
        AuditEvent auditEvent = uow2auditevent.transform(fulfillmentTask, filteredState, true);
        getLogger().debug(".logMLLPTransactions(): [Capture Audit Event][Converting from PetasosFulfillmentTask to AuditEvent] Finish..., auditEvent->{}", auditEvent);
        getLogger().debug(".logMLLPTransactions(): [Capture Audit Event][Calling auditWriter service] Start...");
        Boolean success =  auditWriter.captureAuditEvent(auditEvent, requiresSynchronousWrite);
        getLogger().debug(".logMLLPTransactions(): [Capture Audit Event][Calling auditWriter service] Finish..., success->{}", success);
        getLogger().debug(".logMLLPTransactions(): [Capture Audit Event] Finish...");
    }

    public void logCamelExecutionException(Object object, Exchange camelExchange){
        getLogger().debug(".logCamelExecutionException(): Entry, object->{}",object);
        CamelExecutionException camelExecutionException = camelExchange.getProperty(Exchange.EXCEPTION_CAUGHT, CamelExecutionException.class);
        if(camelExecutionException != null) {
            AuditEvent auditEvent = exception2FHIRAuditEvent.transformCamelExecutionException(camelExecutionException);
            Boolean success =  auditWriter.captureAuditEvent(auditEvent, true);
        }
        getLogger().debug(".logCamelExecutionException(): Entry, object->{}",object);
    }

    protected boolean shouldLogAuditEventForTask(PetasosFulfillmentTask fulfillmentTask){
        if(fulfillmentTask.getTaskFulfillment().getStatus().equals(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_FAILED)) {
            return(true);
        }
        if(fulfillmentTask.getTaskFulfillment().getStatus().equals(FulfillmentExecutionStatusEnum.FULFILLMENT_EXECUTION_STATUS_REGISTERED)) {
            switch (auditEventGranularityLevel.getAuditEventGranularityLevel()) {
                case AUDIT_LEVEL_BLACK_BOX:
                case AUDIT_LEVEL_COARSE:
                    break;
                case AUDIT_LEVEL_FINE:
                case AUDIT_LEVEL_VERY_FINE:
                case AUDIT_LEVEL_EXTREME:
                    return(true);
            }
        }
        if(fulfillmentTask.hasTaskFulfillment()){
            switch(fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentSystemRole()){
                case COMPONENT_ROLE_INTERACT_EGRESS:
                case COMPONENT_ROLE_INTERACT_INGRES: {
                    switch (auditEventGranularityLevel.getAuditEventGranularityLevel()) {
                        case AUDIT_LEVEL_COARSE:
                        case AUDIT_LEVEL_FINE:
                        case AUDIT_LEVEL_VERY_FINE:
                        case AUDIT_LEVEL_EXTREME:
                        case AUDIT_LEVEL_BLACK_BOX:
                        default:
                            return (true);
                    }
                }
                case COMPONENT_ROLE_SUBSYSTEM_EDGE: {
                    switch (auditEventGranularityLevel.getAuditEventGranularityLevel()) {
                        case AUDIT_LEVEL_COARSE:
                        case AUDIT_LEVEL_FINE:
                        case AUDIT_LEVEL_VERY_FINE:
                        case AUDIT_LEVEL_EXTREME:
                            return (true);
                        case AUDIT_LEVEL_BLACK_BOX:
                        default:
                            return (false);
                    }
                }
                case COMPONENT_ROLE_SUBSYSTEM_TASK_DISTRIBUTION: {
                    switch (auditEventGranularityLevel.getAuditEventGranularityLevel()) {
                        case AUDIT_LEVEL_FINE:
                        case AUDIT_LEVEL_VERY_FINE:
                        case AUDIT_LEVEL_EXTREME:
                            return (true);
                        case AUDIT_LEVEL_BLACK_BOX:
                        case AUDIT_LEVEL_COARSE:
                        default:
                            return (false);
                    }
                }
                case COMPONENT_ROLE_SUBSYSTEM_INTERNAL: {
                    switch (auditEventGranularityLevel.getAuditEventGranularityLevel()) {
                        case AUDIT_LEVEL_VERY_FINE:
                        case AUDIT_LEVEL_EXTREME:
                            return (true);
                        case AUDIT_LEVEL_FINE:
                        case AUDIT_LEVEL_BLACK_BOX:
                        case AUDIT_LEVEL_COARSE:
                        default:
                            return (false);
                    }
                }
                default:{
                    switch (auditEventGranularityLevel.getAuditEventGranularityLevel()) {
                        case AUDIT_LEVEL_EXTREME:
                            return (true);
                        case AUDIT_LEVEL_FINE:
                        case AUDIT_LEVEL_VERY_FINE:
                        case AUDIT_LEVEL_BLACK_BOX:
                        case AUDIT_LEVEL_COARSE:
                        default:
                            return (false);
                    }
                }
            }
        }
        return(false);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
