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
package net.fhirfactory.dricats.petasos.observations.metrics.agents;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.EndpointMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.common.CommonComponentMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.dricats.model.petasos.observations.notifications.valuesets.PetasosComponentITOpsNotificationTypeEnum;
import net.fhirfactory.dricats.model.petasos.observations.topology.valuesets.PetasosMonitoredComponentTypeEnum;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.common.ComponentMetricsAgentBase;
import net.fhirfactory.dricats.petasos.observations.metrics.cache.PetasosLocalMetricsDM;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;


public class EndpointMetricsAgent extends ComponentMetricsAgentBase {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointMetricsAgent.class);

    private EndpointMetricsData metricsData;
    private String connectedSystemName;
    private String endpointDisplayName;

    //
    // Constructors
    //

    public EndpointMetricsAgent(ProcessingPlantRoleSupportInterface processingPlantFunction, PetasosLocalMetricsDM localMetricsDM, ComponentId componentId, String participantName, String connectedSystemName, String endpointDisplayName){
        super();
        getLogger().debug(".EndpointMetricsAgent(): Initialising Endpoint Metrics Agent");
        this.metricsData = new EndpointMetricsData();
        setLocalMetricsDM(localMetricsDM);
        this.metricsData.setParticipantName(participantName);
        this.metricsData.setComponentID(componentId);
        this.metricsData.setComponentStartupInstant(Instant.now());
        this.connectedSystemName = connectedSystemName;
        this.endpointDisplayName = endpointDisplayName;
        setProcessingPlantCapabilityStatement(processingPlantFunction);
        getLogger().debug(".EndpointMetricsAgent(): Initialising Endpoint Metrics Agent ->{}", participantName);
    }

    //
    // Implement Superclass Methods
    //

    @Override
    protected CommonComponentMetricsData getMetricsData(){
        return(metricsData);
    }

    public EndpointMetricsData getEndpointMetricsData(){
        return(metricsData);
    }

    //
    // Getters and Setters
    //

    public String getConnectedSystemName() {
        return connectedSystemName;
    }

    public void setConnectedSystemName(String connectedSystemName) {
        this.connectedSystemName = connectedSystemName;
    }

    public String getEndpointDisplayName() {
        return endpointDisplayName;
    }

    public void setEndpointDisplayName(String endpointDisplayName) {
        this.endpointDisplayName = endpointDisplayName;
    }


    //
    // Notifications
    //

    @Override
    public void sendITOpsNotification(String message) {
        try {
            PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
            notification.setComponentId(getMetricsData().getComponentID());
            notification.setParticipantName(getEndpointMetricsData().getParticipantName());
            notification.setContent(message);
            notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.NORMAL_NOTIFICATION_TYPE);
            notification.setContentHeading("ITOpsNotification("+getEndpointMetricsData().getParticipantName()+")");
            notification.setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_ENDPOINT);
            getNotificationAgent().sendNotification(notification);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(".sendITOpsNotification(): Send Notification->{}", notification);
            }
        } catch(Exception ex){
            getLogger().warn(".sendITOpsNotification(): Can't send notifications, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
    }

    @Override
    public void sendITOpsNotification(String unformattedMessage, String formattedMessage) {
        try {
            PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
            notification.setComponentId(getMetricsData().getComponentID());
            notification.setParticipantName(getEndpointMetricsData().getParticipantName());
            notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.NORMAL_NOTIFICATION_TYPE);
            notification.setContentHeading("ITOpsNotification("+getEndpointMetricsData().getParticipantName()+")");
            notification.setContent(unformattedMessage);
            notification.setFormattedContent(formattedMessage);
            notification.setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_ENDPOINT);
            getNotificationAgent().sendNotification(notification);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(".sendITOpsNotification(): Send Notification->{}", notification);
            }
        } catch(Exception ex){
            getLogger().warn(".sendITOpsNotification(): Can't send notifications, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
    }

    @Override
    public void sendITOpsNotification(String unformattedMessage, String formattedMessage, PetasosComponentITOpsNotificationTypeEnum notificationType, String notificationHeader) {
        try {
            PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
            notification.setComponentId(getMetricsData().getComponentID());
            notification.setParticipantName(getEndpointMetricsData().getParticipantName());
            notification.setContent(unformattedMessage);
            notification.setFormattedContent(formattedMessage);
            notification.setNotificationType(notificationType);
            notification.setContentHeading(notificationHeader);
            notification.setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_ENDPOINT);
            getNotificationAgent().sendNotification(notification);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(".sendITOpsNotification(): Send Notification->{}", notification);
            }
        } catch(Exception ex){
            getLogger().warn(".sendITOpsNotification(): Can't send notifications, message->{}, stacktrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
        }
    }

    //
    // Helpers
    //

    @JsonIgnore
    public void incrementRemoteProcedureCallCount(){
        getLogger().debug(".incrementRemoteProcedureCallCount(): Entry");
        synchronized (getMetricsDataLock()){
            int count = getEndpointMetricsData().getRemoteProcedureCallCount();
            count += 1;
            getEndpointMetricsData().setRemoteProcedureCallCount(count);
        }
        getLogger().debug(".incrementRemoteProcedureCallCount(): Exit");
    }

    @JsonIgnore
    public void incrementRemoteProcedureCallFailureCount(){
        getLogger().debug(".incrementRemoteProcedureCallFailureCount(): Entry");
        synchronized (getMetricsDataLock()){
            int count = getEndpointMetricsData().getRemoteProcedureCallFailureCount();
            count += 1;
            getEndpointMetricsData().setRemoteProcedureCallCount(count);
        }
        getLogger().debug(".incrementRemoteProcedureCallFailureCount(): Exit");
    }

    @JsonIgnore
    public void incrementRemoteProcedureCallHandledCount(){
        getLogger().debug(".incrementRemoteProcedureCallHandledCount(): Entry");
        synchronized (getMetricsDataLock()){
            int count = getEndpointMetricsData().getRemoteProcedureCallCount();
            count += 1;
            getEndpointMetricsData().setRemoteProcedureCallCount(count);
        }
        getLogger().debug(".incrementRemoteProcedureCallHandledCount(): Exit");
    }

    @JsonIgnore
    public void incrementEgressSendAttemptCount(){
        synchronized (getMetricsDataLock()){
            int count = getEndpointMetricsData().getEgressSendAttemptCount();
            count += 1;
            getEndpointMetricsData().setEgressSendAttemptCount(count);
        }
    }

    @JsonIgnore
    public void incrementRPCRequestCount(String targetParticipantName){
        if(StringUtils.isNotEmpty(targetParticipantName)){
            synchronized (getMetricsDataLock()){
                if(!getEndpointMetricsData().getRemoteProcedureCallRequestsMap().containsKey(targetParticipantName)){
                    getEndpointMetricsData().getRemoteProcedureCallRequestsMap().put(targetParticipantName, 0);
                }
                Integer count = getEndpointMetricsData().getRemoteProcedureCallRequestsMap().get(targetParticipantName);
                Integer newValue = count + 1;
                getEndpointMetricsData().getRemoteProcedureCallRequestsMap().replace(targetParticipantName, newValue);
            }
        }
    }

    @JsonIgnore
    public void incrementRPCResponseCount(String targetParticipantName){
        if(StringUtils.isNotEmpty(targetParticipantName)){
            synchronized (getMetricsDataLock()){
                if(!getEndpointMetricsData().getRemoteProcedureCallResponsesMap().containsKey(targetParticipantName)){
                    getEndpointMetricsData().getRemoteProcedureCallResponsesMap().put(targetParticipantName, 0);
                }
                Integer count = getEndpointMetricsData().getRemoteProcedureCallResponsesMap().get(targetParticipantName);
                Integer newValue = count + 1;
                getEndpointMetricsData().getRemoteProcedureCallResponsesMap().replace(targetParticipantName, newValue);
            }
        }
    }

    @JsonIgnore
    public void incrementRPCFailureCount(String targetParticipantName){
        if(StringUtils.isNotEmpty(targetParticipantName)){
            synchronized (getMetricsDataLock()){
                if(!getEndpointMetricsData().getRemoteProcedureCallFailuresMap().containsKey(targetParticipantName)){
                    getEndpointMetricsData().getRemoteProcedureCallFailuresMap().put(targetParticipantName, 0);
                }
                Integer count = getEndpointMetricsData().getRemoteProcedureCallFailuresMap().get(targetParticipantName);
                Integer newValue = count + 1;
                getEndpointMetricsData().getRemoteProcedureCallFailuresMap().replace(targetParticipantName, newValue);
            }
        }
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }


    //
    // To String
    //

    @Override
    public String toString() {
        return "EndpointMetricsAgent{" +
                "metricsData=" + metricsData +
                ", endpointMetricsData=" + getEndpointMetricsData() +
                ", metricsDataLock=" + getMetricsDataLock() +
                ", rollingAverageCount=" + getRollingAverageCount() +
                ", initialised=" + isInitialised() +
                '}';
    }
}
