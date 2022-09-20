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
package net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.topology;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.petasos.participant.solution.LocalSolution;

public abstract class EdgeIPCEndpoint extends RouteBuilder {

    @Inject
    private CamelContext camelContext;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlant;

    @Inject
    private LocalSolution topologyIM;

    abstract protected Logger specifyLogger();

    protected Logger getLogger(){
        return(specifyLogger());
    }

    public CamelContext getCamelContext(){
        return(camelContext);
    }

    protected ProcessingPlantConfigurationServiceInterface getProcessingPlant(){
        return(processingPlant);
    }

    public LocalSolution getLocalSolution() {
        return topologyIM;
    }
}
