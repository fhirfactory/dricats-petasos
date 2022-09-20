/*
OOs * Copyright (c) 2021 Mark A. Hunter (ACT Health)
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

import javax.inject.Inject;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.dricats.model.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.dricats.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.base.PetasosClusterConnection;
import net.fhirfactory.dricats.util.FHIRContextUtility;

public abstract class PetasosAuditClusterConnection extends PetasosClusterConnection {

	private IParser fhirJSONParser;
	private PetasosParticipantId participantId;

	@Produce
	private ProducerTemplate camelProducer;

	@Inject
	private FHIRContextUtility fhirContextUtility;

	//
	// Constructor(s)
	//

	public PetasosAuditClusterConnection() {
		super();
	}

	//
	// PostConstruct Activities
	//

	@Override
	protected void executePostConstructActivities() {
		fhirJSONParser = fhirContextUtility.getJsonParser().setPrettyPrint(true);
		executePostConstructInstanceActivities();
	}

	//
	// Abstract Methods
	//

	abstract protected void executePostConstructInstanceActivities();

	//
	// Getters (and Setters)
	//

	protected IParser getFHIRJSONParser() {
		return (fhirJSONParser);
	}

	public ProducerTemplate getCamelProducer() {
		return camelProducer;
	}
       

	//
	// Endpoint Specification
	//

	@Override
	protected ClusterFunctionNameEnum getClusterFunction() {
		return (ClusterFunctionNameEnum.PETASOS_AUDIT_SERVICES);
	}

	//
	// Processing Plant check triggered by JGroups Cluster membership change
	//

	@Override
	protected void doIntegrationPointBusinessFunctionCheck(JGroupsChannelConnectorSummary integrationPointSummary,
			boolean isRemoved, boolean isAdded) {

	}

	//
	// Business Methods
	//

	//
	// ****Tactical****
	// Task Execution / Capability Utilisation Services
	//

	public CapabilityUtilisationResponse executeTask(String capabilityProviderName, CapabilityUtilisationRequest task) {
		getLogger().trace(".executeTask(): Entry, capabilityProviderName->{}, task->{}", capabilityProviderName, task);
		Address targetAddress = getTargetMemberAddress(capabilityProviderName);
		try {
			Object objectSet[] = new Object[1];
			Class classSet[] = new Class[1];
			objectSet[0] = task;
			classSet[0] = CapabilityUtilisationRequest.class;
			RequestOptions requestOptions = new RequestOptions(ResponseMode.GET_FIRST, getRPCUnicastTimeout());
			CapabilityUtilisationResponse response = null;
			synchronized (getChannelLock()) {
				response = getRPCDispatcher().callRemoteMethod(targetAddress, "executeTaskHandler", objectSet, classSet,
						requestOptions);
			}
			getMetricsAgent().incrementRemoteProcedureCallCount();
			getLogger().debug(".executeTask(): Exit, response->{}", response);
			return (response);
		} catch (NoSuchMethodException e) {
			getMetricsAgent().incrementRemoteProcedureCallFailureCount();
			getLogger().error(".executeTask(): Error (NoSuchMethodException) ->{}", e.getMessage());
			CapabilityUtilisationResponse response = new CapabilityUtilisationResponse();
			response.setAssociatedRequestID(task.getRequestID());
			response.setSuccessful(false);
			return (response);
		} catch (Exception e) {
			getMetricsAgent().incrementRemoteProcedureCallFailureCount();
			e.printStackTrace();
			getLogger().error(".executeTask: Error (GeneralException) ->{}", e.getMessage());
			CapabilityUtilisationResponse response = new CapabilityUtilisationResponse();
			response.setAssociatedRequestID(task.getRequestID());
			response.setSuccessful(false);
			return (response);
		}
	}
}
