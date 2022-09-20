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
package net.fhirfactory.pegacorn.platform.edge.itops.configuration;

import net.fhirfactory.dricats.model.configuration.filebased.segments.JavaDeploymentSegment;
import net.fhirfactory.dricats.model.configuration.filebased.segments.LoadBalancerSegment;
import net.fhirfactory.dricats.model.configuration.filebased.segments.SecurityCredentialSegment;
import net.fhirfactory.dricats.model.configuration.filebased.segments.SubsystemImageSegment;
import net.fhirfactory.dricats.model.configuration.filebased.segments.SubsystemInstanceSegment;
import net.fhirfactory.dricats.model.configuration.filebased.segments.VolumeMountSegment;
import net.fhirfactory.dricats.model.configuration.filebased.segments.endpoints.http.HTTPServerEndpointSegment;
import net.fhirfactory.dricats.model.configuration.filebased.segments.endpoints.jgroups.JGroupsInterZoneRepeaterServerPortSegment;

public class JGroupsGossipRouterNodeConfig {

	private SubsystemInstanceSegment subsystemInstant;
	private HTTPServerEndpointSegment kubeReadinessProbe;
	private HTTPServerEndpointSegment kubeLivelinessProbe;
	private HTTPServerEndpointSegment prometheusPort;
	private HTTPServerEndpointSegment jolokiaPort;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterIPC;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterTasking;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterTopology;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterSubscriptions;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterInterception;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterMetrics;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterInfinspan;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterDatagrid;
	private JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterAudit;
	private LoadBalancerSegment loadBalancer;
	private SubsystemImageSegment subsystemImageProperties;
	private SecurityCredentialSegment trustStorePassword;
	private SecurityCredentialSegment keyPassword;
	private JavaDeploymentSegment javaDeploymentParameters;
	private VolumeMountSegment volumeMounts;
	private SecurityCredentialSegment hapiAPIKey;

	//
	// Constructor(s)
	//

	public JGroupsGossipRouterNodeConfig() {
		this.subsystemInstant = new SubsystemInstanceSegment();
		this.kubeLivelinessProbe = new HTTPServerEndpointSegment();
		this.kubeReadinessProbe = new HTTPServerEndpointSegment();
		this.subsystemImageProperties = new SubsystemImageSegment();
		this.trustStorePassword = new SecurityCredentialSegment();
		this.keyPassword = new SecurityCredentialSegment();
		this.jolokiaPort = new HTTPServerEndpointSegment();
		this.prometheusPort = new HTTPServerEndpointSegment();
		this.multizoneRepeaterAudit = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterDatagrid = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterInfinspan = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterInterception = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterIPC = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterMetrics = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterSubscriptions = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterTasking = new JGroupsInterZoneRepeaterServerPortSegment();
		this.multizoneRepeaterTopology = new JGroupsInterZoneRepeaterServerPortSegment();
		this.loadBalancer = new LoadBalancerSegment();
		this.volumeMounts = new VolumeMountSegment();
		this.hapiAPIKey = new SecurityCredentialSegment();
	}

	//
	// Getters and Setters
	//

	public SecurityCredentialSegment getHapiAPIKey() {
		return hapiAPIKey;
	}

	public void setHapiAPIKey(SecurityCredentialSegment hapiAPIKey) {
		this.hapiAPIKey = hapiAPIKey;
	}

	public VolumeMountSegment getVolumeMounts() {
		return volumeMounts;
	}

	public void setVolumeMounts(VolumeMountSegment volumeMounts) {
		this.volumeMounts = volumeMounts;
	}

	public LoadBalancerSegment getLoadBalancer() {
		return loadBalancer;
	}

