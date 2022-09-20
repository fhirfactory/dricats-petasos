/*
 * The MIT License
 *
 * Copyright 2022 Mark A. Hunter.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.fhirfactory.dricats.petasos.participant.endpoint.base.valuesets;

/**
 *
 * @author mhunter
 */
public enum PublishedServiceNodeTypeEnum {
    SERVICE_TYPE_MULTI_SITE_LOAD_BALANCED("MultiSite-LoadBalanced", "dricats.service_node.type.multi_site_load_balanced", "Multiple Site, Multiple Server, Site-Resilient Load Balanced"),
    SERVICE_TYPE_SINGLE_SITE_CLUSTERED("SingleSite-Clustered", "dricats.service_node.type.single_site_clustered", "Single Site, Multiple Server, Kubernetes Service"),
    SERVICE_TYPE_SINGLE_SITE_STANDALONE("SingleSite-Standalone", "dricats.service_node.type.single_site_standalone", "Single Site, Single Server Service");
    
    private String displayName;
    private String token;
    private String displayText;
    
    private PublishedServiceNodeTypeEnum(String displayName, String token, String displayText){
        this.displayName = displayName;
        this.token = token;
        this.displayText = displayText;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }

    public String getDisplayText() {
        return displayText;
    }
    
    
}
