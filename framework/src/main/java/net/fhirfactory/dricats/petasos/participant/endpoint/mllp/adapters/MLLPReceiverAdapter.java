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
package net.fhirfactory.dricats.petasos.participant.endpoint.mllp.adapters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.model.petasos.participant.components.adapter.ClusteredSocketAdapter;

public class MLLPReceiverAdapter extends ClusteredSocketAdapter {

    private Integer maximumConcurrentConsumers;
    private Boolean convertToStringPayload;
    private Boolean validatePayload;
    private Integer acceptTimeout;
    private Integer bindTimeout;

    //
    // Constructor(s)
    //

    public MLLPReceiverAdapter(){
        super();
        this.maximumConcurrentConsumers = null;
        this.convertToStringPayload = null;
        this.validatePayload = null;
        this.acceptTimeout = null;
        this.bindTimeout = null;
    }

    //
    // Getters and Setters
    //

    @JsonIgnore
    public boolean hasMaximumConcurrentConsumers(){
        boolean hasValue = this.maximumConcurrentConsumers != null;
        return(hasValue);
    }
    
    public Integer getMaximumConcurrentConsumers() {
        return maximumConcurrentConsumers;
    }

    public void setMaximumConcurrentConsumers(Integer maximumConcurrentConsumers) {
        this.maximumConcurrentConsumers = maximumConcurrentConsumers;
    }
    
    @JsonIgnore
    public boolean hasConvertToStringPayload(){
        boolean hasValue = this.convertToStringPayload != null;
        return(hasValue);
    }

    public Boolean getConvertToStringPayload() {
        return convertToStringPayload;
    }

    public void setConvertToStringPayload(Boolean convertToStringPayload) {
        this.convertToStringPayload = convertToStringPayload;
    }
    
    @JsonIgnore
    public boolean hasValidatePayload(){
        boolean hasValue = this.validatePayload != null;
        return(hasValue);
    }

    public Boolean getValidatePayload() {
        return validatePayload;
    }

    public void setValidatePayload(Boolean validatePayload) {
        this.validatePayload = validatePayload;
    }
    
    @JsonIgnore
    public boolean hasAcceptTimeout(){
        boolean hasValue = this.acceptTimeout != null;
        return(hasValue);
    }

    public Integer getAcceptTimeout() {
        return acceptTimeout;
    }

    public void setAcceptTimeout(Integer acceptTimeout) {
        this.acceptTimeout = acceptTimeout;
    }

    @JsonIgnore
    public boolean hasBindTimeout(){
        boolean hasValue = this.bindTimeout != null;
        return(hasValue);
    }
    
    public Integer getBindTimeout() {
        return bindTimeout;
    }

    public void setBindTimeout(Integer bindTimeout) {
        this.bindTimeout = bindTimeout;
    }

    //
    // To String
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MLLPServerAdapter{");
        sb.append("maximumConcurrentConsumers=").append(maximumConcurrentConsumers);
        sb.append(", convertToStringPayload=").append(convertToStringPayload);
        sb.append(", validatePayload=").append(validatePayload);
        sb.append(", acceptTimeout=").append(acceptTimeout);
        sb.append(", bindTimeout=").append(bindTimeout);
        sb.append(", ").append(super.toString()).append('}');
        return sb.toString();
    }


}
