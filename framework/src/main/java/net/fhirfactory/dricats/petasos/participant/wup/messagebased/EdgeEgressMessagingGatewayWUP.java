/*
 * Copyright (c) 2020 MAHun
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

import net.fhirfactory.dricats.model.petasos.ipc.PegacornCommonInterfaceNames;
import net.fhirfactory.dricats.model.petasos.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.dricats.petasos.participant.wup.base.GenericMessageBasedWUPTemplate;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.adapters.base.IPCAdapterDefinition;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.base.IPCServerTopologyEndpoint;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.base.IPCTopologyEndpoint;
import net.fhirfactory.pegacorn.petasos.participant.wup.GenericMessageBasedWUPTemplate;
import net.fhirfactory.pegacorn.petasos.participant.wup.MessageBasedWUPEndpointContainer;

import javax.inject.Inject;

public abstract class EdgeEgressMessagingGatewayWUP extends GenericMessageBasedWUPTemplate {
    private IPCTopologyEndpoint associatedEgressTopologyEndpoint;

    public EdgeEgressMessagingGatewayWUP() {
        super();
    }

    protected abstract String specifyEgressInterfaceName();
    protected abstract IPCAdapterDefinition specifyEgressInterfaceDefinition();

    @Inject
    private PegacornCommonInterfaceNames interfaceNames;

    protected PegacornCommonInterfaceNames getInterfaceNames(){
        return(interfaceNames);
    }

    @Override
    protected WUPArchetypeEnum specifyWUPArchetype(){
        return(WUPArchetypeEnum.WUP_NATURE_MESSAGE_EXTERNAL_EGRESS_POINT);
    }

    protected MessageBasedWUPEndpointContainer establishIngresEndpoint(){
        getLogger().debug(".specifyIngresTopologyEndpoint(): Entry");
        MessageBasedWUPEndpointContainer ingresEndpoint = new MessageBasedWUPEndpointContainer();
        ingresEndpoint.setFrameworkEnabled(true);
        ingresEndpoint.setEndpointSpecification(this.getNameSet().getEndPointWUPIngres());
        getLogger().debug(".specifyIngresTopologyEndpoint(): Exit");
        return(ingresEndpoint);
    }

    @Override
    protected boolean getUsesWUPFrameworkGeneratedEgressEndpoint() {
        return(false);
    }

    /**
     * Derive the Ingres Topology Endpoint
     */
    protected void assignEgressTopologyEndpoint(){
        getLogger().debug(".assignIngresTopologyEndpoint(): Entry");
        IPCTopologyEndpoint endpoint = deriveAssociatedTopologyEndpoint(specifyEgressInterfaceName(), specifyEgressInterfaceDefinition());
        if(endpoint != null){
            this.associatedEgressTopologyEndpoint = endpoint;
        } else {
            throw(new RuntimeException("Cannot resolve appropriate Ingres Interface to bind to!"));
        }
        getLogger().debug(".assignIngresTopologyEndpoint(): Exit, endpoint->{}", endpoint);
    }

    public IPCTopologyEndpoint getAssociatedEgressTopologyEndpoint() {
        return associatedEgressTopologyEndpoint;
    }

    public void setAssociatedEgressTopologyEndpoint(IPCServerTopologyEndpoint associatedEgressTopologyEndpoint) {
        this.associatedEgressTopologyEndpoint = associatedEgressTopologyEndpoint;
    }

}
