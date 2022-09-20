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
package net.fhirfactory.dricats.model.transforms.tofhir.task;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.fhirfactory.dricats.model.transforms.tofhir.task.factories.TaskIdentifierFactory;
import net.fhirfactory.dricats.model.transforms.tofhir.task.factories.TaskWorkItemFactory;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.tasktype.valuesets.TaskTypeTypeEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.UoWPayload;

public abstract class FHIRTaskFromPetasosTask {

    ObjectMapper jsonMapper = new ObjectMapper();

    @Inject
    private TaskIdentifierFactory taskIdentifierFactory;

    @Inject
    private TaskWorkItemFactory workItemFactory;

    public FHIRTaskFromPetasosTask(){
        jsonMapper = new ObjectMapper();
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    //
    // Abstract Methods
    //

    abstract protected Logger getLogger();
    abstract protected Reference specifyBasedOn(PetasosTask petasosTask);
    abstract protected Identifier specifyGroupIdentifier(PetasosTask petasosTask);
    abstract protected List<Reference> specifyPartOf(PetasosTask petasosTask);
    abstract protected Task.TaskStatus specifyStatus(PetasosTask petasosTask);
    abstract protected CodeableConcept specifyStatusReason(PetasosTask petasosTask);
    abstract protected CodeableConcept specifyBusinessStatus(PetasosTask petasosTask);
    abstract protected Task.TaskIntent specifyIntent(PetasosTask petasosTask);
    abstract protected Task.TaskPriority specifyPriority(PetasosTask petasosTask);
    abstract protected CodeableConcept specifyCode(PetasosTask petasosTask);
    abstract protected String specifyDescription(PetasosTask petasosTask);
    abstract protected Reference specifyFocus(PetasosTask petasosTask);
    abstract protected Reference specifyFor(PetasosTask petasosTask);
    abstract protected Reference specifyEncounter(PetasosTask petasosTask);
    abstract protected Period specifyExecutionPeriod(PetasosTask petasosTask);
    abstract protected Period specifyExecutionPeriod(PetasosTask petasosTask, Set<PetasosTask> subTaskSet);
    abstract protected Reference specifyRequester(PetasosTask petasosTask);
    abstract protected List<CodeableConcept> specifyPerformerType(PetasosTask petasosTask);
    abstract protected Reference specifyOwner(PetasosTask petasosTask);
    abstract protected CodeableConcept specifyReasonCode(PetasosTask petasosTask);
    abstract protected Reference specifyReasonReference(PetasosTask petasosTask);

    //
    // Business Methods
    //

    public Task newTaskFromPetasosTask(PetasosTask petasosTask){
        getLogger().debug(".newTask(): Entry, actionableTask->{}", petasosTask);
        Task fhirTask = new Task();

        //
        // Create and add the Task::Identifier
        Identifier identifier = getTaskIdentifierFactory().newTaskIdentifier(TaskTypeTypeEnum.PETASOS_ACTIONABLE_TASK_TYPE, petasosTask.getTaskId());
        fhirTask.addIdentifier(identifier);

        //
        // Set the "BasedOn" Attribute
        Reference basedOnReference = specifyBasedOn(petasosTask);
        if(basedOnReference != null) {
            fhirTask.addBasedOn(basedOnReference);
        }

        //
        // Set the Group Identifier
        Identifier groupIdentifier = specifyGroupIdentifier(petasosTask);
        if(groupIdentifier != null){
            fhirTask.setGroupIdentifier(groupIdentifier);
        }

        //
        // Set the "Part Of" Attribute
        List<Reference> partOfReference = specifyPartOf(petasosTask);
        if(partOfReference != null){
            fhirTask.getPartOf().addAll(partOfReference);
        }

        //
        // Set the status
        fhirTask.setStatus(specifyStatus(petasosTask));

        //
        // Set the Status Reason
        CodeableConcept statusReasonCC = specifyStatusReason(petasosTask);
        if(statusReasonCC != null) {
            fhirTask.setStatusReason(statusReasonCC);
        }

        //
        // Set the Business Status
        CodeableConcept businessStatusCC = specifyBusinessStatus(petasosTask);
        if(businessStatusCC != null){
            fhirTask.setBusinessStatus(businessStatusCC);
        }

        //
        // Set the Task "Intent"
        Task.TaskIntent taskIntent = specifyIntent(petasosTask);
        if(taskIntent != null){
            fhirTask.setIntent(taskIntent);
        }

        //
        // Set the Task "Priority"
        Task.TaskPriority taskPriority = specifyPriority(petasosTask);
        if(taskPriority != null){
            fhirTask.setPriority(taskPriority);
        }

        //
        // Set the Task "Code"
        CodeableConcept taskCodeCC = specifyCode(petasosTask);
        if(taskCodeCC != null){
            fhirTask.setCode(taskCodeCC);
        }

        //
        // Set the Task Description
        String taskDescription = specifyDescription(petasosTask);
        if(StringUtils.isNotBlank(taskDescription)){
            fhirTask.setDescription(taskDescription);
        }

        //
        // Set the Task "Focus"
        Reference taskFocus = specifyFocus(petasosTask);
        if(taskFocus != null){
            fhirTask.setFocus(taskFocus);
        }

        //
        // Set the Task "For" (Beneficiary)
        Reference taskFor = specifyFor(petasosTask);
        if(taskFor != null){
            fhirTask.setFor(taskFor);
        }

        //
        // Set the Task Encounter
        Reference taskEncounter = specifyEncounter(petasosTask);
        if(taskEncounter != null){
            fhirTask.setEncounter(taskEncounter);
        }

        //
        // Set the Execution Period
        Period taskExecutionPeriod = specifyExecutionPeriod(petasosTask);
        if(taskExecutionPeriod != null){
            fhirTask.setExecutionPeriod(taskExecutionPeriod);
        }

        //
        // Set the creation (Authored On) date
        Instant creationInstant = petasosTask.getCreationInstant();
        Date creationDate = Date.from(creationInstant);
        fhirTask.setAuthoredOn(creationDate);

        //
        // Set the last update (Last Update On) date
        Instant lastModifiedInstant = petasosTask.getUpdateInstant();
        Date updateDate = Date.from(lastModifiedInstant);
        fhirTask.setLastModified(creationDate);

        //
        // Set the Requester
        Reference taskRequester = specifyRequester(petasosTask);
        if(taskRequester != null){
            fhirTask.setRequester(taskRequester);
        }

        //
        // Set the PerformerType
        List<CodeableConcept> taskPerformerCC = specifyPerformerType(petasosTask);
        if(taskPerformerCC != null){
            fhirTask.getPerformerType().addAll(taskPerformerCC);
        }

        //
        // Set the Owner
        Reference taskOwner = specifyOwner(petasosTask);
        if(taskOwner != null){
            fhirTask.setOwner(taskOwner);
        }

        //
        // Set the Task Reason
        CodeableConcept taskReasonCodeCC = specifyReasonCode(petasosTask);
        if(taskReasonCodeCC != null){
            fhirTask.setReasonCode(taskReasonCodeCC);
        }

        //
        // Add the Task Input
        if(petasosTask.getTaskWorkItem().getIngresContent() != null){
            Task.ParameterComponent taskInput = getWorkItemFactory().newWorkItemPayload(petasosTask.getTaskWorkItem().getIngresContent());
            fhirTask.addInput(taskInput);
        }

        //
        // Add the Task Output
        if(petasosTask.getTaskWorkItem().hasEgressContent()){
            if(!petasosTask.getTaskWorkItem().getEgressContent().getPayloadElements().isEmpty()){
                for(UoWPayload currentEgressPayload: petasosTask.getTaskWorkItem().getEgressContent().getPayloadElements()){
                    Task.ParameterComponent taskOutput = getWorkItemFactory().newWorkItemPayload(currentEgressPayload);
                    fhirTask.addInput(taskOutput);
                }
            }
        }

        getLogger().debug(".newTask(): Exit, fhirTask->{}", fhirTask);
        return(fhirTask);
    }

    //
    // Helper Method(s)
    //

    protected String mapDataParcelManifestToString(DataParcelManifest parcelManifest){
        try {
            String s = getJSONMapper().writeValueAsString(parcelManifest);
            return(s);
        } catch (JsonProcessingException e) {
            getLogger().error(".mapDataParcelManifestToString(): Error -->{}", ExceptionUtils.getStackTrace(e));
            return(null);
        }
    }

    //
    // Getters (and Setters)
    //

    protected TaskIdentifierFactory getTaskIdentifierFactory(){
        return(this.taskIdentifierFactory);
    }

    protected ObjectMapper getJSONMapper(){
        return(this.jsonMapper);
    }

    protected TaskWorkItemFactory getWorkItemFactory(){
        return(this.workItemFactory);
    }
}
