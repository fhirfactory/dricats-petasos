/*
 * Copyright (c) 2020 Mark A. Hunter (ACT Health)
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

package net.fhirfactory.dricats.petasos.participant.endpoint.base.valuesets;

public enum EndpointResilienceModeEnum {
    RESILIENCE_MODE_STANDALONE("StandAlone", "dricats.endpoint.resilience_mode.standalone"),
    RESILIENCE_MODE_KUBERNETES_STANDALONE("Kubenetes-SinglePod", "dricats.endpoint.resilience_mode.kubernetes_standalone"),
    RESILIENCE_MODE_CLUSTERED("Clustered", "dricats.endpoint.resilience_mode.clustered"),
    RESILIENCE_MODE_KUBERNETES_CLUSTERED("Kubernetes-Clustered", "dricats.endpoint.resilience_mode.kubernetes_clustered"),
    RESILIENCE_MODE_MULTISITE("Multisite", "dricats.endpoint.resileince_mode.multisite"),
    RESILIENCE_MODE_MULTISITE_CLUSTERED("Multisite-Clustered","dricats.endpoint.resilience_mode.clustered_multisite"),
    RESILIENCE_MODE_KUBERNETES_MULTISITE("Multisite-Kubernetes-SinglePod", "dricats.endpoint.resilience_mode.kubernetes_multisite"),
    RESILIENCE_MODE_KUBERNETES_MULTISITE_CLUSTERED("Multisite-Kubernetes-Clustered","dricats.endpoint.resilience_mode.kubernetes_multisite_clustered");

    private String token;
    private String displayName;

    private EndpointResilienceModeEnum(String nodeModeDisplayName, String nodeMode){
        this.token = nodeMode;
        this.displayName = nodeModeDisplayName;
    }

    public String getToken(){
        return(this.token);
    }

    public String getDisplayName(){
        return(this.displayName);
    }
}
