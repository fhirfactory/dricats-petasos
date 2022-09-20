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

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirfactory.dricats.configuration.defaults.dricats.systemwide.ReferenceProperties;

@ApplicationScoped
public class TaskStatusReasonFactory {

    @Inject
    private ReferenceProperties systemWideProperties;

    private static final String PEGACORN_TASK_STATUS_REASON_SYSTEM = "/task-status-reason";

    private static final String PEGACORN_TASK_IS_FINALISED_CODE = "pegacorn.task.status.reason.finalised";
    private static final String PEGACORN_TASK_IS_NOT_FINALISED_CODE = "pegacorn.task.status.reason.not-finalised";

    //
    // Business Methods
    //

    public String getPegacornTaskStatusReasonSystem(){
        String codeSystem = systemWideProperties.getPegacornCodeSystemSite() + PEGACORN_TASK_STATUS_REASON_SYSTEM;
        return (codeSystem);
    }

    public String getPegacornTaskIsFinalisedCode(){
        return(PEGACORN_TASK_IS_FINALISED_CODE);
    }

    public String getPegacornTaskIsNotFinalisedCode(){
        return(PEGACORN_TASK_IS_NOT_FINALISED_CODE);
    }

    public CodeableConcept newTaskStatusReason(boolean isFinalised ){
        CodeableConcept taskReasonCC = new CodeableConcept();
        Coding taskReasonCoding = new Coding();
        taskReasonCoding.setSystem(getPegacornTaskStatusReasonSystem());
        if(isFinalised){
            taskReasonCoding.setCode(PEGACORN_TASK_IS_FINALISED_CODE);
            taskReasonCoding.setDisplay("Task Finalised");
        } else {
            taskReasonCoding.setCode(PEGACORN_TASK_IS_NOT_FINALISED_CODE);
            taskReasonCoding.setDisplay("Task Not Finalised");
        }
        taskReasonCC.setText(taskReasonCoding.getDisplay());
        taskReasonCC.addCoding(taskReasonCoding);
        return(taskReasonCC);
    }
}
