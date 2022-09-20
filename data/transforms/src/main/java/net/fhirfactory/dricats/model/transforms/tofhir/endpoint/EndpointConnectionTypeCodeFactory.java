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
package net.fhirfactory.dricats.model.transforms.tofhir.endpoint;

import org.hl7.fhir.r4.model.Coding;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirfactory.dricats.configuration.defaults.dricats.systemwide.ReferenceProperties;
import net.fhirfactory.dricats.model.petasos.participant.components.endpoint.valuesets.EndpointTypeEnum;

@ApplicationScoped
public class EndpointConnectionTypeCodeFactory {

    private static final String PEGACORN_ENDPOINT_CONNECTION_TYPE_SYSTEM = "/endpoint-connection_type";

    @Inject
    private ReferenceProperties systemWideProperties;

    //
    // Business Methods
    //

    public String getPegacornEndpointConnectionTypeSystem(){
        String codeSystem = systemWideProperties.getPegacornCodeSystemSite() + PEGACORN_ENDPOINT_CONNECTION_TYPE_SYSTEM;
        return (codeSystem);
    }

    public Coding newPegacornEndpointJGroupsConnectionCodeSystem(String technologyType, String endpointType) {
        Coding coding = new Coding();
        coding.setCode(technologyType);
        coding.setSystem(getPegacornEndpointConnectionTypeSystem());
        String codeDisplay = technologyType + "(" + endpointType +" )";
        coding.setDisplay(codeDisplay);
        return (coding);
    }

    public Coding newPegacornEndpointJGroupsConnectionCodeSystem(EndpointTypeEnum endpointType) {
        Coding coding = new Coding();
        coding.setCode(endpointType.getToken());
        coding.setSystem(getPegacornEndpointConnectionTypeSystem());
        coding.setDisplay(endpointType.getDisplayName());
        return (coding);
    }
}
