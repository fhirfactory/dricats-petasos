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
package net.fhirfactory.dricats.petasos.tasking.observations.reporting.tasks;

import net.fhirfactory.dricats.petasos.observations.notifications.PetasosITOpsNotificationContentFactory;
import net.fhirfactory.pegacorn.internals.hl7v2.helpers.UltraDefensivePipeParser;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.status.valuesets.ActionableTaskOutcomeStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PetasosTaskReportFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosTaskReportFactory.class);

    private DateTimeFormatter timeFormatter;

    @Inject
    private PetasosITOpsNotificationContentFactory contentFactory;

    @Inject
    private UltraDefensivePipeParser pipeParser;

    //
    // Constructor(s)
    //

    public PetasosTaskReportFactory(){
        timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
    }
    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected DateTimeFormatter getTimeFormatter(){
        return(timeFormatter);
    }

    //
    // Business Method
    //

    public PetasosComponentITOpsNotification newTaskSummaryReport(PetasosActionableTask actionableTask){
        getLogger().debug(".newTaskSummaryReport(): Entry");

        //
        // Derive the Key Information
        String taskId = actionableTask.getTaskId().getId();
        String startTime = null;
        if(actionableTask.getTaskFulfillment().hasStartInstant()) {
            startTime = getTimeFormatter().format(actionableTask.getTaskFulfillment().getStartInstant());
        } else {
            startTime = "-";
        }
        String finishTime = null;
        if(actionableTask.getTaskFulfillment().hasFinishedDate()) {
            finishTime = getTimeFormatter().format(actionableTask.getTaskFulfillment().getFinishInstant());
        } else {
            finishTime = "-";
        }
        String fulfillerComponentName = "Not Available";
        String fulfillerComponentId = "Not Available";
        if(actionableTask.getTaskFulfillment().hasFulfillerWorkUnitProcessor()) {
            fulfillerComponentName = actionableTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getParticipantDisplayName();
            fulfillerComponentId = actionableTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentID().getId();
        } else {
            getLogger().warn(".newTaskSummaryReport(): No Task Fulfiller Component Defined on Task->{}", actionableTask.getTaskId());
        }
        ActionableTaskOutcomeStatusEnum outcomeStatus = actionableTask.getTaskOutcomeStatus().getOutcomeStatus();
        if(outcomeStatus == null){
            outcomeStatus = ActionableTaskOutcomeStatusEnum.ACTIONABLE_TASK_OUTCOME_STATUS_UNKNOWN;
        }
        String taskOutcomeStatus = outcomeStatus.getDisplayName();

        List<String> metadataHeader = null;
        List<String> metadataBody = null;
        if(contentFactory.isHL7V2Payload(actionableTask.getTaskWorkItem().getIngresContent())){
            metadataHeader = contentFactory.getHL7v2MetadataHeaderInfo(actionableTask.getTaskWorkItem().getIngresContent());
            if(!actionableTask.getTaskWorkItem().getIngresContent().getPayloadManifest().hasContainerDescriptor()) {
                metadataBody = pipeParser.extractMetadataFromHL7v2xMessage(actionableTask.getTaskWorkItem().getIngresContent().getPayload());
            } else {
                metadataBody = new ArrayList<>();
                metadataBody.add("Metadata Not Available");
            }
        } else {
            metadataHeader = new ArrayList<>();
            metadataBody = new ArrayList<>();
        }

        int outputPayloadCounter = 0;
        Map<Integer, List<String>> outputHeaders = new HashMap<>();
        Map<Integer, List<String>> outputMetadata = new HashMap<>();

        for(UoWPayload currentEgressPayload: actionableTask.getTaskWorkItem().getEgressContent().getPayloadElements()){
            if(contentFactory.isHL7V2Payload(currentEgressPayload)){
                List<String> currentHeaderList = contentFactory.getHL7v2MetadataHeaderInfo(currentEgressPayload);
                outputHeaders.put(outputPayloadCounter, currentHeaderList);
                if(!currentEgressPayload.getPayloadManifest().hasContainerDescriptor()) {
                    outputMetadata.put(outputPayloadCounter, pipeParser.extractMetadataFromHL7v2xMessage(currentEgressPayload.getPayload()));
                } else {
                    List<String> notOutputMetadataList = new ArrayList<>();
                    notOutputMetadataList.add("Metadata Not Available");
                    outputMetadata.put(outputPayloadCounter, notOutputMetadataList);
                }
            } else {
                List<String> currentHeaderList = pipeParser.getGeneralHeaderDetail(currentEgressPayload);
                outputHeaders.put(outputPayloadCounter, currentHeaderList);
                List<String> notOutputMetadataList = new ArrayList<>();
                notOutputMetadataList.add("Metadata Not Available");
                outputMetadata.put(outputPayloadCounter, notOutputMetadataList);
            }
            outputPayloadCounter += 1;
        }

        //
        // Build the Standard Text Body
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append(":: -------------------- Task Report ------------------ :: \n");
        reportBuilder.append(":: Task Id --> "+taskId +"\n");
        reportBuilder.append(":: Start Time --> "+startTime+"\n");
        reportBuilder.append(":: Finish Time --> "+finishTime+"\n");
        reportBuilder.append(":: Task Outcome --> "+taskOutcomeStatus+"\n");
        reportBuilder.append(":: Component Name --> "+fulfillerComponentName+" \n");
        reportBuilder.append(":: Component Id --> "+ fulfillerComponentId + "\n");
        reportBuilder.append(":: Component Id --> "+ fulfillerComponentId + "\n");
        reportBuilder.append(":: --- Input ---\n");
        for(String currentMetadataHeaderLine: metadataHeader) {
            reportBuilder.append(":: " + currentMetadataHeaderLine + "\n");
        }
        for(String currentMetadataBodyLine: metadataBody) {
            reportBuilder.append(currentMetadataBodyLine + "\n");
        }
        for(int outputCounter = 0; outputCounter < outputPayloadCounter; outputCounter += 1) {
            reportBuilder.append(":: --- Output[" + outputCounter + "] ---\n");
            for (String currentMetadataHeaderLine : outputHeaders.get(outputCounter)) {
                reportBuilder.append(":: " + currentMetadataHeaderLine + "\n");
            }
            for (String currentMetadataBodyLine : outputMetadata.get(outputCounter)) {
                reportBuilder.append(currentMetadataBodyLine + "\n");
            }
        }
        reportBuilder.append(":: --------------------------------------------------- :: \n");

        //
        // Build the Formatted Body
        StringBuilder formattedReportBuilder = new StringBuilder();
        formattedReportBuilder.append("<hr 'width=100%'>");
        formattedReportBuilder.append("<table 'cellpadding=0'>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<td><b>TaskId</b></td>");
        formattedReportBuilder.append("<td>"+taskId+"</td>");
        formattedReportBuilder.append("</tr>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<td><b>StartTime</b></td>");
        formattedReportBuilder.append("<td>"+startTime+"</td>");
        formattedReportBuilder.append("</tr>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<td><b>EndTime</b></td>");
        formattedReportBuilder.append("<td>"+finishTime+"</td>");
        formattedReportBuilder.append("</tr>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<td><b>Outcome</b></td>");
        if(outcomeStatus.equals(ActionableTaskOutcomeStatusEnum.ACTIONABLE_TASK_OUTCOME_STATUS_FAILED)) {
            formattedReportBuilder.append("<td><font color=Red" + taskOutcomeStatus + "</font></td>");
        } else {
            formattedReportBuilder.append("<td>" + taskOutcomeStatus + "</td>");
        }
        formattedReportBuilder.append("</tr>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<td><b>ParticipantName</b> </td>");
        formattedReportBuilder.append("<td>"+fulfillerComponentName+"</td>");
        formattedReportBuilder.append("</tr>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<td><b>ComponentID</b></td>");
        formattedReportBuilder.append("<td>"+fulfillerComponentId+"</td>");
        formattedReportBuilder.append("</tr>");
        for(String currentMetadataHeaderLine: metadataHeader) {
            formattedReportBuilder.append("<tr>");
            formattedReportBuilder.append("<td><b>Input Type</b></td>");
            formattedReportBuilder.append("<td>" + currentMetadataHeaderLine + "</td>");
            formattedReportBuilder.append("</tr>");
        }
        for(String currentMetadataBodyLine: metadataBody) {
            formattedReportBuilder.append("<tr>");
            formattedReportBuilder.append("<td><b>Input Meta</b></td>");
            formattedReportBuilder.append("<td>" + currentMetadataBodyLine + "</td>");
            formattedReportBuilder.append("</tr>");
        }
        for(int outputCounter = 0; outputCounter < outputPayloadCounter; outputCounter += 1) {
            for (String currentMetadataHeaderLine : outputHeaders.get(outputCounter)) {
                formattedReportBuilder.append("<tr>");
                formattedReportBuilder.append("<td><b>Output["+outputCounter+"] Type</b></td>");
                formattedReportBuilder.append("<td>" + currentMetadataHeaderLine + "</td>");
                formattedReportBuilder.append("</tr>");
            }
            for (String currentMetadataBodyLine : outputMetadata.get(outputCounter)) {
                formattedReportBuilder.append("<tr>");
                formattedReportBuilder.append("<td><b>Output["+outputCounter+"] Meta</b></td>");
                formattedReportBuilder.append("<td>" + currentMetadataBodyLine + "</td>");
                formattedReportBuilder.append("</tr>");
            }
        }
        formattedReportBuilder.append("</table>");

        PetasosComponentITOpsNotification taskReport = new PetasosComponentITOpsNotification();

        taskReport.setContent(reportBuilder.toString());
        taskReport.setFormattedContent(formattedReportBuilder.toString());

        getLogger().debug(".newTaskSummaryReport(): Exit, taskReport->{}", taskReport);
        return(taskReport);
    }

    public String newTaskJourneyReportHeader(PetasosActionableTask firstActionableTask){


        return(null);
    }

    public PetasosComponentITOpsNotification newTaskSummaryReport(PetasosActionableTask actionableTask, List<PetasosActionableTask> newActionableTasks){
        getLogger().debug(".newTaskSummaryReport(): Entry");
        if(actionableTask == null){
            getLogger().debug(".newTaskSummaryReport(): Exit, actionableTask is null");
            return(null);
        }
        //
        // Derive the Key Information
        String taskId = actionableTask.getTaskId().getId();
        Boolean noNewTasks = newActionableTasks.isEmpty();

        List<String> downstreamTaskIDs = new ArrayList<>();
        if(!noNewTasks){
            for(PetasosActionableTask currentActionableTask: newActionableTasks){
                downstreamTaskIDs.add(currentActionableTask.getTaskId().getId());
            }
        }


        //
        // Build the Standard Text Body
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append(":: -------------------- Task Report ------------------ :: \n");
        reportBuilder.append(":: Current Task Id --> "+taskId +"\n");
        if(noNewTasks){
            reportBuilder.append("::  No Downstream Tasks Created (no downstream subscribers present) ::"+"\n");
        } else {
            Integer counter = 0;
            for(String currentDownstreamTaskId: downstreamTaskIDs){
                reportBuilder.append(":: Downstream Task ID [" + counter + "] --> " + currentDownstreamTaskId + "\n");
                counter += 1;
            }
        }
        reportBuilder.append(":: -------------------- Task Report ------------------ :: \n");

        //
        // Build the Formatted Body
        StringBuilder formattedReportBuilder = new StringBuilder();
        formattedReportBuilder.append("<hr width=100%>");
        formattedReportBuilder.append("<table style='width:100%'>");
        formattedReportBuilder.append("<tr>");
        formattedReportBuilder.append("<td><b>Current TaskId</b></td>");
        formattedReportBuilder.append("<td>"+taskId+"</td>");
        formattedReportBuilder.append("</tr>");
        if(noNewTasks){
            formattedReportBuilder.append("<tr>");
            formattedReportBuilder.append("<td><b>Downstream Task ID</b></td>");
            formattedReportBuilder.append("<td> No Downstream Tasks Created (no downstream subscribers present) </td>");
            formattedReportBuilder.append("</tr>");
            formattedReportBuilder.append("<tr>");
        } else {
            Integer counter = 0;
            for(String currentDownstreamTaskId: downstreamTaskIDs){
                formattedReportBuilder.append("<tr>");
                formattedReportBuilder.append("<td><b>Downstream Task ID [" + counter + "]</b></td>");
                formattedReportBuilder.append("<td>" + currentDownstreamTaskId + "</td>");
                formattedReportBuilder.append("</tr>");
                formattedReportBuilder.append("<tr>");
                counter += 1;
            }
        }
        formattedReportBuilder.append("</table>");

        PetasosComponentITOpsNotification taskReport = new PetasosComponentITOpsNotification();

        taskReport.setContent(reportBuilder.toString());
        taskReport.setFormattedContent(formattedReportBuilder.toString());

        getLogger().debug(".newTaskSummaryReport(): Exit, taskReport->{}", taskReport);
        return(taskReport);
    }


}
