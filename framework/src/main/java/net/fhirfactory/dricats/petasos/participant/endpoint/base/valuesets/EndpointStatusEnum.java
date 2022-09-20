/*
 * Copyright (c) 2020 Mark A. Hunter
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

public enum EndpointStatusEnum {
    DRICATS_ENDPOINT_STATUS_SAME("dricats.endpoint.status.same"),
    DRICATS_ENDPOINT_STATUS_DETECTED("dricats.endpoint.status.detected"),
    DRICATS_ENDPOINT_STATUS_REACHABLE("dricats.endpoint.status.reachable"),
    DRICATS_ENDPOINT_STATUS_UNREACHABLE("dricats.endpoint.status.unreachable"),
    DRICATS_ENDPOINT_STATUS_STARTED("dricats.endpoint.status.started"),
    DRICATS_ENDPOINT_STATUS_OPERATIONAL("dricats.endpoint.status.operational"),
    DRICATS_ENDPOINT_STATUS_SUSPECT("dricats.endpoint.status.suspect"),
    DRICATS_ENDPOINT_STATUS_UNKNOWN("dricats.endpoint.status.unknown"),
    DRICATS_ENDPOINT_STATUS_FAILED("dricats.endpoint.status.failed");

    private String endpointStatus;

    private EndpointStatusEnum(String status){
        this.endpointStatus = status;
    }

    public String getEndpointStatus() {
        return endpointStatus;
    }
}
