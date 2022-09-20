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
package net.fhirfactory.dricats.model.transforms.tofhir.task.factories;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirfactory.dricats.configuration.defaults.dricats.systemwide.ReferenceProperties;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.performer.datatypes.TaskPerformerTypeType;

@ApplicationScoped
public class TaskPerformerTypeFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TaskPerformerTypeFactory.class);

    @Inject
    private ReferenceProperties systemWideProperties;

    private static final String PEGACORN_TASK_PERFORMER_TYPE = "/task-performer-type";

    //
    // Constructor
    //

    //
    // Business Methods
    //

    //
    // FromFHIR

    public TaskPerformerTypeType mapTaskPerformerType(CodeableConcept taskPerformerCC){
        getLogger().debug(".mapTaskPerformerType(): Entry, taskPerformerCC->{}", taskPerformerCC);
        TaskPerformerTypeType taskPerformer = new TaskPerformerTypeType();
        // Extract Node Participant Name

        // Extract Node Participant Description

        // Extract Node Function
        

        // Extract Node Implementation Component Id (if Exists)

        getLogger().debug(".mapTaskPerformerType(): Exit, taskPerformer->{}", taskPerformer);
        return(taskPerformer);
    }

    //
    // ToFHIR

    public CodeableConcept mapTaskPerformerType(TaskPerformerTypeType taskPerformer){
        getLogger().debug(".newTaskPerformerType(): Entry, taskPerformer->{}", taskPerformer);

        // Let's be defensive
        if(taskPerformer == null){
            getLogger().debug(".newTaskPerformerType(): Exit, taskPerformer is null, returning -null-");
            return(null);
        }
        if(StringUtils.isEmpty(taskPerformer.getRequiredParticipantName())){
            getLogger().debug(".newTaskPerformerType(): Exit, taskPerformer.getRequiredParticipantName is empty, returning -null-");
            return(null);
        }

        // Get the PerformerParticipantName
        String performerParticipantName = taskPerformer.getRequiredParticipantName();

        // Get the PerformerDescription
        String performerParticipantDescription = performerParticipantName;
        if(StringUtils.isNotEmpty(taskPerformer.getRequiredPerformerTypeDescription())){
            performerParticipantDescription = taskPerformer.getRequiredPerformerTypeDescription();
        }

        // Create the base CodeableConcept
        CodeableConcept performerType = mapTaskPerformerType(performerParticipantName, performerParticipantDescription);

        // Add the FunctionFDN Extension if a FunctionFDN is defined
        if(taskPerformer.getRequiredPerformerType() != null){
            Extension functionFDNExtension = mapPerformerParticipantoExtension(taskPerformer.getRequiredPerformerType());
            if(functionFDNExtension != null){
                performerType.addExtension(functionFDNExtension);
            }
        }

        // Add the ComponentId Extension if a knownFullerInstance is defined
        if(taskPerformer.getKnownFulfillerInstance() != null){
            Extension knownFulfiller = mapPerformerComponentIdToExtension(taskPerformer.getKnownFulfillerInstance());
            if(knownFulfiller != null){
                performerType.addExtension(knownFulfiller);
            }
        }

        // All Done!
        getLogger().debug(".newTaskPerformerType(): Exit, codeableConcept->{}",performerType );
        return(performerType);
    }

    protected String getPegacornTaskPerformerType(){
        String codeSystem = systemWideProperties.getPegacornCodeSystemSite() + PEGACORN_TASK_PERFORMER_TYPE;
        return (codeSystem);
    }

    protected CodeableConcept mapTaskPerformerType(String participantName, String participantDescription){
        CodeableConcept performerTypeCC = new CodeableConcept();
        Coding performerTypeCoding = new Coding();
        performerTypeCoding.setSystem(getPegacornTaskPerformerType());
        performerTypeCoding.setCode(participantName);
        performerTypeCC.addCoding(performerTypeCoding);
        performerTypeCC.setText(participantDescription);
        return(performerTypeCC);
    }

    protected String extractCodeFromCodeableConceptForTaskPerformerType(CodeableConcept performerType){
        if(performerType == null){
            return(null);
        }
        if (performerType.getCoding() == null) {
            return(null);
        }
        for(Coding code: performerType.getCoding()){
            if(code.getSystem().contentEquals(getPegacornTaskPerformerType())){
                return(code.getCode());
            }
        }
        return(null);
    }

    protected Extension mapPerformerParticipantoExtension(PetasosParticipantId peformerParticipantId){
        Extension performerFunctionExtension = new Extension();



        return(performerFunctionExtension);
    }

    protected Extension mapPerformerComponentIdToExtension(ComponentId componentId){
        Extension performerDescriptionExtension = new Extension();

        return(performerDescriptionExtension);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

}
