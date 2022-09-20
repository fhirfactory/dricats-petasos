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
package net.fhirfactory.dricats.petasos.tasking.observations.reporting.tasks.agents;

import net.fhirfactory.dricats.petasos.tasking.observations.reporting.tasks.PetasosTaskReportFactory;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.tasks.PetasosITOpsTaskReportingAgentInterface;
import net.fhirfactory.pegacorn.core.model.componentid.ComponentIdType;
import net.fhirfactory.pegacorn.core.model.petasos.oam.topology.valuesets.PetasosMonitoredComponentTypeEnum;

public class WorkUnitProcessorTaskReportAgent extends TaskReportAgentBase{

    public WorkUnitProcessorTaskReportAgent(ProcessingPlantRoleSupportInterface processingPlantFunction, String participantName, ComponentIdType componentId, PetasosTaskReportFactory factory, PetasosITOpsTaskReportingAgentInterface taskReportingAgent) {
        super(processingPlantFunction, participantName, componentId, factory, taskReportingAgent);
    }

    @Override
    protected PetasosMonitoredComponentTypeEnum specifyComponentType() {
        return PetasosMonitoredComponentTypeEnum.PETASOS_MONITORED_COMPONENT_WORK_UNIT_PROCESSOR;
    }
}
