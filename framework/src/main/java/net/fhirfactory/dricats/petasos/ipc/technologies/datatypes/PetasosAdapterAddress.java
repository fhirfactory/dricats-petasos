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
package net.fhirfactory.dricats.petasos.ipc.technologies.datatypes;

import org.jgroups.Address;

import java.net.URL;
import java.util.Objects;

public class PetasosAdapterAddress {
    private Address jGroupsAddress;
    private URL hTTPAddress;
    private PetasosAdapterAddressTypeEnum addressType;
    private String addressName;

    public PetasosAdapterAddress(){
        this.addressType = null;
        this.jGroupsAddress = null;
        this.hTTPAddress = null;
        this.addressName = null;
    }

    public PetasosAdapterAddress(PetasosAdapterAddress ori){
        setAddressType(ori.getAddressType());
        setHTTPAddress(ori.getHTTPAddress());
        setJGroupsAddress(ori.getJGroupsAddress());
        setAddressName(ori.getAddressName());
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    public Address getJGroupsAddress() {
        return jGroupsAddress;
    }

    public void setJGroupsAddress(Address jGgroupsAddress) {
        this.jGroupsAddress = jGgroupsAddress;
    }

    public URL getHTTPAddress() {
        return hTTPAddress;
    }

    public void setHTTPAddress(URL hTTPAddress) {
        this.hTTPAddress = hTTPAddress;
    }

    public PetasosAdapterAddressTypeEnum getAddressType() {
        return addressType;
    }

    public void setAddressType(PetasosAdapterAddressTypeEnum addressType) {
        this.addressType = addressType;
    }

    @Override
    public String toString() {
        return "IPCEndpointAddress{" +
                "jGroupsAddress=" + jGroupsAddress +
                ", hTTPAddress=" + hTTPAddress +
                ", addressType=" + addressType +
                ", addressName=" + addressName +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PetasosAdapterAddress)) return false;
        PetasosAdapterAddress that = (PetasosAdapterAddress) o;
        return Objects.equals(jGroupsAddress, that.jGroupsAddress) && Objects.equals(hTTPAddress, that.hTTPAddress) && getAddressType() == that.getAddressType() && Objects.equals(getAddressName(), that.getAddressName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(jGroupsAddress, hTTPAddress, getAddressType(), getAddressName());
    }
}
