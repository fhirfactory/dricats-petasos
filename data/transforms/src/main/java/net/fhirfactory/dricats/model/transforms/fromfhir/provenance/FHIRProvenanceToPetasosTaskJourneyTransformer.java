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
package net.fhirfactory.dricats.model.transforms.fromfhir.provenance;

import net.fhirfactory.dricats.internals.fhir.r4.resources.provenance.factories.ProvenanceSequenceNumberExtensionFactory;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.traceability.datatypes.TaskTraceabilityElementType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.traceability.datatypes.TaskTraceabilityType;

@ApplicationScoped
public class FHIRProvenanceToPetasosTaskJourneyTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(FHIRProvenanceToPetasosTaskJourneyTransformer.class);

    @Inject
    private ProvenanceSequenceNumberExtensionFactory sequenceNumberExtensionFactory;

    //
    // Business Logic
    //

    public TaskTraceabilityType newTaskTraceabilityFromFHIRProvenance(TaskIdType taskId, Provenance fhirProvenance){
        getLogger().debug(".newTaskTraceabilityFromFHIRProvenance(): Entry, fhirProvenance->{}", fhirProvenance);
        if(fhirProvenance == null) {
            getLogger().debug(".newTaskTraceabilityFromFHIRProvenance(): Exit, fhirProvenance is null");
            return (null);
        }
        boolean isRelevant = false;
        for(Reference currentTarget: fhirProvenance.getTarget()) {
            boolean sameValue = taskId.getPrimaryBusinessIdentifier().getValue().equals(currentTarget.getIdentifier().getValue());
            boolean sameSystem = taskId.getPrimaryBusinessIdentifier().getSystem().equals(currentTarget.getIdentifier().getSystem());
            if(sameSystem && sameValue){
                isRelevant = true;
            }
        }
        if(!isRelevant){
            getLogger().debug(".newTaskTraceabilityFromFHIRProvenance(): Exit, fhirProvenance is not relevant to task");
            return(null);
        }
        TaskTraceabilityType taskTraceability = new TaskTraceabilityType();
        for(Provenance.ProvenanceEntityComponent currentEntity: fhirProvenance.getEntity()){
            TaskTraceabilityElementType currentTraceabilityElement = new TaskTraceabilityElementType();
            Reference currentReference = currentEntity.getWhat();
            Integer traceabilitySequenceNo = sequenceNumberExtensionFactory.extractTargetSequenceNumber(currentReference);
            Provenance.ProvenanceAgentComponent currentAgent = currentEntity.getAgentFirstRep();
            if(traceabilitySequenceNo != null && currentAgent != null) {
                TaskIdType currentTaskId = new TaskIdType();
                currentTaskId.setLocalId(currentReference.getIdentifier().getValue());
                currentTaskId.setPrimaryBusinessIdentifier(currentReference.getIdentifier());
                currentTraceabilityElement.setActionableTaskId(currentTaskId);
                ComponentId currentComponentId = new ComponentId();
                currentComponentId.setId(currentAgent.getWho().getIdentifier().getValue());
                if(currentAgent.getWho().getIdentifier().hasPeriod()) {
                    Period identifierPeriod = currentAgent.getWho().getIdentifier().getPeriod();
                    if(identifierPeriod.hasStart()) {
                        currentComponentId.setIdValidityStartInstant(identifierPeriod.getStart().toInstant());
                    }
                    if(identifierPeriod.hasEnd()){
                        currentComponentId.setIdValidityEndInstant(identifierPeriod.getEnd().toInstant());
                    }
                }
                currentTraceabilityElement.setFulfillerId(currentComponentId);
                taskTraceability.getTaskJourney().put(traceabilitySequenceNo, currentTraceabilityElement);
            }
        }
        getLogger().debug(".newTaskTraceabilityFromFHIRProvenance(): Exit, taskTraceability->", taskTraceability);
        return(taskTraceability);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
