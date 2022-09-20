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
package net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections;

import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.base.PetasosClusterConnection;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;

public abstract class PetasosFHIRClusterConnection extends PetasosClusterConnection {
    @Produce
    private ProducerTemplate camelProducer;

    //
    // Constructor(s)
    //

    public PetasosFHIRClusterConnection(){
        super();
    }

    //
    // PostConstruct Activities
    //

    @Override
    protected void executePostConstructActivities() {

    }

    //
    // Getters and Setters
    //

    protected ProducerTemplate getCamelProducer() {
        return camelProducer;
    }

    @Override
    public ClusterFunctionNameEnum getChannelFunction() {
        return ClusterFunctionNameEnum.PETASOS_FHIR_SERVICES;
    }

    //
    // Endpoint Specifications
    //

    @Override
    protected ClusterFunctionNameEnum getClusterFunction() {
        return (ClusterFunctionNameEnum.PETASOS_FHIR_SERVICES);
    }

    //
    // Processing Plant check triggered by JGroups Cluster membership change
    //

    @Override
    protected void doIntegrationPointBusinessFunctionCheck(JGroupsChannelConnectorSummary integrationPointSummary, boolean isRemoved, boolean isAdded) {

    }
}
