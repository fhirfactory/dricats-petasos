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
package net.fhirfactory.dricats.model.transforms.fromhl7v2x;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.HL7V2XTopicFactory;
import net.fhirfactory.dricats.model.dataparcel.DataDescriptor;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.TaskBeneficiaryType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.TaskContextType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.TaskEncounterType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.TaskTriggerSummaryType;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.valuesets.TaskBeneficiaryTypeTypeEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.context.valuesets.TaskTriggerSummaryTypeTypeEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.TaskWorkItem;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.UoWPayload;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.UoWPayloadSet;
import net.fhirfactory.dricats.util.FHIRContextUtility;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;


@ApplicationScoped
public class FHIRTaskMetadataResourceSetFromHL7v2x {
    private static final Logger LOG = LoggerFactory.getLogger(FHIRTaskMetadataResourceSetFromHL7v2x.class);

    private boolean initialised;
    private HL7ToFHIRConverter hl7ToFHIRConverter;
    private IParser fhirParser;

    @Inject
    private FHIRContextUtility fhirContextUtility;

    @Inject
    private HL7V2XTopicFactory hl7V2XTopicFactory;

    //
    // Constructor(s)
    //

    public FHIRTaskMetadataResourceSetFromHL7v2x(){
        this.initialised = false;
        this.hl7ToFHIRConverter = new HL7ToFHIRConverter();
    }

    //
    // Post Construct
    //

    @PostConstruct
    public void initialise(){
        getLogger().debug(".initialise(): Entry");
        if(initialised) {
            getLogger().debug(".initialise(): Doing nothing, already initialised!");
        } else {
            fhirParser = fhirContextUtility.getJsonParser();

            this.initialised = true;
        }
        getLogger().debug(".initialise(): Exit");
    }

    //
    // Business Logic
    //

    public List<Resource> extractResourcesForTaskMetadata(String hl7Message){
        getLogger().debug(".extractResourcesForTaskMetadata(): Entry");
        List<Resource> resourceList = new ArrayList<>();
        String convertedContent = null;
        try {
            convertedContent = hl7ToFHIRConverter.convert(hl7Message);
        } catch(Exception e){
            getLogger().warn(".extractResourcesForTaskMetadata(): Could not convert hl7 message, error->{}", ExceptionUtils.getStackTrace(e));
        }
        if(StringUtils.isEmpty(convertedContent)){
            getLogger().debug(".extractResourcesForTaskMetadata(): Exit, Empty Converted HL7 Content");
            return(resourceList);
        }
        Bundle bundle = null;
        try {
            bundle = fhirParser.parseResource(Bundle.class, convertedContent);
        } catch(Exception e){
            getLogger().warn(".extractResourcesForTaskMetadata(): Could not convert JSON string into a Bundle, error->{}", ExceptionUtils.getStackTrace(e));
        }
        if(bundle == null){
            getLogger().debug(".extractResourcesForTaskMetadata(): Exit, Empty Bundle");
            return(resourceList);
        }
        for(Bundle.BundleEntryComponent currentBundleEntry: bundle.getEntry()){
            Resource currentResource = currentBundleEntry.getResource();
            ResourceType currentResourceType = currentResource.getResourceType();
            switch(currentResourceType){
                case Patient:
                case Encounter:
                    resourceList.add(currentResource);
                    break;
                default:
                    // do nothing
            }
        }
        getLogger().debug(".extractResourcesForTaskMetadata(): Exit, resourceList->{}", resourceList);
        return(resourceList);
    }

    public TaskContextType newTaskContextType(PetasosActionableTask actionableTask){
        TaskContextType taskContext = null;
        if(actionableTask.hasTaskContext()){
            taskContext = SerializationUtils.clone(actionableTask.getTaskContext());
        } else {
            taskContext = new TaskContextType();
        }
        String hl7Message = extractHL7Message(actionableTask.getTaskWorkItem());
        if (StringUtils.isNotEmpty(hl7Message)) {
            List<Resource> metaResourceList = extractResourcesForTaskMetadata(hl7Message);
            if ( !taskContext.hasTaskBeneficiary() ) {
                Patient patientFromMetadataResourceList = getPatientFromMetadataResourceList(metaResourceList);
                if (patientFromMetadataResourceList != null) {
                    TaskBeneficiaryType taskBeneficiary = new TaskBeneficiaryType();
                    taskBeneficiary.setBeneficiaryType(TaskBeneficiaryTypeTypeEnum.TASK_BENEFICIARY_PATIENT);
                    taskBeneficiary.setIdentifier(patientFromMetadataResourceList.getIdentifierFirstRep());
                    taskContext.setTaskBeneficiary(taskBeneficiary);
                }
            }
            if (!taskContext.hasTaskEncounter()) {
                Encounter encounterFromMetadataResourceList = getEncounterFromMetadataResourceList(metaResourceList);
                if (encounterFromMetadataResourceList != null) {
                    TaskEncounterType taskEncounter = new TaskEncounterType();
                    taskEncounter.setEncounterId(encounterFromMetadataResourceList.getIdentifierFirstRep().getValue());
                    if (encounterFromMetadataResourceList.hasPeriod()) {
                        taskEncounter.setEncounterPeriod(encounterFromMetadataResourceList.getPeriod());
                    }
                    taskContext.setTaskEncounter(taskEncounter);
                }
            }
            TaskTriggerSummaryType taskTriggerSummary = null;
            if (!taskContext.hasTaskTriggerSummary()) {
                taskTriggerSummary = taskContext.getTaskTriggerSummary();
            } else {
                taskTriggerSummary = new TaskTriggerSummaryType();
                taskContext.setTaskTriggerSummary(taskTriggerSummary);
            }
            if(!taskTriggerSummary.hasTriggerSummaryType()){
                taskTriggerSummary.setTriggerSummaryType(TaskTriggerSummaryTypeTypeEnum.TASK_TRIGGER_HL7_V2X_MESSAGE);
            }
            if(!taskTriggerSummary.hasTriggerName()){
                String taskTriggerName = extractHL7TriggerName(actionableTask.getTaskWorkItem());
                if(taskTriggerName != null){
                    taskTriggerSummary.setTriggerName(taskTriggerName);
                }
            }
        }
        return(taskContext);
    }

