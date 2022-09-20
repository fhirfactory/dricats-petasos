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
package net.fhirfactory.dricats.petasos.observations.metrics;

import net.fhirfactory.dricats.interfaces.observations.notifications.PetasosITOpsNotificationAgentInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.observations.topology.valuesets.PetasosMonitoredComponentTypeEnum;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.EndpointMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.ProcessingPlantMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.metrics.cache.PetasosLocalMetricsDM;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosMetricAgentFactory {

    @Inject
    private PetasosLocalMetricsDM localMetricsDM;

    @Inject
    private PetasosITOpsNotificationAgentInterface notificationAgent;

    public WorkUnitProcessorMetricsAgent newWorkUnitProcessingMetricsAgent(ProcessingPlantRoleSupportInterface processingPlantFunction, ComponentId componentId, String participantName){
        WorkUnitProcessorMetricsAgent wupMetricsAgent = new WorkUnitProcessorMetricsAgent(processingPlantFunction, localMetricsDM, componentId, participantName);
        wupMetricsAgent.getWUPMetricsData().setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR);
        wupMetricsAgent.setNotificationAgent(notificationAgent);
        wupMetricsAgent.registerWithCache();
        return(wupMetricsAgent);
    }

    public ProcessingPlantMetricsAgent newProcessingPlantMetricsAgent(ProcessingPlantRoleSupportInterface processingPlantFunction, ComponentId componentId, String participantName){
        ProcessingPlantMetricsAgent plantMetricsAgent = new ProcessingPlantMetricsAgent(processingPlantFunction, localMetricsDM, componentId, participantName);
        plantMetricsAgent.getProcessingPlantMetricsData().setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_PROCESSING_PLANT);
        plantMetricsAgent.registerWithCache();
        plantMetricsAgent.setNotificationAgent(notificationAgent);
        return(plantMetricsAgent);
    }
    public EndpointMetricsAgent newEndpointMetricsAgent(ProcessingPlantRoleSupportInterface processingPlantFunction, ComponentId componentId, String participantName, String connectedSystemName, String endpointDescription){
        EndpointMetricsAgent wupMetricsAgent = new EndpointMetricsAgent(processingPlantFunction, localMetricsDM, componentId, participantName, connectedSystemName, endpointDescription);
        wupMetricsAgent.getEndpointMetricsData().setComponentType(PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_ENDPOINT);
        wupMetricsAgent.setNotificationAgent(notificationAgent);
        wupMetricsAgent.registerWithCache();
        return(wupMetricsAgent);
    }

}
