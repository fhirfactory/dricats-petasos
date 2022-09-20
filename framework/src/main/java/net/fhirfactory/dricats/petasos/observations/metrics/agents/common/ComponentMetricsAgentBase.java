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
package net.fhirfactory.dricats.petasos.observations.metrics.agents.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.interfaces.observations.notifications.PetasosITOpsNotificationAgentInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.common.CommonComponentMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.notifications.valuesets.PetasosComponentITOpsNotificationTypeEnum;
import net.fhirfactory.dricats.petasos.observations.metrics.cache.PetasosLocalMetricsDM;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

public abstract class ComponentMetricsAgentBase  {

    private Object metricsDataLock;
    private boolean initialised;
    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    private static Long ROLLING_AVERAGE_COUNT = 10L;
    private PetasosLocalMetricsDM localMetricsDM;

    private PetasosITOpsNotificationAgentInterface notificationAgent;

    //
    // Constructor(s)
    //

    public ComponentMetricsAgentBase() {
        this.metricsDataLock = new Object();
        initialised = false;
    }

    //
    // Abstract Methods
    //

    abstract protected CommonComponentMetricsData getMetricsData();
    abstract public void sendITOpsNotification(String message);
    abstract public void sendITOpsNotification(String unformatedMessage, String formattedMessage);
    abstract public void sendITOpsNotification(String unformatedMessage, String formattedMessage, PetasosComponentITOpsNotificationTypeEnum notificationType, String notificationHeader);

    //
    // Some Helper Methods
    //

    @JsonIgnore
    public void touchLastActivityInstant(){
        getMetricsData().setLastActivityInstant(Instant.now());
    }

    //
    // Cache Interaction Methods
    //

    public void registerWithCache(){
        getLocalMetricsDM().addNodeMetrics(getMetricsData(), getMetricsDataLock());
    }

    //
    // Getters and Setters
    //

    public ProcessingPlantRoleSupportInterface getProcessingPlantCapabilityStatement() {
        return processingPlantCapabilityStatement;
    }

    public void setProcessingPlantCapabilityStatement(ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement) {
        this.processingPlantCapabilityStatement = processingPlantCapabilityStatement;
    }

    public Object getMetricsDataLock() {
        return metricsDataLock;
    }

    public Long getRollingAverageCount(){
        return(ROLLING_AVERAGE_COUNT);
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    public void setLocalMetricsDM(PetasosLocalMetricsDM localMetricsDM) {
        this.localMetricsDM = localMetricsDM;
    }

    protected PetasosLocalMetricsDM getLocalMetricsDM(){
        return(this.localMetricsDM);
    }

    protected PetasosITOpsNotificationAgentInterface getNotificationAgent(){
        return(notificationAgent);
    }

    public void setNotificationAgent(PetasosITOpsNotificationAgentInterface notificationAgent) {
        this.notificationAgent = notificationAgent;
    }

    //
    // Business Methods
    //

    @JsonIgnore
    public void incrementEgressMessageFailureCount(){
        synchronized (getMetricsDataLock()){
            int count = getMetricsData().getEgressMessageFailureCount();
            count += 1;
            getMetricsData().setEgressMessageFailureCount(count);
        }
    }

    @JsonIgnore
    public void incrementIngresMessageCount(){
        synchronized (getMetricsDataLock()) {
            int count = getMetricsData().getIngresMessageCount();
            count += 1;
            getMetricsData().setIngresMessageCount(count);
        }
    }

    @JsonIgnore
    public void incrementEgressMessageAttemptCount(){
        synchronized (getMetricsDataLock()) {
            int count = getMetricsData().getEgressMessageAttemptCount();
            count += 1;
            getMetricsData().setEgressMessageAttemptCount(count);
        }
    }

    @JsonIgnore
    public void incrementEgressMessageSuccessCount(){
        synchronized (getMetricsDataLock()) {
            int count = getMetricsData().getEgressMessageSuccessCount();
            count += 1;
            getMetricsData().setEgressMessageSuccessCount(count);
        }
    }

    @JsonIgnore
    public void incrementInternalMessageDistributionCount(){
        synchronized (getMetricsDataLock()) {
            int count = getMetricsData().getInternalDistributedMessageCount();
            count += 1;
            getMetricsData().setInternalDistributedMessageCount(count);
        }
    }

    @JsonIgnore
    public void incrementInternalReceivedMessageCount(){
        synchronized (getMetricsData()){
            int count = getMetricsData().getInternalReceivedMessageCount();
            count += 1;
            getMetricsData().setInternalReceivedMessageCount(count);
        }
    }

    @JsonIgnore
    public void incrementInternalMessageDistributionCount(String targetParticipantName){
        if(StringUtils.isNotEmpty(targetParticipantName)){
            synchronized (getMetricsDataLock()){
                if(!getMetricsData().getInternalDistributionCountMap().containsKey(targetParticipantName)){
                    getMetricsData().getInternalDistributionCountMap().put(targetParticipantName, 0);
                }
                Integer count = getMetricsData().getInternalDistributionCountMap().get(targetParticipantName);
                Integer newValue = count + 1;
                getMetricsData().getInternalDistributionCountMap().replace(targetParticipantName, newValue);
            }
        }
    }

    //
    // To String
    //


    @Override
    public String toString() {
        return "ComponentMetricsAgentBase{" +
                "metricsDataLock=" + metricsDataLock +
                ", initialised=" + initialised +
                ", processingPlantCapabilityStatement=" + processingPlantCapabilityStatement +
                ", localMetricsDM=" + localMetricsDM +
                ", notificationAgent=" + notificationAgent +
                ", rollingAverageCount=" + getRollingAverageCount() +
                '}';
    }
}
