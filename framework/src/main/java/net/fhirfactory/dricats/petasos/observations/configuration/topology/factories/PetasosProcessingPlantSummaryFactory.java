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
package net.fhirfactory.dricats.petasos.observations.configuration.topology.factories;

import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCProcessingPlantSummary;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCWorkshopSummary;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.processingplants.base.ProcessingPlantTopologyNode;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workshops.base.WorkshopTopologyNodeBase;
import net.fhirfactory.dricats.petasos.observations.configuration.topology.factories.common.PetasosMonitoredComponentFactory;
import net.fhirfactory.dricats.petasos.participant.solution.LocalSolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosProcessingPlantSummaryFactory extends PetasosMonitoredComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosProcessingPlantSummaryFactory.class);

    @Inject
    private PetasosMonitoredWorkshopFactory workshopFactory;

    @Inject
    private LocalSolution localSolutionMap;

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    public ISCProcessingPlantSummary newProcessingPlant(ProcessingPlantTopologyNode topologyNode){
        LOG.debug(".newProcessingPlant(): Entry, topologyNode->{}", topologyNode);
        ISCProcessingPlantSummary processingPlant = new ISCProcessingPlantSummary();
        processingPlant = (ISCProcessingPlantSummary) newPetasosMonitoredComponent(processingPlant, topologyNode);
        processingPlant.setSecurityZone(topologyNode.getNetworkZone().getDisplayName());
        processingPlant.setActualHostIP(topologyNode.getActualHostIP());
        processingPlant.setActualPodIP(topologyNode.getActualPodIP());
        processingPlant.setName(topologyNode.getSubsystemParticipantName());
        processingPlant.setDisplayName(topologyNode.getSubsystemParticipantName());
        processingPlant.setReplicationCount(topologyNode.getReplicationCount());
        for(WorkshopTopologyNodeBase currentWorkshop: topologyNode.getWorkshopSet()){
            if(currentWorkshop.getWUPMap().isEmpty()){
                // don't add it... it's pointless
            } else {
                ISCWorkshopSummary currentWorkshopSummary = workshopFactory.newWorkshop(currentWorkshop);
                processingPlant.addWorkshop(currentWorkshopSummary);
            }
        }
        LOG.debug(".newProcessingPlant(): Exit, processingPlant->{}", processingPlant);
        return(processingPlant);
    }
}