    public Encounter getEncounterFromMetadataResourceList(List<Resource> metaResourceList){
        getLogger().debug(".getEncounterFromMetadataResourceList(): Entry, metaResourceList->{}", metaResourceList);
        if(metaResourceList == null){
            getLogger().debug(".getEncounterFromMetadataResourceList(): Exit, metaResourceList is null");
            return(null);
        }
        if(metaResourceList.isEmpty()){
            getLogger().debug(".getEncounterFromMetadataResourceList(): Exit, metaResourceList is empty");
            return(null);
        }
        for(Resource currentResource: metaResourceList){
            if(currentResource.getResourceType().equals(ResourceType.Encounter)){
                Encounter encounter = (Encounter)currentResource;
                getLogger().debug(".getEncounterFromMetadataResourceList(): Exit, encounter->{}", encounter);
                return(encounter);
            }
        }
        getLogger().debug(".getEncounterFromMetadataResourceList(): Exit, could not find encounter resource");
        return(null);
    }

    public Patient getPatientFromMetadataResourceList(List<Resource> metaResourceList){
        getLogger().debug(".getPatientFromMetadataResourceList(): Entry, metaResourceList->{}", metaResourceList);
        if(metaResourceList == null){
            getLogger().debug(".getPatientFromMetadataResourceList(): Exit, metaResourceList is null");
            return(null);
        }
        if(metaResourceList.isEmpty()){
            getLogger().debug(".getPatientFromMetadataResourceList(): Exit, metaResourceList is empty");
            return(null);
        }
        for(Resource currentResource: metaResourceList){
            if(currentResource.getResourceType().equals(ResourceType.Patient)){
                Patient patient = (Patient)currentResource;
                getLogger().debug(".getPatientFromMetadataResourceList(): Exit, patient->{}", patient);
                return(patient);
            }
        }
        getLogger().debug(".getPatientFromMetadataResourceList(): Exit, could not find patient resource");
        return(null);
    }

    public String extractHL7Message(TaskWorkItem taskWorkItem){
        if(containsHL7v2xMessage(taskWorkItem)){
            if(taskWorkItem.hasIngresContent()){
                DataParcelManifest payloadManifest = taskWorkItem.getIngresContent().getPayloadManifest();
                DataDescriptor parcelTypeDescriptor = payloadManifest.getContainerDescriptor();
                if(parcelTypeDescriptor != null){
                    boolean isHL7Defined = parcelTypeDescriptor.getDataParcelDefiner().equals(hl7V2XTopicFactory.getHl7MessageDefiner());
                    boolean isHL7Message = parcelTypeDescriptor.getDataParcelCategory().equals(hl7V2XTopicFactory.getHl7MessageCategory());
                    if(isHL7Message && isHL7Defined){
                        return(taskWorkItem.getIngresContent().getPayload());
                    }
                }
            }
        }
        if(taskWorkItem.hasEgressContent()){
            UoWPayloadSet egressContent = taskWorkItem.getEgressContent();
            if(egressContent != null) {
                for (UoWPayload currentPayload : egressContent.getPayloadElements()){
                    DataParcelManifest currentPayloadManifest = currentPayload.getPayloadManifest();
                    DataDescriptor currentParcelTypeDescriptor = currentPayloadManifest.getContainerDescriptor();
                    if(currentParcelTypeDescriptor != null){
                        boolean isHL7Defined = currentParcelTypeDescriptor.getDataParcelDefiner().equals(hl7V2XTopicFactory.getHl7MessageDefiner());
                        boolean isHL7Message = currentParcelTypeDescriptor.getDataParcelCategory().equals(hl7V2XTopicFactory.getHl7MessageCategory());
                        if(isHL7Message && isHL7Defined){
                            return(currentPayload.getPayload());
                        }
                    }
                }
            }
        }
        return(null);
    }

