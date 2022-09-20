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
package net.fhirfactory.dricats.petasos.participant.endpoint.interact;

import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.participant.components.endpoint.ConnectedExternalSystem;
import net.fhirfactory.dricats.model.petasos.participant.components.endpoint.SocketBasedClientEndpoint;

public abstract class StandardInteractSocketBasedClient extends SocketBasedClientEndpoint {

    private ConnectedExternalSystem targetSystem;
    private PetasosParticipantId  endpointParticipantId;
    
    //
    // Constructor(s)
    //

    public StandardInteractSocketBasedClient(){
        targetSystem = new ConnectedExternalSystem();
    }
    
    //
    // Getters and Setters
    //

    public ConnectedExternalSystem getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(ConnectedExternalSystem targetSystem) {
        this.targetSystem = targetSystem;
    }

    @Override
    public PetasosParticipantId getEndpointParticipantId() {
        return endpointParticipantId;
    }

    public void setEndpointParticipantId(PetasosParticipantId endpointParticipantId){
        this.endpointParticipantId = endpointParticipantId;
    }

    //
    // toString()
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StandardInteractClientTopologyEndpointPort{");
        sb.append("targetSystem=").append(targetSystem);
        sb.append(", ").append(super.toString()).append('}');
        return sb.toString();
    }

}
