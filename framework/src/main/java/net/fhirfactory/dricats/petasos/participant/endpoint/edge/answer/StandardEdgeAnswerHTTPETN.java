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
package net.fhirfactory.dricats.petasos.participant.endpoint.edge.answer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.model.topology.endpoints.base.SocketBasedServerETN;
import net.fhirfactory.dricats.model.topology.endpoints.base.valuesets.EndpointTopologyNodeTypeEnum;
import net.fhirfactory.dricats.model.topology.endpoints.http.adapters.HTTPServerAdapter;

public class StandardEdgeAnswerHTTPETN extends SocketBasedServerETN {

    //
    // Constructor(s)
    //

    public StandardEdgeAnswerHTTPETN(){
        super();
        setEndpointType(EndpointTopologyNodeTypeEnum.HTTP_API_SERVER);
//        setComponentSystemRole(SoftwareComponentConnectivityContextEnum.COMPONENT_ROLE_SUBSYSTEM_EDGE);
    }

    //
    // Getters and Setters
    //

    @JsonIgnore
    public HTTPServerAdapter getHTTPServerAdapter(){
        if(getAdapterList().isEmpty()){
            return(null);
        }
        HTTPServerAdapter mllpServer = (HTTPServerAdapter) getAdapterList().get(0);
        return(mllpServer);
    }

    @JsonIgnore
    public void setHTTPServerAdapter(HTTPServerAdapter httpServer){
        if(httpServer != null){
            getAdapterList().add(httpServer);
        }
    }

    //
    // To String
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StandardEdgeAnswerHTTPEndpoint{");
        sb.append(", ").append(super.toString()).append("}");
        return sb.toString();
    }

    

}
