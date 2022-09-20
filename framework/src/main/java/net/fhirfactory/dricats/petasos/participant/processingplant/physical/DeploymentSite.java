/*
 * Copyright (c) 2022 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.participant.processingplant.physical;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.configuration.api.services.processingplant.EdgeProcessingPlantConfigurationService;
import net.fhirfactory.dricats.model.simplified.resources.simple.LocationESR;
import net.fhirfactory.dricats.model.simplified.resources.summaries.LocationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DeploymentSite {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentSite.class);
    private String siteName;
    private LocationESR siteLocation;

    @Inject
    private EdgeProcessingPlantConfigurationService processingPlantConfigurationService;

    //
    // Constructor(s)
    //

    //
    // Post Constructor
    //

    //
    // Getters and Setters
    //

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public LocationESR getSiteLocation() {
        return siteLocation;
    }

    public void setSiteLocation(LocationESR siteLocation) {
        this.siteLocation = siteLocation;
    }

    @JsonIgnore
    protected Logger getLogger(){
        return(LOG);
    }

    @JsonIgnore
    protected EdgeProcessingPlantConfigurationService getProcessingPlantConfigurationService(){
        return(this.processingPlantConfigurationService);
    }

    //
    // toString
    //

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DeploymentSite{");
        sb.append("siteName='").append(siteName).append('\'');
        sb.append(", siteLocation=").append(siteLocation);
        sb.append('}');
        return sb.toString();
    }

    //
    // toSummary() Methods
    //

    public LocationSummary toSummary(){
        getLogger().debug(".toSummary(): Entry");
        LocationSummary summary = new LocationSummary();

        getLogger().debug(".toSummary(): Exit, summary->{}", summary);
        return(summary);
    }
}