	public void setLoadBalancer(LoadBalancerSegment loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	public HTTPServerEndpointSegment getKubeReadinessProbe() {
		return kubeReadinessProbe;
	}

	public void setKubeReadinessProbe(HTTPServerEndpointSegment kubeReadinessProbe) {
		this.kubeReadinessProbe = kubeReadinessProbe;
	}

	public HTTPServerEndpointSegment getKubeLivelinessProbe() {
		return kubeLivelinessProbe;
	}

	public void setKubeLivelinessProbe(HTTPServerEndpointSegment kubeLivelinessProbe) {
		this.kubeLivelinessProbe = kubeLivelinessProbe;
	}

	public HTTPServerEndpointSegment getPrometheusPort() {
		return prometheusPort;
	}

	public void setPrometheusPort(HTTPServerEndpointSegment prometheusPort) {
		this.prometheusPort = prometheusPort;
	}

	public HTTPServerEndpointSegment getJolokiaPort() {
		return jolokiaPort;
	}

	public void setJolokiaPort(HTTPServerEndpointSegment jolokiaPort) {
		this.jolokiaPort = jolokiaPort;
	}

	public SubsystemInstanceSegment getSubsystemInstant() {
		return subsystemInstant;
	}

	public void setSubsystemInstant(SubsystemInstanceSegment subsystemInstant) {
		this.subsystemInstant = subsystemInstant;
	}

	public SubsystemImageSegment getSubsystemImageProperties() {
		return subsystemImageProperties;
	}

	public void setSubsystemImageProperties(SubsystemImageSegment subsystemImageProperties) {
		this.subsystemImageProperties = subsystemImageProperties;
	}

	public SecurityCredentialSegment getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(SecurityCredentialSegment trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public SecurityCredentialSegment getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(SecurityCredentialSegment keyPassword) {
		this.keyPassword = keyPassword;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterIPC() {
		return multizoneRepeaterIPC;
	}

	public void setMultizoneRepeaterIPC(JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterIPC) {
		this.multizoneRepeaterIPC = multizoneRepeaterIPC;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterTasking() {
		return multizoneRepeaterTasking;
	}

	public void setMultizoneRepeaterTasking(JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterTasking) {
		this.multizoneRepeaterTasking = multizoneRepeaterTasking;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterTopology() {
		return multizoneRepeaterTopology;
	}

	public void setMultizoneRepeaterTopology(JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterTopology) {
		this.multizoneRepeaterTopology = multizoneRepeaterTopology;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterSubscriptions() {
		return multizoneRepeaterSubscriptions;
	}

	public void setMultizoneRepeaterSubscriptions(
			JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterSubscriptions) {
		this.multizoneRepeaterSubscriptions = multizoneRepeaterSubscriptions;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterInterception() {
		return multizoneRepeaterInterception;
	}

	public void setMultizoneRepeaterInterception(
			JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterInterception) {
		this.multizoneRepeaterInterception = multizoneRepeaterInterception;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterMetrics() {
		return multizoneRepeaterMetrics;
	}

	public void setMultizoneRepeaterMetrics(JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterMetrics) {
		this.multizoneRepeaterMetrics = multizoneRepeaterMetrics;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterInfinspan() {
		return multizoneRepeaterInfinspan;
	}

	public void setMultizoneRepeaterInfinspan(JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterInfinspan) {
		this.multizoneRepeaterInfinspan = multizoneRepeaterInfinspan;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterDatagrid() {
		return multizoneRepeaterDatagrid;
	}

	public void setMultizoneRepeaterDatagrid(JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterDatagrid) {
		this.multizoneRepeaterDatagrid = multizoneRepeaterDatagrid;
	}

	public JGroupsInterZoneRepeaterServerPortSegment getMultizoneRepeaterAudit() {
		return multizoneRepeaterAudit;
	}

	public void setMultizoneRepeaterAudit(JGroupsInterZoneRepeaterServerPortSegment multizoneRepeaterAudit) {
		this.multizoneRepeaterAudit = multizoneRepeaterAudit;
	}

	public JavaDeploymentSegment getJavaDeploymentParameters() {
		return javaDeploymentParameters;
	}

	public void setJavaDeploymentParameters(JavaDeploymentSegment javaDeploymentParameters) {
		this.javaDeploymentParameters = javaDeploymentParameters;
	}

	//
	// To String
	//

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JGroupsGossipRouterNodeConfig{");
        sb.append("subsystemInstant=").append(subsystemInstant);
        sb.append(", kubeReadinessProbe=").append(kubeReadinessProbe);
        sb.append(", kubeLivelinessProbe=").append(kubeLivelinessProbe);
        sb.append(", prometheusPort=").append(prometheusPort);
        sb.append(", jolokiaPort=").append(jolokiaPort);
        sb.append(", multizoneRepeaterIPC=").append(multizoneRepeaterIPC);
        sb.append(", multizoneRepeaterTasking=").append(multizoneRepeaterTasking);
        sb.append(", multizoneRepeaterTopology=").append(multizoneRepeaterTopology);
        sb.append(", multizoneRepeaterSubscriptions=").append(multizoneRepeaterSubscriptions);
        sb.append(", multizoneRepeaterInterception=").append(multizoneRepeaterInterception);
        sb.append(", multizoneRepeaterMetrics=").append(multizoneRepeaterMetrics);
        sb.append(", multizoneRepeaterInfinspan=").append(multizoneRepeaterInfinspan);
        sb.append(", multizoneRepeaterDatagrid=").append(multizoneRepeaterDatagrid);
        sb.append(", multizoneRepeaterAudit=").append(multizoneRepeaterAudit);
        sb.append(", loadBalancer=").append(loadBalancer);
        sb.append(", subsystemImageProperties=").append(subsystemImageProperties);
        sb.append(", trustStorePassword=").append(trustStorePassword);
        sb.append(", keyPassword=").append(keyPassword);
        sb.append(", javaDeploymentParameters=").append(javaDeploymentParameters);
        sb.append(", volumeMounts=").append(volumeMounts);
        sb.append(", hapiAPIKey=").append(hapiAPIKey);
        sb.append('}');
        return sb.toString();
    }
}
