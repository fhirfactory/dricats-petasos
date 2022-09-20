/*
 * Copyright (c) 2020 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.participant.wup.framework;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.interfaces.topology.PegacornTopologyFactoryInterface;
import net.fhirfactory.dricats.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workunitprocessors.base.WorkUnitProcessorTopologyNode;
import net.fhirfactory.dricats.petasos.participant.wup.base.UnmonitoredWorkUnitProcessorBase;
import net.fhirfactory.pegacorn.core.model.componentid.PegacornSystemComponentTypeTypeEnum;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.pegacorn.deployment.topology.manager.TopologyIM;
import net.fhirfactory.dricats.petasos.participant.workshops.base.PetasosEnablerWorkshop;
import net.fhirfactory.dricats.petasos.participant.solution.LocalSolution;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class OAMWorkUnitProcessor extends UnmonitoredWorkUnitProcessorBase {

    private WorkUnitProcessorTopologyNode wupTopologyNode;
    private boolean isInitialised;

    @Inject
    private LocalSolution topologyIM;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlant;

    //
    // Constructor(s)
    //

    public OAMWorkUnitProcessor() {
        super();
        this.isInitialised = false;
    }

    //
    // Getters and Setters
    //

    public WorkUnitProcessorTopologyNode getWUPTopologyNode(){
        return(this.wupTopologyNode);
    }

    protected Logger getLogger() {return(specifyLogger());}

    protected ProcessingPlantConfigurationServiceInterface getProcessingPlant(){
        return(processingPlant);
    }

    public PegacornTopologyFactoryInterface getTopologyFactory(){
        return(processingPlant.getTopologyFactory());
    }

    public PetasosEnablerWorkshop getWorkshop(){
        return(specifyOAMWorkshop());
    }

    //
    // Abstract Methods
    //

    abstract protected String specifyOAMWUPName();
    abstract protected String specifyOAMWUPVersion();
    abstract protected PetasosEnablerWorkshop specifyOAMWorkshop();
    abstract protected void invokePostConstructInitialisation();
    abstract protected Logger specifyLogger();

    //
    // Post Construct
    //

    @PostConstruct
    private void initialise() {
        if (!isInitialised) {
            getLogger().debug("StandardWorkshop::initialise(): Invoked!");
            processingPlant.initialisePlant();
            buildOAMWorkUnitProcessor();
            invokePostConstructInitialisation();
            getLogger().trace("StandardWorkshop::initialise(): Node --> {}", getWUPTopologyNode());
            isInitialised = true;
        }
    }

    //
    // Business Methods
    //

    public void initialiseOAMWorkUnitProcessor(){
        initialise();
    }

    private void buildOAMWorkUnitProcessor() {
        getLogger().debug(".buildOAMWorkUnitProcessor(): Entry, adding Workshop --> {}, version --> {}", specifyOAMWUPName(), specifyOAMWUPVersion());
        String participantName = getWorkshop().getWorkshopPetasosParticipant().getParticipantName() + "." + specifyOAMWUPName();
        WorkUnitProcessorTopologyNode wup = getTopologyFactory().createWorkUnitProcessor(specifyOAMWUPName(), specifyOAMWUPVersion(), participantName, specifyOAMWorkshop().getWorkshopPetasosParticipant(), DricatsSoftwareComponentTypeEnum.OAM_WORK_UNIT_PROCESSOR);
        topologyIM.addTopologyNode(specifyOAMWorkshop().getWorkshopPetasosParticipant().getComponentFDN(), wup);
        this.wupTopologyNode = wup;
        getLogger().debug(".buildOAMWorkUnitProcessor(): Exit");
    }

    public boolean isInitialised() {
        return isInitialised;
    }

    public void setInitialised(boolean initialised) {
        isInitialised = initialised;
    }

    private String getFriendlyName(){
        String nodeName = getWUPTopologyNode().getComponentRDN().getNodeName() + "(" + getWUPTopologyNode().getComponentRDN().getNodeVersion() + ")";
        return(nodeName);
    }
}
