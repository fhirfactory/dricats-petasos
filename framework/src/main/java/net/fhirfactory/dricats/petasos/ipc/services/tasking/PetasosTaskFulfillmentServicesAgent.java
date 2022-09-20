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
package net.fhirfactory.dricats.petasos.ipc.services.tasking;

import net.fhirfactory.dricats.configuration.defaults.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.petasos.ipc.services.agents.tasks.oversight.PetasosTaskAgentProxyInterface;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.oversight.registration.PetasosTaskRegistration;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.PetasosTaskingClusterConnection;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PetasosTaskFulfillmentServicesAgent extends PetasosTaskingClusterConnection implements PetasosTaskAgentProxyInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosTaskFulfillmentServicesAgent.class);

    @Produce
    private ProducerTemplate template;

    //
    // Constructor
    //

    //
    // Post Construct
    //

    //
    // Business Methods
    //

    @Override
    public PetasosTaskRegistration registerTaskCreation(PetasosActionableTask actionableTask) {
        return null;
    }

    @Override
    public PetasosTaskRegistration registerTaskUpdate(PetasosActionableTask actionableTask) {
        return null;
    }

    @Override
    public ClusterFunctionNameEnum getChannelFunction() {
        return ClusterFunctionNameEnum.PETASOS_TASKING_SERVICES;
    }

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    public PetasosActionableTask fulfillActionableTask(PetasosActionableTask actionableTask, JGroupsChannelConnectorSummary requesterEndpointIdentifier) {
        template.sendBody(PetasosPropertyConstants.TASK_DISTRIBUTION_QUEUE, ExchangePattern.InOnly, actionableTask);
        return(actionableTask);
    }
}
