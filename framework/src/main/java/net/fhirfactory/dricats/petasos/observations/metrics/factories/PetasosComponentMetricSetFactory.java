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
package net.fhirfactory.dricats.petasos.observations.metrics.factories;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.EndpointMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.ProcessingPlantMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.WorkUnitProcessorMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.common.CommonComponentMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.metrics.reporting.PetasosComponentMetric;
import net.fhirfactory.dricats.model.petasos.observations.metrics.reporting.PetasosComponentMetricSet;
import net.fhirfactory.dricats.model.petasos.observations.metrics.reporting.datatypes.PetasosComponentMetricValue;
import net.fhirfactory.dricats.model.petasos.observations.metrics.reporting.valuesets.PetasosComponentMetricTypeEnum;
import net.fhirfactory.dricats.model.petasos.observations.metrics.reporting.valuesets.PetasosComponentMetricUnitEnum;
import net.fhirfactory.dricats.petasos.participant.processingplant.PetasosEnabledProcessingPlant;
import net.fhirfactory.dricats.petasos.participant.processingplant.PetasosEnabledProcessingPlantInformationAccessor;

@ApplicationScoped
public class PetasosComponentMetricSetFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosComponentMetricSetFactory.class);

    @Inject
    private PetasosEnabledProcessingPlantInformationAccessor processingPlantServicesAccessor;
 
    //
    // Getters (and Setters)
    //

    protected static Logger getLOG() {
        return LOG;
    }

    protected PetasosEnabledProcessingPlantInformationAccessor getProcessingPlantServicesAccessor() {
        return processingPlantServicesAccessor;
    }
    
    protected PetasosEnabledProcessingPlant getProcessingPlant(){
        return(processingPlantServicesAccessor.getProcessingPlant());
    }

    //
    // Metric Conversion Functions
    //

    public PetasosComponentMetricSet convertProcessingPlantMetricsData(ProcessingPlantMetricsData plantMetricsData) {
        getLogger().debug(".convertProcessingPlantMetricsData(): Entry, plantMetricsData->{}", plantMetricsData);

        if (plantMetricsData.getComponentID() == null) {
                getLogger().debug(".convertProcessingPlantMetricsData(): Exit, plantMetricsData is null, returning -null-");
                return (null);
        }

        PetasosComponentMetricSet metricSet = new PetasosComponentMetricSet();
        metricSet.setMetricSourceComponentId(plantMetricsData.getComponentID());
        metricSet.setSourceParticipantName(plantMetricsData.getParticipantName());
        metricSet.setComponentType(plantMetricsData.getComponentType());

        PetasosComponentMetricSet commonMetrics = convertCommonMetrics(plantMetricsData);
        Set<String> commonMetricsKeys = commonMetrics.getMetrics().keySet();
        for (String currentMetric : commonMetricsKeys) {
                metricSet.addMetric(commonMetrics.getMetric(currentMetric));
        }

        if (plantMetricsData.getComponentStatus() != null) {
                PetasosComponentMetric componentStatusMetric = new PetasosComponentMetric();
                componentStatusMetric.setMetricAgent(plantMetricsData.getComponentID());
                componentStatusMetric.setMetricSource(plantMetricsData.getComponentID());
                componentStatusMetric.setMetricName("ProcessingPlant-Status");
                componentStatusMetric.setMetricType(PetasosComponentMetricTypeEnum.COMPONENT_GENERAL_STATUS);
                componentStatusMetric.setMetricUnit(PetasosComponentMetricUnitEnum.STRING_DESCRIPTION);
                componentStatusMetric
                                .setMetricValue(new PetasosComponentMetricValue(plantMetricsData.getComponentStatus()));
                metricSet.addMetric(componentStatusMetric);
        }

        if (plantMetricsData.getAsynchronousAuditEventsWritten() >= 0) {
                PetasosComponentMetric auditEventsWritten = new PetasosComponentMetric();
                auditEventsWritten.setMetricAgent(plantMetricsData.getComponentID());
                auditEventsWritten.setMetricSource(plantMetricsData.getComponentID());
                auditEventsWritten.setMetricName("Async-AuditEvent-Writes");
                auditEventsWritten.setMetricType(PetasosComponentMetricTypeEnum.AUDIT_EVENT_WRITES);
                auditEventsWritten.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                auditEventsWritten.setMetricValue(
                                new PetasosComponentMetricValue(plantMetricsData.getAsynchronousAuditEventsWritten()));
                metricSet.addMetric(auditEventsWritten);
        }

        if (plantMetricsData.getSynchronousAuditEventsWritten() >= 0) {
                PetasosComponentMetric auditEventsWritten = new PetasosComponentMetric();
                auditEventsWritten.setMetricAgent(plantMetricsData.getComponentID());
                auditEventsWritten.setMetricSource(plantMetricsData.getComponentID());
                auditEventsWritten.setMetricName("Sync-AuditEvent-Writes");
                auditEventsWritten.setMetricType(PetasosComponentMetricTypeEnum.AUDIT_EVENT_WRITES);
                auditEventsWritten.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                auditEventsWritten.setMetricValue(
                                new PetasosComponentMetricValue(plantMetricsData.getSynchronousAuditEventsWritten()));
                metricSet.addMetric(auditEventsWritten);
        }

        if (plantMetricsData.getAsynchronousAuditEventsQueued() >= 0) {
                PetasosComponentMetric auditEventsWritten = new PetasosComponentMetric();
                auditEventsWritten.setMetricAgent(plantMetricsData.getComponentID());
                auditEventsWritten.setMetricSource(plantMetricsData.getComponentID());
                auditEventsWritten.setMetricName("Queued-AuditEvent-Writes");
                auditEventsWritten.setMetricType(PetasosComponentMetricTypeEnum.AUDIT_EVENT_WRITES);
                auditEventsWritten.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                auditEventsWritten.setMetricValue(
                                new PetasosComponentMetricValue(plantMetricsData.getAsynchronousAuditEventsQueued()));
                metricSet.addMetric(auditEventsWritten);
        }

        if (plantMetricsData.getLastActivityInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(plantMetricsData.getComponentID());
                activityInstant.setMetricSource(plantMetricsData.getComponentID());
                activityInstant.setMetricName("Last-Activity-Instant");
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.ACTIVITY_TIMESTAMP);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant.setMetricValue(new PetasosComponentMetricValue(plantMetricsData.getLastActivityInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (plantMetricsData.getComponentStartupInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(plantMetricsData.getComponentID());
                activityInstant.setMetricSource(plantMetricsData.getComponentID());
                activityInstant.setMetricName("Component-Startup-Instant");
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.ACTIVITY_TIMESTAMP);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant
                                .setMetricValue(new PetasosComponentMetricValue(plantMetricsData.getComponentStartupInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (!plantMetricsData.getLocalCacheSize().isEmpty()) {
                for (String currentCacheMetricName : plantMetricsData.getLocalCacheSize().keySet()) {
                        PetasosComponentMetric componentStatusMetric = new PetasosComponentMetric();
                        componentStatusMetric.setMetricAgent(plantMetricsData.getComponentID());
                        componentStatusMetric.setMetricSource(plantMetricsData.getComponentID());
                        componentStatusMetric.setMetricName(currentCacheMetricName);
                        componentStatusMetric.setMetricType(PetasosComponentMetricTypeEnum.CACHE_SIZE);
                        componentStatusMetric.setMetricUnit(PetasosComponentMetricUnitEnum.STRING_DESCRIPTION);
                        componentStatusMetric.setMetricValue(new PetasosComponentMetricValue(
                                        plantMetricsData.getLocalCacheSize().get(currentCacheMetricName)));
                        metricSet.addMetric(componentStatusMetric);
                }
        }

        getLogger().debug(".convertProcessingPlantMetricsData(): Exit, metricSet->{}", metricSet);
        return (metricSet);
    }

    public PetasosComponentMetricSet convertWorkUnitProcessorMetricsData(WorkUnitProcessorMetricsData wupMetricsData) {
        getLogger().debug(".convertWorkUnitProcessorMetricsData(): Entry, wupMetricsData->{}", wupMetricsData);

        if (wupMetricsData == null) {
                getLogger().debug(".convertWorkUnitProcessorMetricsData(): Exit, wupMetricsData is null, returning -null-");
                return (null);
        }

        if (wupMetricsData.getComponentID() == null) {
                getLogger().debug(".convertWorkUnitProcessorMetricsData(): Exit, componentId is null, returning -null-");
                return (null);
        }

        if (wupMetricsData.getParticipantName() == null) {
                getLogger()
                                .debug(".convertWorkUnitProcessorMetricsData(): Exit, participantName is null, returning -null-");
                return (null);
        }

        PetasosComponentMetricSet metricSet = new PetasosComponentMetricSet();
        metricSet.setMetricSourceComponentId(wupMetricsData.getComponentID());
        metricSet.setSourceParticipantName(wupMetricsData.getParticipantName());
        metricSet.setComponentType(wupMetricsData.getComponentType());
        metricSet.setSourceParticipantName(wupMetricsData.getParticipantName());

        PetasosComponentMetricSet commonMetrics = convertCommonMetrics(wupMetricsData);
        Set<String> commonMetricsKeys = commonMetrics.getMetrics().keySet();
        for (String currentMetric : commonMetricsKeys) {
                metricSet.addMetric(commonMetrics.getMetric(currentMetric));
        }

        if (wupMetricsData.getRegisteredTasks() >= 0) {
                PetasosComponentMetric registeredTaskCount = new PetasosComponentMetric();
                registeredTaskCount.setMetricAgent(getProcessingPlant().getComponentId());
                registeredTaskCount.setMetricSource(wupMetricsData.getComponentID());
                registeredTaskCount.setMetricName(PetasosComponentMetricTypeEnum.REGISTERED_TASK_COUNT.getDisplayName());
                registeredTaskCount.setMetricType(PetasosComponentMetricTypeEnum.REGISTERED_TASK_COUNT);
                registeredTaskCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                registeredTaskCount.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getRegisteredTasks()));
                metricSet.addMetric(registeredTaskCount);
        }

        if (wupMetricsData.getStartedTasks() >= 0) {
                PetasosComponentMetric startedTasks = new PetasosComponentMetric();
                startedTasks.setMetricAgent(getProcessingPlant().getComponentId());
                startedTasks.setMetricSource(wupMetricsData.getComponentID());
                startedTasks.setMetricName(PetasosComponentMetricTypeEnum.STARTED_TASK_COUNT.getDisplayName());
                startedTasks.setMetricType(PetasosComponentMetricTypeEnum.STARTED_TASK_COUNT);
                startedTasks.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                startedTasks.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getRegisteredTasks()));
                metricSet.addMetric(startedTasks);
        }

        if (wupMetricsData.getRollingEventProcessingDuration() > 0) {
                PetasosComponentMetric rollingEventProcessingDuration = new PetasosComponentMetric();
                rollingEventProcessingDuration
                                .setMetricAgent(getProcessingPlant().getComponentId());
                rollingEventProcessingDuration.setMetricSource(wupMetricsData.getComponentID());
                rollingEventProcessingDuration
                                .setMetricName(PetasosComponentMetricTypeEnum.ROLLING_TASK_PROCESSING_TIME.getDisplayName());
                rollingEventProcessingDuration.setMetricType(PetasosComponentMetricTypeEnum.ROLLING_TASK_PROCESSING_TIME);
                rollingEventProcessingDuration.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_DURATION_MILLISECONDS);
                String roundedValue = String.format("%.3f", (wupMetricsData.getRollingEventProcessingDuration() + 0.005D))
                                + " sec";
                rollingEventProcessingDuration.setMetricValue(new PetasosComponentMetricValue(roundedValue));
                metricSet.addMetric(rollingEventProcessingDuration);
        }

        if (wupMetricsData.getEventProcessingStartInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(wupMetricsData.getComponentID());
                activityInstant.setMetricName("Last-Task-Start-Time");
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.ACTIVITY_TIMESTAMP);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant
                                .setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getEventProcessingStartInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (wupMetricsData.getEventProcessingFinishInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(wupMetricsData.getComponentID());
                activityInstant.setMetricName("Last-Task-Finish-Time");
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.ACTIVITY_TIMESTAMP);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant
                                .setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getEventProcessingFinishInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (wupMetricsData.getAverageEventProcessingDuration() > 0) {
                PetasosComponentMetric averageEventProcessingDuration = new PetasosComponentMetric();
                averageEventProcessingDuration.setMetricAgent(getProcessingPlant().getComponentId());
                averageEventProcessingDuration.setMetricSource(wupMetricsData.getComponentID());
                averageEventProcessingDuration.setMetricName(PetasosComponentMetricTypeEnum.CUMULATIVE_TASK_PROCESSING_TIME.getDisplayName());
                averageEventProcessingDuration.setMetricType(PetasosComponentMetricTypeEnum.CUMULATIVE_TASK_PROCESSING_TIME);
                averageEventProcessingDuration.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_DURATION_MILLISECONDS);
                String roundedValue = String.format("%.3f", (wupMetricsData.getAverageEventProcessingDuration() + 0.005D))+ " sec";
                averageEventProcessingDuration.setMetricValue(new PetasosComponentMetricValue(roundedValue));
                metricSet.addMetric(averageEventProcessingDuration);
        }

        if (wupMetricsData.getCancelledTasks() >= 0) {
                PetasosComponentMetric cancelledTaskCount = new PetasosComponentMetric();
                cancelledTaskCount.setMetricAgent(getProcessingPlant().getComponentId());
                cancelledTaskCount.setMetricSource(wupMetricsData.getComponentID());
                cancelledTaskCount.setMetricName(PetasosComponentMetricTypeEnum.CANCELLED_TASK_COUNT.getDisplayName());
                cancelledTaskCount.setMetricType(PetasosComponentMetricTypeEnum.CANCELLED_TASK_COUNT);
                cancelledTaskCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                cancelledTaskCount.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getCancelledTasks()));
                metricSet.addMetric(cancelledTaskCount);
        }

        if (wupMetricsData.getFinishedTasks() >= 0) {
                PetasosComponentMetric finishedTaskCount = new PetasosComponentMetric();
                finishedTaskCount.setMetricAgent(getProcessingPlant().getComponentId());
                finishedTaskCount.setMetricSource(wupMetricsData.getComponentID());
                finishedTaskCount.setMetricName(PetasosComponentMetricTypeEnum.FINISHED_TASK_COUNT.getDisplayName());
                finishedTaskCount.setMetricType(PetasosComponentMetricTypeEnum.FINISHED_TASK_COUNT);
                finishedTaskCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                finishedTaskCount.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getFinishedTasks()));
                metricSet.addMetric(finishedTaskCount);
        }

        if (wupMetricsData.getFinalisedTasks() >= 0) {
                PetasosComponentMetric finalisedTasks = new PetasosComponentMetric();
                finalisedTasks.setMetricAgent(getProcessingPlant().getComponentId());
                finalisedTasks.setMetricSource(wupMetricsData.getComponentID());
                finalisedTasks.setMetricName(PetasosComponentMetricTypeEnum.FINALISED_TASK_COUNT.getDisplayName());
                finalisedTasks.setMetricType(PetasosComponentMetricTypeEnum.FINALISED_TASK_COUNT);
                finalisedTasks.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                finalisedTasks.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getFinalisedTasks()));
                metricSet.addMetric(finalisedTasks);
        }

        if (wupMetricsData.getFailedTasks() >= 0) {
                PetasosComponentMetric finalisedTasks = new PetasosComponentMetric();
                finalisedTasks.setMetricAgent(getProcessingPlant().getComponentId());
                finalisedTasks.setMetricSource(wupMetricsData.getComponentID());
                finalisedTasks.setMetricName(PetasosComponentMetricTypeEnum.FAILED_TASK_COUNT.getDisplayName());
                finalisedTasks.setMetricType(PetasosComponentMetricTypeEnum.FAILED_TASK_COUNT);
                finalisedTasks.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                finalisedTasks.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getFailedTasks()));
                metricSet.addMetric(finalisedTasks);
        }

        if (wupMetricsData.getCurrentActionableTask() != null) {
                PetasosComponentMetric currentActionalTask = new PetasosComponentMetric();
                currentActionalTask.setMetricAgent(getProcessingPlant().getComponentId());
                currentActionalTask.setMetricSource(wupMetricsData.getComponentID());
                currentActionalTask.setMetricName("Current-Task");
                currentActionalTask.setMetricType(PetasosComponentMetricTypeEnum.CONTEXTUAL);
                currentActionalTask.setMetricUnit(PetasosComponentMetricUnitEnum.STRING_DESCRIPTION);
                currentActionalTask.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getCurrentActionableTask().toString()));
                metricSet.addMetric(currentActionalTask);
        }

        if (wupMetricsData.getLastActionableTask() != null) {
                PetasosComponentMetric lastActionalTask = new PetasosComponentMetric();
                lastActionalTask.setMetricAgent(getProcessingPlant().getComponentId());
                lastActionalTask.setMetricSource(wupMetricsData.getComponentID());
                lastActionalTask.setMetricName(PetasosComponentMetricTypeEnum.LAST_TASK_ID.getDisplayName());
                lastActionalTask.setMetricType(PetasosComponentMetricTypeEnum.LAST_TASK_ID);
                lastActionalTask.setMetricUnit(PetasosComponentMetricUnitEnum.STRING_DESCRIPTION);
                lastActionalTask.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getLastActionableTask().toString()));
                metricSet.addMetric(lastActionalTask);
        }

        if (wupMetricsData.getLastActivityInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(wupMetricsData.getComponentID());
                activityInstant.setMetricName(PetasosComponentMetricTypeEnum.LAST_TASK_INSTANT.getDisplayName());
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.LAST_TASK_INSTANT);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant.setMetricValue(new PetasosComponentMetricValue(wupMetricsData.getLastActivityInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (wupMetricsData.getLastEventProcessingDuration() > 0) {
                PetasosComponentMetric lastEventProcessingDuration = new PetasosComponentMetric();
                lastEventProcessingDuration.setMetricAgent(getProcessingPlant().getComponentId());
                lastEventProcessingDuration.setMetricSource(wupMetricsData.getComponentID());
                lastEventProcessingDuration.setMetricName(PetasosComponentMetricTypeEnum.LAST_TASK_PROCESSING_TIME.getDisplayName());
                lastEventProcessingDuration.setMetricType(PetasosComponentMetricTypeEnum.LAST_TASK_PROCESSING_TIME);
                lastEventProcessingDuration.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_DURATION_MILLISECONDS);
                String roundedValue = String.format("%.3f", (wupMetricsData.getLastEventProcessingDuration() + 0.005D)) + " sec";
                lastEventProcessingDuration.setMetricValue(new PetasosComponentMetricValue(roundedValue));
                metricSet.addMetric(lastEventProcessingDuration);
        }

        return (metricSet);
    }

    public PetasosComponentMetricSet convertEndpointMetricsData(EndpointMetricsData endpointMetricsData) {
        getLogger().debug(".convertEndpointMetricsData(): Entry, endpointMetricsData->{}", endpointMetricsData);

        if (endpointMetricsData.getComponentID() == null) {
                getLogger().debug(".convertEndpointMetricsData(): Exit, endpointMetricsData is null, returning -null-");
                return (null);
        }

        PetasosComponentMetricSet metricSet = new PetasosComponentMetricSet();
        metricSet.setMetricSourceComponentId(endpointMetricsData.getComponentID());
        metricSet.setSourceParticipantName(endpointMetricsData.getParticipantName());
        metricSet.setComponentType(endpointMetricsData.getComponentType());

        PetasosComponentMetricSet commonMetrics = convertCommonMetrics(endpointMetricsData);
        Set<String> commonMetricsKeys = commonMetrics.getMetrics().keySet();
        for (String currentMetric : commonMetricsKeys) {
                metricSet.addMetric(commonMetrics.getMetric(currentMetric));
        }

        if (endpointMetricsData.getEgressSendAttemptCount() > 0) {
                PetasosComponentMetric componentStatusMetric = new PetasosComponentMetric();
                componentStatusMetric.setMetricAgent(getProcessingPlant().getComponentId());
                componentStatusMetric.setMetricSource(endpointMetricsData.getComponentID());
                componentStatusMetric.setMetricName("Egress-Send-Attempt-Count");
                componentStatusMetric.setMetricType(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_ATTEMPTS);
                componentStatusMetric.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                componentStatusMetric.setMetricValue(new PetasosComponentMetricValue(endpointMetricsData.getEgressSendAttemptCount()));
                metricSet.addMetric(componentStatusMetric);
        }

        if (endpointMetricsData.getComponentStatus() != null) {
                PetasosComponentMetric componentStatusMetric = new PetasosComponentMetric();
                componentStatusMetric.setMetricAgent(getProcessingPlant().getComponentId());
                componentStatusMetric.setMetricSource(endpointMetricsData.getComponentID());
                componentStatusMetric.setMetricName("ProcessingPlant-Status");
                componentStatusMetric.setMetricType(PetasosComponentMetricTypeEnum.COMPONENT_GENERAL_STATUS);
                componentStatusMetric.setMetricUnit(PetasosComponentMetricUnitEnum.STRING_DESCRIPTION);
                componentStatusMetric.setMetricValue(new PetasosComponentMetricValue(endpointMetricsData.getComponentStatus()));
                metricSet.addMetric(componentStatusMetric);
        }

        if (endpointMetricsData.getLastActivityInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(endpointMetricsData.getComponentID());
                activityInstant.setMetricName("Last-Activity-Instant");
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.ACTIVITY_TIMESTAMP);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant.setMetricValue(new PetasosComponentMetricValue(endpointMetricsData.getLastActivityInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (endpointMetricsData.getRemoteProcedureCallCount() > 0) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(endpointMetricsData.getComponentID());
                activityInstant.setMetricName(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_REQUEST.getDisplayName());
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_REQUEST);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                activityInstant.setMetricValue(new PetasosComponentMetricValue(endpointMetricsData.getRemoteProcedureCallCount()));
                metricSet.addMetric(activityInstant);
        }

        if (endpointMetricsData.getRemoteProcedureCallFailureCount() > 0) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(endpointMetricsData.getComponentID());
                activityInstant.setMetricName(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_FAILURE.getDisplayName());
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_FAILURE);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                activityInstant.setMetricValue(new PetasosComponentMetricValue(endpointMetricsData.getRemoteProcedureCallFailureCount()));
                metricSet.addMetric(activityInstant);
        }

        if (endpointMetricsData.getRemoteProcedureCallHandledCount() > 0) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(endpointMetricsData.getComponentID());
                activityInstant.setMetricName(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_RESPONSE.getDisplayName());
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_RESPONSE);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                activityInstant.setMetricValue(
                                new PetasosComponentMetricValue(endpointMetricsData.getRemoteProcedureCallHandledCount()));
                metricSet.addMetric(activityInstant);
        }

        getLogger().trace(".convertEndpointMetricsData(): [RemoteProcedureCallResponseMap] Check");
        if (!endpointMetricsData.getRemoteProcedureCallResponsesMap().isEmpty()) {
                getLogger().trace(".convertEndpointMetricsData(): [Processing RemoteProcedureCallResponseMap] Start");
                int counter = 0;
                for (String currentTargetName : endpointMetricsData.getInternalDistributionCountMap().keySet()) {
                        PetasosComponentMetric rpcHandledMetric = new PetasosComponentMetric();
                        rpcHandledMetric.setMetricAgent(getProcessingPlant().getComponentId());
                        rpcHandledMetric.setMetricSource(endpointMetricsData.getComponentID());
                        rpcHandledMetric.setMetricName("RPC-HandledFrom[" + currentTargetName + "]");
                        rpcHandledMetric.setMetricType(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_RESPONSE);
                        rpcHandledMetric.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                        Integer rpcRequestsHandled = endpointMetricsData.getRemoteProcedureCallResponsesMap().get(currentTargetName);
                        rpcHandledMetric.setMetricValue(new PetasosComponentMetricValue(rpcRequestsHandled));
                        metricSet.addMetric(rpcHandledMetric);
                        counter += 1;
                }
                getLogger().trace(
                                ".convertEndpointMetricsData(): [Processing RemoteProcedureCallResponseMap] Finish (entries={})",
                                counter);
        }
        getLogger().trace(".convertEndpointMetricsData(): [RemoteProcedureCallResponseMap] Done...");

        getLogger().trace(".convertEndpointMetricsData(): [RemoteProcedureCallRequestMap] Check");
        if (!endpointMetricsData.getRemoteProcedureCallRequestsMap().isEmpty()) {
                getLogger().trace(".convertEndpointMetricsData(): [Processing RemoteProcedureCallRequestMap] Start");
                int counter = 0;
                for (String currentTargetName : endpointMetricsData.getRemoteProcedureCallRequestsMap().keySet()) {
                        PetasosComponentMetric rpcHandledMetric = new PetasosComponentMetric();
                        rpcHandledMetric.setMetricAgent(getProcessingPlant().getComponentId());
                        rpcHandledMetric.setMetricSource(endpointMetricsData.getComponentID());
                        rpcHandledMetric.setMetricName("RPC-RequestsTo[" + currentTargetName + "]");
                        rpcHandledMetric.setMetricType(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_REQUEST);
                        rpcHandledMetric.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                        Integer rpcRequestsHandled = endpointMetricsData.getRemoteProcedureCallRequestsMap().get(currentTargetName);
                        rpcHandledMetric.setMetricValue(new PetasosComponentMetricValue(rpcRequestsHandled));
                        metricSet.addMetric(rpcHandledMetric);
                        counter += 1;
                }
                getLogger().trace(
                                ".convertEndpointMetricsData(): [Processing RemoteProcedureCallRequestMap] Finish (entries={})",
                                counter);
        }
        getLogger().trace(".convertEndpointMetricsData(): [RemoteProcedureCallRequestMap] Done...");

        getLogger().trace(".convertEndpointMetricsData(): [RemoteProcedureCallFailuresMap] Check");
        if (!endpointMetricsData.getRemoteProcedureCallFailuresMap().isEmpty()) {
                getLogger().trace(".convertEndpointMetricsData(): [Processing RemoteProcedureCallFailuresMap] Start");
                int counter = 0;
                for (String currentTargetName : endpointMetricsData.getRemoteProcedureCallFailuresMap().keySet()) {
                        PetasosComponentMetric rpcHandledMetric = new PetasosComponentMetric();
                        rpcHandledMetric.setMetricAgent(getProcessingPlant().getComponentId());
                        rpcHandledMetric.setMetricSource(endpointMetricsData.getComponentID());
                        rpcHandledMetric.setMetricName("RPC-Failures[" + currentTargetName + "]");
                        rpcHandledMetric.setMetricType(PetasosComponentMetricTypeEnum.INTER_SUBSYSTEM_RPC_FAILURE);
                        rpcHandledMetric.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                        Integer rpcRequestsHandled = endpointMetricsData.getRemoteProcedureCallFailuresMap()
                                        .get(currentTargetName);
                        rpcHandledMetric.setMetricValue(new PetasosComponentMetricValue(rpcRequestsHandled));
                        metricSet.addMetric(rpcHandledMetric);
                        counter += 1;
                }
                getLogger().trace(
                                ".convertEndpointMetricsData(): [Processing RemoteProcedureCallFailuresMap] Finish (entries={})",
                                counter);
        }
        getLogger().trace(".convertEndpointMetricsData(): [RemoteProcedureCallFailuresMap] Done...");

        getLogger().debug(".convertEndpointMetricsData(): Exit, metricSet->{}", metricSet);
        return (metricSet);
    }

    protected PetasosComponentMetricSet convertCommonMetrics(CommonComponentMetricsData metricsData) {
        getLogger().debug(".convertCommonMetrics(): Entry, metricsData->{}", metricsData);

        if (metricsData.getComponentID() == null) {
                getLogger().debug(".convertCommonMetrics(): Exit, endpointMetricsData is null, returning -null-");
                return (null);
        }

        PetasosComponentMetricSet metricSet = new PetasosComponentMetricSet();
        metricSet.setMetricSourceComponentId(metricsData.getComponentID());
        metricSet.setSourceParticipantName(metricsData.getParticipantName());
        metricSet.setComponentType(metricsData.getComponentType());

        if (metricsData.getComponentStatus() != null) {
                PetasosComponentMetric componentStatusMetric = new PetasosComponentMetric();
                componentStatusMetric
                                .setMetricAgent(getProcessingPlant().getComponentId());
                componentStatusMetric.setMetricSource(metricsData.getComponentID());
                componentStatusMetric.setMetricName("ProcessingPlant-Status");
                componentStatusMetric.setMetricType(PetasosComponentMetricTypeEnum.COMPONENT_GENERAL_STATUS);
                componentStatusMetric.setMetricUnit(PetasosComponentMetricUnitEnum.STRING_DESCRIPTION);
                componentStatusMetric.setMetricValue(new PetasosComponentMetricValue(metricsData.getComponentStatus()));
                metricSet.addMetric(componentStatusMetric);
        }

        if (metricsData.getLastActivityInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(metricsData.getComponentID());
                activityInstant.setMetricName("Last-Activity-Instant");
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.ACTIVITY_TIMESTAMP);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant.setMetricValue(new PetasosComponentMetricValue(metricsData.getLastActivityInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (metricsData.getComponentStartupInstant() != null) {
                PetasosComponentMetric activityInstant = new PetasosComponentMetric();
                activityInstant.setMetricAgent(getProcessingPlant().getComponentId());
                activityInstant.setMetricSource(metricsData.getComponentID());
                activityInstant.setMetricName("Component-Startup-Instant");
                activityInstant.setMetricType(PetasosComponentMetricTypeEnum.ACTIVITY_TIMESTAMP);
                activityInstant.setMetricUnit(PetasosComponentMetricUnitEnum.TIME_INSTANT);
                activityInstant.setMetricValue(new PetasosComponentMetricValue(metricsData.getComponentStartupInstant()));
                metricSet.addMetric(activityInstant);
        }

        if (metricsData.getEgressMessageFailureCount() > 0) {
                PetasosComponentMetric ingresMessageCount = new PetasosComponentMetric();
                ingresMessageCount.setMetricAgent(getProcessingPlant().getComponentId());
                ingresMessageCount.setMetricSource(metricsData.getComponentID());
                ingresMessageCount.setMetricName(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_FAILURES.getDisplayName());
                ingresMessageCount.setMetricType(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_FAILURES);
                ingresMessageCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                ingresMessageCount.setMetricValue(new PetasosComponentMetricValue(metricsData.getEgressMessageFailureCount()));
                metricSet.addMetric(ingresMessageCount);
        }

        if (metricsData.getEgressMessageSuccessCount() > 0) {
                PetasosComponentMetric ingresMessageCount = new PetasosComponentMetric();
                ingresMessageCount.setMetricAgent(getProcessingPlant().getComponentId());
                ingresMessageCount.setMetricSource(metricsData.getComponentID());
                ingresMessageCount.setMetricName(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_SUCCESSFUL.getDisplayName());
                ingresMessageCount.setMetricType(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_SUCCESSFUL);
                ingresMessageCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                ingresMessageCount.setMetricValue(new PetasosComponentMetricValue(metricsData.getEgressMessageSuccessCount()));
                metricSet.addMetric(ingresMessageCount);
        }

        if (metricsData.getIngresMessageCount() > 0) {
                PetasosComponentMetric ingresMessageCount = new PetasosComponentMetric();
                ingresMessageCount.setMetricAgent(getProcessingPlant().getComponentId());
                ingresMessageCount.setMetricSource(metricsData.getComponentID());
                ingresMessageCount.setMetricName(PetasosComponentMetricTypeEnum.INGRES_MESSAGES.getDisplayName());
                ingresMessageCount.setMetricType(PetasosComponentMetricTypeEnum.INGRES_MESSAGES);
                ingresMessageCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                ingresMessageCount.setMetricValue(new PetasosComponentMetricValue(metricsData.getIngresMessageCount()));
                metricSet.addMetric(ingresMessageCount);
        }

        if (metricsData.getEgressMessageAttemptCount() > 0) {
                PetasosComponentMetric egressMessageCount = new PetasosComponentMetric();
                egressMessageCount.setMetricAgent(getProcessingPlant().getComponentId());
                egressMessageCount.setMetricSource(metricsData.getComponentID());
                egressMessageCount.setMetricName(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_ATTEMPTS.getDisplayName());
                egressMessageCount.setMetricType(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_ATTEMPTS);
                egressMessageCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                egressMessageCount.setMetricValue(new PetasosComponentMetricValue(metricsData.getEgressMessageAttemptCount()));
                metricSet.addMetric(egressMessageCount);
        }

        if (metricsData.getInternalDistributedMessageCount() > 0) {
                PetasosComponentMetric egressMessageCount = new PetasosComponentMetric();
                egressMessageCount.setMetricAgent(getProcessingPlant().getComponentId());
                egressMessageCount.setMetricSource(metricsData.getComponentID());
                egressMessageCount.setMetricName(PetasosComponentMetricTypeEnum.INTERNAL_MESSAGE_FORWARDING.getDisplayName());
                egressMessageCount.setMetricType(PetasosComponentMetricTypeEnum.INTERNAL_MESSAGE_FORWARDING);
                egressMessageCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                egressMessageCount.setMetricValue(new PetasosComponentMetricValue(metricsData.getInternalDistributedMessageCount()));
                metricSet.addMetric(egressMessageCount);
        }

        if (metricsData.getInternalReceivedMessageCount() > 0) {
                PetasosComponentMetric egressMessageCount = new PetasosComponentMetric();
                egressMessageCount.setMetricAgent(getProcessingPlant().getComponentId());
                egressMessageCount.setMetricSource(metricsData.getComponentID());
                egressMessageCount.setMetricName(PetasosComponentMetricTypeEnum.INTERNAL_MESSAGE_RECEPTION.getDisplayName());
                egressMessageCount.setMetricType(PetasosComponentMetricTypeEnum.INTERNAL_MESSAGE_RECEPTION);
                egressMessageCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                egressMessageCount.setMetricValue(new PetasosComponentMetricValue(metricsData.getInternalReceivedMessageCount()));
                metricSet.addMetric(egressMessageCount);
        }

        getLogger().trace(".convertCommonMetrics(): [DistributionCountMap] Check");
        if (!metricsData.getInternalDistributionCountMap().isEmpty()) {
                getLogger().trace(".convertCommonMetrics(): [Processing DistributionCountMap] Start");
                int counter = 0;
                for (String currentTargetName : metricsData.getInternalDistributionCountMap().keySet()) {
                        PetasosComponentMetric egressMessageCount = new PetasosComponentMetric();
                        egressMessageCount
                                        .setMetricAgent(getProcessingPlant().getComponentId());
                        egressMessageCount.setMetricSource(metricsData.getComponentID());
                        egressMessageCount.setMetricName("Messages-Forwarded[" + currentTargetName + "]");
                        egressMessageCount.setMetricType(PetasosComponentMetricTypeEnum.EGRESS_MESSAGES_FORWARD_ATTEMPTS);
                        egressMessageCount.setMetricUnit(PetasosComponentMetricUnitEnum.INTEGER_COUNT);
                        Integer forwardedMessageCount = metricsData.getInternalDistributionCountMap().get(currentTargetName);
                        egressMessageCount.setMetricValue(new PetasosComponentMetricValue(forwardedMessageCount));
                        metricSet.addMetric(egressMessageCount);
                        counter += 1;
                }
                getLogger().trace(".convertCommonMetrics(): [Processing DistributionCountMap] Finish (entries={})",
                                counter);
        }
        getLogger().trace(".convertCommonMetrics(): [DistributionCountMap] Done...");

        getLogger().debug(".convertCommonMetrics(): Exit, metricSet->{}", metricSet);
        return (metricSet);

    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger() {
            return (LOG);
    }
}
