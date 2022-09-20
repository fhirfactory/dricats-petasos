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
package net.fhirfactory.dricats.petasos.participant.endpoint.mllp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.model.petasos.participant.components.adapter.AdapterBase;
import net.fhirfactory.dricats.model.petasos.participant.components.endpoint.valuesets.EndpointTypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.petasos.participant.endpoint.interact.StandardInteractSocketBasedServer;
import net.fhirfactory.dricats.petasos.participant.endpoint.mllp.adapters.MLLPReceiverAdapter;
import net.fhirfactory.dricats.petasos.participant.endpoint.mllp.adapters.MLLPSenderAdapter;

import java.util.Set;

public class MLLPReceiverEndpoint extends StandardInteractSocketBasedServer {

	//
	// Constructor(s)
	//
	public MLLPReceiverEndpoint() {
		super();
		setEndpointType(EndpointTypeEnum.MLLP_SERVER);
//        setComponentSystemRole(SoftwareComponentConnectivityContextEnum.COMPONENT_ROLE_INTERACT_INGRES);
	}

	//
	// Getters and Setters
	//

	@JsonIgnore
	public MLLPReceiverAdapter getMLLPServerAdapter() {
		if (getAdapterList().isEmpty()) {
			return (null);
		}
		MLLPReceiverAdapter mllpServer = (MLLPReceiverAdapter) getAdapterList().get(0);
		return (mllpServer);
	}

	@JsonIgnore
	public void setMLLPServerAdapter(MLLPReceiverAdapter mllpServer) {
		if (mllpServer != null) {
			getAdapterList().add(mllpServer);
		}
	}

	@JsonIgnore
	public String getTargetSystemName() {
		return (getConnectedSystemName());
	}

	@JsonIgnore
	public String getTargetConnectionDescription() {
		if (!getAdapterList().isEmpty()) {
			MLLPReceiverAdapter mllpServer = (MLLPReceiverAdapter) getAdapterList().get(0);
			String portDescription = "mllp://" + mllpServer.getHostName() + ":" + mllpServer.getPortNumber();
			return (portDescription);
		}
		return (null);
	}

	@JsonIgnore
	public String getDetailedConnectionDescription() {
		if (!getAdapterList().isEmpty()) {
			MLLPReceiverAdapter mllpServer = (MLLPReceiverAdapter) getAdapterList().get(0);
			StringBuilder portDescription = new StringBuilder();
			portDescription.append("mllp://" + mllpServer.getHostName() + ":" + mllpServer.getPortNumber());
			if (mllpServer.getAdditionalParameters().isEmpty()) {
				return (portDescription.toString());
			}
			portDescription.append("?");
			Set<String> configurationParameterNames = mllpServer.getAdditionalParameters().keySet();
			int size = configurationParameterNames.size();
			int count = 0;
			for (String currentConfigParameterName : configurationParameterNames) {
				String value = mllpServer.getAdditionalParameters().get(currentConfigParameterName);
				portDescription.append(currentConfigParameterName + "=" + value);
				if (count < (size - 1)) {
					portDescription.append("&");
				}
				count += 1;
			}
			return (portDescription.toString());
		}
		return (null);
	}

	@Override
	public String getEndpointURL() {
		if (getAdapterList().isEmpty()) {
			return (null);
		}
		MLLPSenderAdapter activePort = null;
		for ( AdapterBase currentPort : getAdapterList()) {
			if (currentPort.isActive()) {
				activePort = (MLLPSenderAdapter)currentPort;
				break;
			}
		}
		if (activePort == null) {
			return (null);
		}
		StringBuilder portDescription = new StringBuilder();
		portDescription.append("mllp://" + activePort.getHostName() + ":" + activePort.getPortNumber());
		if (activePort.getAdditionalParameters().isEmpty()) {
			return (portDescription.toString());
		}
		portDescription.append("?");
		Set<String> configurationParameterNames = activePort.getAdditionalParameters().keySet();
		int size = configurationParameterNames.size();
		int count = 0;
		for (String currentParameterName : configurationParameterNames) {
			String value = activePort.getAdditionalParameters().get(currentParameterName);
			portDescription.append(currentParameterName + "=" + value);
			if (count < (size - 1)) {
				portDescription.append("&");
			}
			count += 1;
		}
		return (portDescription.toString());
	}

	@Override
	public String getEndpointDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PetasosParticipantId getEndpointParticipantId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUsingStandardCamelRoute() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String specifyComponentName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String specifyComponentVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	//
	// toString
	//

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MLLPServerEndpoint{");
		sb.append(super.toString());
		sb.append('}');
		return sb.toString();
	}
}
