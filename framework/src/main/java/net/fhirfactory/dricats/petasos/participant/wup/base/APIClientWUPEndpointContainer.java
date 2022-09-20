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
package net.fhirfactory.dricats.petasos.participant.wup.base;

import net.fhirfactory.pegacorn.core.interfaces.topology.PetasosEndpointContainerInterface;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.base.IPCClientTopologyEndpoint;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.base.IPCTopologyEndpoint;

import java.io.Serializable;

public abstract class APIClientWUPEndpointContainer implements Serializable, PetasosEndpointContainerInterface {
    private IPCClientTopologyEndpoint endpointTopologyNode;
    private String egressMessagingInjectPoint;
    private String ingresMessagingInjectPoint;
    private String endpointSpecification;
    private boolean frameworkEnabled;

    //
    // Constructor(s)
    //

    public APIClientWUPEndpointContainer(){
        this.endpointSpecification = null;
        this.ingresMessagingInjectPoint = null;
        this.egressMessagingInjectPoint = null;
        this.endpointTopologyNode = null;
        this.frameworkEnabled = false;
    }

    //
    // Getters and Setters
    //

    @Override
    public IPCTopologyEndpoint getEndpointTopologyNode() {
        return endpointTopologyNode;
    }

    public void setEndpointTopologyNode(IPCClientTopologyEndpoint endpointTopologyNode) {
        this.endpointTopologyNode = endpointTopologyNode;
    }

    @Override
    public String getEndpointSpecification() {
        return endpointSpecification;
    }

    public void setEndpointSpecification(String endpointSpecification) {
        this.endpointSpecification = endpointSpecification;
    }

    public String getEgressMessagingInjectPoint() {
        return egressMessagingInjectPoint;
    }

    public void setEgressMessagingInjectPoint(String egressMessagingInjectPoint) {
        this.egressMessagingInjectPoint = egressMessagingInjectPoint;
    }

    public String getIngresMessagingInjectPoint() {
        return ingresMessagingInjectPoint;
    }

    public void setIngresMessagingInjectPoint(String ingresMessagingInjectPoint) {
        this.ingresMessagingInjectPoint = ingresMessagingInjectPoint;
    }

    @Override
    public boolean isFrameworkEnabled() {
        return frameworkEnabled;
    }

    public void setFrameworkEnabled(boolean frameworkEnabled) {
        this.frameworkEnabled = frameworkEnabled;
    }

    //
    // To String
    //

    @Override
    public String toString() {
        return "MatrixAPIClientEndpointContainer{" +
                "endpointTopologyNode=" + endpointTopologyNode +
                ", egressMessagingInjectPoint='" + egressMessagingInjectPoint + '\'' +
                ", ingresMessagingInjectPoint='" + ingresMessagingInjectPoint + '\'' +
                ", endpointSpecification='" + endpointSpecification + '\'' +
                ", isFrameworkEnabled=" + isFrameworkEnabled() +
                '}';
    }
}
