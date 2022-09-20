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

import net.fhirfactory.dricats.model.transforms.tofhir.task.FHIRTaskFromPetasosActionableTask;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirfactory.dricats.interfaces.petasos.participant.processingplant.ProcessingPlantInterface;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.TaskContextType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.TaskWorkItem;
import net.fhirfactory.dricats.model.transforms.fromhl7v2x.FHIRTaskMetadataResourceSetFromHL7v2x;

@ApplicationScoped
public class FHIRResourceSetFromPetasosActionableTask {
    private static final Logger LOG = LoggerFactory.getLogger(FHIRResourceSetFromPetasosActionableTask.class);

    @Inject
    private FHIRProvenanceFromPetasosTaskJourney provenanceFromPetasosTaskJourney;

    @Inject
    private FHIRTaskFromPetasosActionableTask taskFromPetasosActionableTask;

    @Inject
    private ProcessingPlantInterface processingPlant;

    @Inject
    private FHIRTaskMetadataResourceSetFromHL7v2x metadataResourceSetFromHL7v2x;


    public List<Resource> transformTask(PetasosActionableTask actionableTask){
        getLogger().debug(".newTask(): Entry, actionableTask->{}", actionableTask);
        List<Resource> resourceList = new ArrayList<>();

        //
        // Build the FHIR::Task
        Task fhirTask = taskFromPetasosActionableTask.newTaskFromPetasosTask(actionableTask);
        resourceList.add(fhirTask);
        //
        // Now the FHIR::Provenance
        Provenance fhirProvenance = provenanceFromPetasosTaskJourney.newProvenanceFromTaskJourney(processingPlant.getComponentId(), fhirTask.getIdentifierFirstRep(), actionableTask.getTaskTraceability());
        resourceList.add(fhirProvenance);
        //
        // Now check to see if we need to build metadata set
        boolean hasBeneficiary = false;
        boolean hasEncounter = false;
        boolean hasCompleteTaskTrigger = false;
        TaskContextType taskContext = new TaskContextType();
        if(actionableTask.hasTaskContext()){
            if(actionableTask.getTaskContext().hasTaskBeneficiary()){
                hasBeneficiary = true;
                taskContext.setTaskBeneficiary(actionableTask.getTaskContext().getTaskBeneficiary());
            }
            if(actionableTask.getTaskContext().hasTaskEncounter()){
                hasEncounter = true;
                taskContext.setTaskEncounter(actionableTask.getTaskContext().getTaskEncounter());
            }
            if(actionableTask.getTaskContext().hasTaskTriggerSummary()){
                taskContext.setTaskTriggerSummary(actionableTask.getTaskContext().getTaskTriggerSummary());
                boolean hasSummaryType = actionableTask.getTaskContext().getTaskTriggerSummary().hasTriggerSummaryType();
                boolean hasTriggerName = actionableTask.getTaskContext().getTaskTriggerSummary().hasTriggerName();
                if(hasSummaryType && hasTriggerName){
                    hasCompleteTaskTrigger = true;
                }
            }
        }
        if(!hasBeneficiary || !hasEncounter || !hasCompleteTaskTrigger){
            if(actionableTask.hasTaskWorkItem()) {
                TaskWorkItem taskWorkItem = actionableTask.getTaskWorkItem();
                if(metadataResourceSetFromHL7v2x.containsHL7v2xMessage(taskWorkItem)) {
                    TaskContextType updatedTaskContext = metadataResourceSetFromHL7v2x.newTaskContextType(actionableTask);
                    actionableTask.setTaskContext(updatedTaskContext);
                    String hl7Message = metadataResourceSetFromHL7v2x.extractHL7Message(taskWorkItem);
                    List<Resource> metadataResourceList = metadataResourceSetFromHL7v2x.extractResourcesForTaskMetadata(hl7Message);
                    if(!hasBeneficiary){
                        Patient patientFromMetadataResourceList = metadataResourceSetFromHL7v2x.getPatientFromMetadataResourceList(metadataResourceList);
                        if(patientFromMetadataResourceList != null) {
                            Reference patientReference = new Reference();
                            patientReference.setIdentifier(patientFromMetadataResourceList.getIdentifierFirstRep());
                            patientReference.setType(ResourceType.Patient.name());
                            fhirTask.setFor(patientReference);
                            resourceList.add(patientFromMetadataResourceList);
                        }
                    }
                    if(!hasEncounter){
                        Encounter encounterFromMetadataResourceList = metadataResourceSetFromHL7v2x.getEncounterFromMetadataResourceList(metadataResourceList);
                        if(encounterFromMetadataResourceList != null) {
                            Reference encounterReference = new Reference();
                            encounterReference.setIdentifier(encounterFromMetadataResourceList.getIdentifierFirstRep());
                            encounterReference.setType(ResourceType.Encounter.name());
                            fhirTask.setEncounter(encounterReference);
                            resourceList.add(encounterFromMetadataResourceList);
                        }
                    }
                }
            }
        }
        //
        // And done...
        getLogger().debug(".newTask(): Exit, resourceList->{}", resourceList);
        return(resourceList);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
