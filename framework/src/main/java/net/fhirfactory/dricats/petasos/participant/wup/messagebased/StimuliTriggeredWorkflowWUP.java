/*
 * Copyright (c) 2020 Mark A. Hunter
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
package net.fhirfactory.dricats.petasos.participant.wup.messagebased;

import net.fhirfactory.pegacorn.petasos.participant.wup.MessageBasedWUPEndpointContainer;
import net.fhirfactory.pegacorn.petasos.participant.wup.GenericMessageBasedWUPTemplate;
import net.fhirfactory.dricats.model.petasos.wup.valuesets.WUPArchetypeEnum;

public abstract class StimuliTriggeredWorkflowWUP extends GenericMessageBasedWUPTemplate {

    public StimuliTriggeredWorkflowWUP() {
        super();
    }
    
    @Override
    protected WUPArchetypeEnum specifyWUPArchetype(){
        return(WUPArchetypeEnum.WUP_NATURE_STIMULI_TRIGGERED_WORKFLOW);
    }

    @Override
    protected MessageBasedWUPEndpointContainer specifyEgressEndpoint(){
        getLogger().debug(".specifyEgressTopologyEndpoint(): Entry");
        MessageBasedWUPEndpointContainer egressEndpoint = new MessageBasedWUPEndpointContainer();
        egressEndpoint.setFrameworkEnabled(true);
        egressEndpoint.setEndpointSpecification(this.getNameSet().getEndPointWUPEgress());
        getLogger().debug(".specifyEgressTopologyEndpoint(): Exit");
        return(egressEndpoint);
    }

    protected MessageBasedWUPEndpointContainer establishIngresEndpoint(){
        getLogger().debug(".specifyIngresTopologyEndpoint(): Entry");
        MessageBasedWUPEndpointContainer ingressEndpoint = new MessageBasedWUPEndpointContainer();
        ingressEndpoint.setFrameworkEnabled(true);
        ingressEndpoint.setEndpointSpecification(this.getNameSet().getEndPointWUPIngres());
        getLogger().debug(".specifyIngresTopologyEndpoint(): Exit");
        return(ingressEndpoint);
    }
}
