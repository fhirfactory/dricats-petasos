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
package net.fhirfactory.dricats.petasos.participant.endpoint.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.model.topology.endpoints.base.adapters.IPCAdapter;
import net.fhirfactory.dricats.model.topology.endpoints.base.valuesets.EndpointTopologyNodeTypeEnum;
import net.fhirfactory.dricats.model.topology.endpoints.interact.StandardInteractSocketBasedClientETN;
import net.fhirfactory.dricats.petasos.participant.endpoint.http.adapters.HTTPClientAdapter;

import java.util.ArrayList;
import java.util.List;

public class HTTPClientETN extends StandardInteractSocketBasedClientETN {

    public HTTPClientETN(){
        super();
        setEndpointType(EndpointTopologyNodeTypeEnum.HTTP_API_CLIENT);
//        setComponentSystemRole(SoftwareComponentConnectivityContextEnum.COMPONENT_ROLE_INTERACT_EGRESS);
    }

    @JsonIgnore
    public List<HTTPClientAdapter> getHTTPClientAdapters() {
        List<HTTPClientAdapter> httpAdapterList = new ArrayList<>();
        for(IPCAdapter currentInterface: getAdapterList()){
            HTTPClientAdapter currentClientAdapter = (HTTPClientAdapter)currentInterface;
            httpAdapterList.add(currentClientAdapter);
        }
        return httpAdapterList;
    }

    @JsonIgnore
    public void setHTTPClientAdapters(List<HTTPClientAdapter> targetHTTPClientAdapters) {
        if(targetHTTPClientAdapters != null) {
            this.getAdapterList().clear();
            this.getAdapterList().addAll(targetHTTPClientAdapters);
        }
    }
    
        
    //
    // toString
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTPClientETN{");
        sb.append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
