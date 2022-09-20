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

import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.ProcessingPlantMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.common.CommonComponentMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.dricats.model.petasos.observations.notifications.valuesets.PetasosComponentITOpsNotificationTypeEnum;
import net.fhirfactory.dricats.model.petasos.observations.topology.valuesets.PetasosMonitoredComponentTypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.processingplant.valuesets.ProcessingPlantRoleEnum;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.common.ComponentMetricsAgentBase;
import net.fhirfactory.dricats.petasos.observations.metrics.cache.PetasosLocalMetricsDM;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;



public class ProcessingPlantMetricsAgent extends ComponentMetricsAgentBase {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingPlantMetricsAgent.class);

    private ProcessingPlantMetricsData metricsData;

    //
    // Constructors
    //

    public ProcessingPlantMetricsAgent(ProcessingPlantRoleSupportInterface processingPlantFunction, PetasosLocalMetricsDM localMetricsDM, ComponentId componentId, String participantName){
        super();
        this.metricsData = new ProcessingPlantMetricsData();
        setLocalMetricsDM(localMetricsDM);
        this.metricsData.setParticipantName(participantName);
        this.metricsData.setComponentID(componentId);
        this.metricsData.setComponentStartupInstant(Instant.now());
        setProcessingPlantCapabilityStatement(processingPlantFunction);
        getLogger().debug(".WorkUnitProcessorMetricsAgent(): Initialising Processing Plant Metrics Agent ->{}", participantName);
    }

    //
    // Implement Superclass Methods
    //

    @Override
    protected CommonComponentMetricsData getMetricsData(){
        return(metricsData);
    }

    public ProcessingPlantMetricsData getProcessingPlantMetricsData(){
        return(metricsData);
    }

    //
    // Notifications
    //

    @Override
    public void sendITOpsNotification(String message) {
        if(getProcessingPlantCapabilityStatement().getProcessingPlantCapability().equals(ProcessingPlantRoleEnum.PETASOS_SERVICE_PROVIDER_ITOPS_MANAGEMENT)){
            return;
        }
        PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
        notification.setComponentId(getMetricsData().getComponentID());
        notification.setParticipantName(getProcessingPlantMetricsData().getParticipantName());
        notification.setContent(message);
        notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.NORMAL_NOTIFICATION_TYPE);
        notification.setContentHeading("ITOpsNotification("+getProcessingPlantMetricsData().getParticipantName()+")");
        notification.setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_SUBSYSTEM);
        getNotificationAgent().sendNotification(notification);
    }

    @Override
    public void sendITOpsNotification(String unformattedMessage, String formattedMessage) {
        if(getProcessingPlantCapabilityStatement().getProcessingPlantCapability().equals(ProcessingPlantRoleEnum.PETASOS_SERVICE_PROVIDER_ITOPS_MANAGEMENT)){
            return;
        }
        PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
        notification.setComponentId(getMetricsData().getComponentID());
        notification.setParticipantName(getProcessingPlantMetricsData().getParticipantName());
        notification.setContent(unformattedMessage);
        notification.setNotificationType(PetasosComponentITOpsNotificationTypeEnum.NORMAL_NOTIFICATION_TYPE);
        notification.setContentHeading("ITOpsNotification("+getProcessingPlantMetricsData().getParticipantName()+")");
        notification.setFormattedContent(formattedMessage);
        notification.setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_SUBSYSTEM);
        getNotificationAgent().sendNotification(notification);
    }

    @Override
    public void sendITOpsNotification(String unformatedMessage, String formattedMessage, PetasosComponentITOpsNotificationTypeEnum notificationType, String notificationHeader) {
        if(getProcessingPlantCapabilityStatement().getProcessingPlantCapability().equals(ProcessingPlantRoleEnum.PETASOS_SERVICE_PROVIDER_ITOPS_MANAGEMENT)){
            return;
        }
        PetasosComponentITOpsNotification notification = new PetasosComponentITOpsNotification();
        notification.setComponentId(getMetricsData().getComponentID());
        notification.setParticipantName(getProcessingPlantMetricsData().getParticipantName());
        notification.setContent(unformatedMessage);
        notification.setNotificationType(notificationType);
        notification.setContentHeading(notificationHeader);
        notification.setFormattedContent(formattedMessage);
        notification.setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_SUBSYSTEM);
        getNotificationAgent().sendNotification(notification);
    }

    //
    // Helpers
    //

    public void updateLocalCacheStatus(String cacheName, Integer cacheSize){
        if(StringUtils.isEmpty(cacheName) || cacheSize == null){
            return;
        }
        synchronized (getMetricsDataLock()) {
            if (getProcessingPlantMetricsData().getLocalCacheSize().containsKey(cacheName)) {
                getProcessingPlantMetricsData().getLocalCacheSize().replace(cacheName, cacheSize);
            }
            getProcessingPlantMetricsData().getLocalCacheSize().put(cacheName, cacheSize);
        }
    }

    public void incrementSynchronousAuditEventWritten(){
        synchronized (getMetricsDataLock()) {
            int count = getProcessingPlantMetricsData().getSynchronousAuditEventsWritten();
            count += 1;
            getProcessingPlantMetricsData().setSynchronousAuditEventsWritten(count);
        }
    }

    public void incrementAsynchronousAuditEventWritten(){
        synchronized (getMetricsDataLock()) {
            int count = getProcessingPlantMetricsData().getAsynchronousAuditEventsWritten();
            count += 1;
            getProcessingPlantMetricsData().setAsynchronousAuditEventsWritten(count);
        }
    }

    public void touchAsynchronousAuditEventWrite(){
        synchronized (getMetricsDataLock()) {
            getProcessingPlantMetricsData().setLastAsynchronousAuditEventWrite(Instant.now());
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
        return "ProcessingPlantMetricsAgent{" +
                "metricsData=" + metricsData +
                ", metricsDataLock=" + getMetricsDataLock() +
                ", rollingAverageCount=" + getRollingAverageCount() +
                '}';
    }
}
