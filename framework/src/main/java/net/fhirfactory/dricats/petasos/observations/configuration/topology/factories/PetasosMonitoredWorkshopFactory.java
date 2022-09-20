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

import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCWorkUnitProcessorSummary;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCWorkshopSummary;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workshops.base.WorkshopTopologyNodeBase;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workunitprocessors.base.WorkUnitProcessorTopologyNode;
import net.fhirfactory.dricats.petasos.observations.configuration.topology.factories.common.PetasosMonitoredComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosMonitoredWorkshopFactory extends PetasosMonitoredComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosMonitoredWorkshopFactory.class);

    @Inject
    private PetasosMonitoredWorkUnitProcessorFactory wupFactory;

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    public ISCWorkshopSummary newWorkshop(WorkshopTopologyNodeBase workshopNode){
        getLogger().debug(".newWorkshop(): Entry, workshopNode->{}", workshopNode);
        ISCWorkshopSummary workshop = new ISCWorkshopSummary();
        workshop = (ISCWorkshopSummary) newPetasosMonitoredComponent(workshop, workshopNode);
        for(WorkUnitProcessorTopologyNode currentWUPTopologyNode: workshopNode.getWUPSet()){
            ISCWorkUnitProcessorSummary currentWUP = wupFactory.newWorkUnitProcessor(workshop.getName(), currentWUPTopologyNode);
            workshop.addWorkUnitProcessor(currentWUP);
        }
        getLogger().debug(".newWorkshop(): Exit, workshop->{}", workshop);
        return(workshop);
    }
}