    public boolean containsHL7v2xMessage(TaskWorkItem taskWorkItem){
        if(taskWorkItem.hasIngresContent()){
            DataParcelManifest payloadManifest = taskWorkItem.getIngresContent().getPayloadManifest();
            DataDescriptor parcelTypeDescriptor = payloadManifest.getContainerDescriptor();
            if(parcelTypeDescriptor != null){
                boolean isHL7Defined = parcelTypeDescriptor.getDataParcelDefiner().equals(hl7V2XTopicFactory.getHl7MessageDefiner());
                boolean isHL7Message = parcelTypeDescriptor.getDataParcelCategory().equals(hl7V2XTopicFactory.getHl7MessageCategory());
                if(isHL7Message && isHL7Defined){
                    return(true);
                }
            }
        }
        if(taskWorkItem.hasEgressContent()){
            UoWPayloadSet egressContent = taskWorkItem.getEgressContent();
            if(egressContent != null) {
                for (UoWPayload currentPayload : egressContent.getPayloadElements()){
                    DataParcelManifest currentPayloadManifest = currentPayload.getPayloadManifest();
                    DataDescriptor currentParcelTypeDescriptor = currentPayloadManifest.getContainerDescriptor();
                    if(currentParcelTypeDescriptor != null){
                        boolean isHL7Defined = currentParcelTypeDescriptor.getDataParcelDefiner().equals(hl7V2XTopicFactory.getHl7MessageDefiner());
                        boolean isHL7Message = currentParcelTypeDescriptor.getDataParcelCategory().equals(hl7V2XTopicFactory.getHl7MessageCategory());
                        if(isHL7Message && isHL7Defined){
                            return(true);
                        }
                    }
                }
            }
        }
        return(false);
    }

    public String extractHL7TriggerName(TaskWorkItem taskWorkItem){
        if(taskWorkItem.hasIngresContent()){
            DataParcelManifest payloadManifest = taskWorkItem.getIngresContent().getPayloadManifest();
            DataDescriptor parcelTypeDescriptor = payloadManifest.getContainerDescriptor();
            if(parcelTypeDescriptor != null){
                boolean isHL7Defined = parcelTypeDescriptor.getDataParcelDefiner().equals(hl7V2XTopicFactory.getHl7MessageDefiner());
                boolean isHL7Message = parcelTypeDescriptor.getDataParcelCategory().equals(hl7V2XTopicFactory.getHl7MessageCategory());
                if(isHL7Message && isHL7Defined){
                    String taskTriggerName = generateTriggerNameFromContentDescriptor(parcelTypeDescriptor);
                    return(taskTriggerName);
                }
            }
        }
        if(taskWorkItem.hasEgressContent()){
            UoWPayloadSet egressContent = taskWorkItem.getEgressContent();
            if(egressContent != null) {
                for (UoWPayload currentPayload : egressContent.getPayloadElements()){
                    DataParcelManifest currentPayloadManifest = currentPayload.getPayloadManifest();
                    DataDescriptor currentParcelTypeDescriptor = currentPayloadManifest.getContainerDescriptor();
                    if(currentParcelTypeDescriptor != null){
                        boolean isHL7Defined = currentParcelTypeDescriptor.getDataParcelDefiner().equals(hl7V2XTopicFactory.getHl7MessageDefiner());
                        boolean isHL7Message = currentParcelTypeDescriptor.getDataParcelCategory().equals(hl7V2XTopicFactory.getHl7MessageCategory());
                        if(isHL7Message && isHL7Defined){
                            String taskTriggerName = generateTriggerNameFromContentDescriptor(currentParcelTypeDescriptor);
                            return(taskTriggerName);
                        }
                    }
                }
            }
        }
        return(null);
    }

    protected String generateTriggerNameFromContentDescriptor(DataDescriptor descriptor) {
        if (descriptor == null) {
            return (null);
        }
        StringBuilder stringBuilder = new StringBuilder();
        boolean hasDataParcelDefiner = descriptor.hasDataParcelDefiner();
        boolean hasDataParcelCategory = descriptor.hasDataParcelCategory();
        boolean hasDataParcelSubCategory = descriptor.hasDataParcelSubCategory();
        boolean hasDataParcelResource = descriptor.hasDataParcelResource();
        if (hasDataParcelDefiner) {
            stringBuilder.append(descriptor.getDataParcelDefiner());
        }
        if (hasDataParcelCategory) {
            if (hasDataParcelDefiner) {
                stringBuilder.append(".");
            }
            stringBuilder.append(descriptor.getDataParcelCategory());
        }
        if (hasDataParcelSubCategory) {
            if (hasDataParcelDefiner || hasDataParcelCategory) {
                stringBuilder.append(".");
            }
            stringBuilder.append(descriptor.getDataParcelSubCategory());
        }
        if (hasDataParcelResource){
            if (hasDataParcelCategory || hasDataParcelDefiner || hasDataParcelSubCategory) {
                stringBuilder.append(".");
            }
            stringBuilder.append(descriptor.getDataParcelResource());
        }
        String triggerName = stringBuilder.toString();
        return(triggerName);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}