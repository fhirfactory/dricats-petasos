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
import net.fhirfactory.dricats.petasos.participant.endpoint.interact.StandardInteractSocketBasedClient;
import net.fhirfactory.dricats.petasos.participant.endpoint.mllp.adapters.MLLPSenderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MLLPSenderEndpoint extends StandardInteractSocketBasedClient {
	private static final Logger LOG = LoggerFactory.getLogger(MLLPSenderEndpoint.class);
	private static final String MLLP_SENDER_ENDPOINT_VERSION = "1.0.0";

	//
	// Constructor(s)
	//

	public MLLPSenderEndpoint() {
		super();
		setEndpointType(EndpointTypeEnum.MLLP_CLIENT);
	}

	//
	// Getters and Setters
	//

	@JsonIgnore
	public List<MLLPSenderAdapter> getMLLPClientAdapters() {
		List<MLLPSenderAdapter> mllpAdapterList = new ArrayList<>();
		for (AdapterBase currentInterface : getAdapterList()) {
			MLLPSenderAdapter currentClientAdapter = (MLLPSenderAdapter) currentInterface;
			mllpAdapterList.add(currentClientAdapter);
		}
		return mllpAdapterList;
	}

	@JsonIgnore
	public void setMLLPClientAdapters(List<MLLPSenderAdapter> targetMLLPSenderAdapters) {
		if (targetMLLPSenderAdapters != null) {
			this.getAdapterList().clear();
			this.getAdapterList().addAll(targetMLLPSenderAdapters);
		}
	}

	public String getTargetSystemName() {
		return (getConnectedSystemName());
	}

	@Override
	public String getEndpointDescription() {
		if (getMLLPClientAdapters().isEmpty()) {
			return (null);
		}
		MLLPSenderAdapter activePort = null;
		for (MLLPSenderAdapter currentPort : getMLLPClientAdapters()) {
			if (currentPort.isActive()) {
				activePort = currentPort;
				break;
			}
		}
		if (activePort == null) {
			return (null);
		}
		String portDescription = "mllp://" + activePort.getHostName() + ":" + activePort.getPortNumber();
		return (portDescription);
	}

	@Override
	public String getEndpointURL() {
		if (getMLLPClientAdapters().isEmpty()) {
			return (null);
		}
		MLLPSenderAdapter activePort = null;
		for (MLLPSenderAdapter currentPort : getMLLPClientAdapters()) {
			if (currentPort.isActive()) {
				activePort = currentPort;
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
	protected String specifyComponentName() {
		return (getClass().getSimpleName());
	}

	@Override
	protected String specifyComponentVersion() {
		return (MLLP_SENDER_ENDPOINT_VERSION);
	}

	@Override
	protected Logger getLogger() {
		return (LOG);
	}

	@Override
	public boolean isUsingStandardCamelRoute() {
		return false;
	}

	//
	// toString
	//

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MLLPClientEndpoint{");
		sb.append(super.toString());
		sb.append('}');
		return sb.toString();
	}
}
