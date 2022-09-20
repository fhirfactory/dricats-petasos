/*
 * Copyright (c) 2021 Mark A. Hunter
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
package net.fhirfactory.dricats.model.transforms.tofhir;


import java.time.Instant;
import java.util.Date;
import java.util.Enumeration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.codesystems.ProvenanceAgentRole;
import org.hl7.fhir.r4.model.codesystems.ProvenanceAgentType;
import org.hl7.fhir.r4.model.codesystems.W3cProvenanceActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.internals.fhir.r4.codesystems.PegacornIdentifierCodeEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.identifier.PegacornIdentifierFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.provenance.factories.ProvenanceFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.provenance.factories.ProvenanceSequenceNumberExtensionFactory;
import net.fhirfactory.dricats.model.transforms.tofhir.task.factories.TaskIdentifierFactory;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.tasktype.valuesets.TaskTypeTypeEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.traceability.datatypes.TaskTraceabilityElementType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.traceability.datatypes.TaskTraceabilityType;

@ApplicationScoped
public class FHIRProvenanceFromPetasosTaskJourney {
    private static final Logger LOG = LoggerFactory.getLogger(FHIRProvenanceFromPetasosTaskJourney.class);

    @Inject
    private TaskIdentifierFactory taskIdentifierFactory;

    @Inject
    private ProvenanceSequenceNumberExtensionFactory sequenceNumberExtensionFactory;

    @Inject
    private PegacornIdentifierFactory identifierFactory;

    @Inject
    private ProvenanceFactory provenanceFactory;


    //
    // Business Methods
    //

    public Provenance newProvenanceFromTaskJourney(ComponentId agentId, Identifier taskIdentifier, TaskTraceabilityType taskJourney){
        if(taskIdentifier == null){
            return(null);
        }
        if(taskJourney == null){
            return(null);
        }
        if(taskJourney.getTaskJourney().isEmpty()){
            return(null);
        }

        //
        // Create Empty Provenance Resource/Object
        Provenance taskJourneyProvenance = new Provenance();

        //
        // Set Provenance Target (in this case, a Task)
        Reference targetReference = new Reference();
        targetReference.setIdentifier(taskIdentifier);
        targetReference.setReference(ResourceType.Task.name());
        taskJourneyProvenance.addTarget(targetReference);

        //
        // Set the Provenance Agent
        Provenance.ProvenanceAgentComponent provenanceAgent = newProvenanceAgentComponent(agentId, ProvenanceAgentRole.PERFORMER, ProvenanceAgentType.PERFORMER);
        taskJourneyProvenance.addAgent(provenanceAgent);

        //
        // Set the Provenance Recorded Instant
        taskJourneyProvenance.setRecorded(Date.from(Instant.now()));

        //
        // Set the Provenance Reason
        // TODO Add Provenance Reason

        //
        // Set the Provenance Activity
        CodeableConcept provenanceActivity = getProvenanceFactory().newProvenanceActivity(W3cProvenanceActivityType.GENERATION);
        taskJourneyProvenance.setActivity(provenanceActivity);

        //
        // Set the Provenance Entities
        Enumeration<Integer> sequenceNumberEnumerator = taskJourney.getTaskJourney().keys();
        while(sequenceNumberEnumerator.hasMoreElements()){
            Integer sequenceNumber = sequenceNumberEnumerator.nextElement();
            TaskTraceabilityElementType journeyElement = taskJourney.getTaskJourney().get(sequenceNumber);
            Identifier journeyElementIdentifier = getTaskIdentifierFactory().newTaskIdentifier(TaskTypeTypeEnum.PETASOS_ACTIONABLE_TASK_TYPE, journeyElement.getActionableTaskId());
            Reference journeyElementReference = new Reference();
            journeyElementReference.setIdentifier(journeyElementIdentifier);
            journeyElementReference.setType(ResourceType.Task.name());
            getSequenceNumberExtensionFactory().injectTargetSequenceNumber(journeyElementReference, sequenceNumber);
            Provenance.ProvenanceEntityComponent currentProvenanceEntity = new Provenance.ProvenanceEntityComponent();
            currentProvenanceEntity.setWhat(journeyElementReference);
            currentProvenanceEntity.setRole(Provenance.ProvenanceEntityRole.DERIVATION);
            ComponentId currentTaskFulfillerId = journeyElement.getFulfillerId();
            // Create the Provenance Agent (a Device --> SoftwareComponent) Identifier
            Provenance.ProvenanceAgentComponent currentProvenanceEntityAgent = newProvenanceAgentComponent(currentTaskFulfillerId, ProvenanceAgentRole.PERFORMER, ProvenanceAgentType.PERFORMER);
            currentProvenanceEntity.addAgent(currentProvenanceEntityAgent);
        }

        return(taskJourneyProvenance);
    }

    protected Provenance.ProvenanceAgentComponent newProvenanceAgentComponent(ComponentId id, ProvenanceAgentRole agentRole, ProvenanceAgentType agentType){
        Provenance.ProvenanceAgentComponent currentProvenanceAgent = new Provenance.ProvenanceAgentComponent();
        Period period = new Period();
        if(id.hasIdValidityStartInstant()){
            Date startDate = Date.from(id.getIdValidityStartInstant());
            period.setStart(startDate);
        }
        if(id.hasIdValidityEndInstant()){
            Date endDate = Date.from(id.getIdValidityEndInstant());
            period.setEnd(endDate);
        }
        Identifier deviceIdentifier = getIdentifierFactory().newIdentifier(PegacornIdentifierCodeEnum.IDENTIFIER_CODE_SOFTWARE_COMPONENT, id.getId(), period);
        Reference deviceReference = new Reference();
        deviceReference.setIdentifier(deviceIdentifier);
        deviceReference.setType(ResourceType.Device.name());
        currentProvenanceAgent.setWho(deviceReference);
        CodeableConcept currentAgentRole = getProvenanceFactory().newAgentRole(agentRole);
        CodeableConcept currentAgentType = getProvenanceFactory().newAgentType(agentType);
        currentProvenanceAgent.addRole(currentAgentRole);
        currentProvenanceAgent.setType(currentAgentType);
        return(currentProvenanceAgent);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected TaskIdentifierFactory getTaskIdentifierFactory(){
        return(this.taskIdentifierFactory);
    }

    protected ProvenanceSequenceNumberExtensionFactory getSequenceNumberExtensionFactory(){
        return(this.sequenceNumberExtensionFactory);
    }

    protected PegacornIdentifierFactory getIdentifierFactory(){
        return(this.identifierFactory);
    }

    protected ProvenanceFactory getProvenanceFactory(){
        return(this.provenanceFactory);
    }
}
