/* Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved. */
package oracle.fmwplatformqa.opatchautoqa.util;

/**
 * Contains information about an LCM agent
 */
public class AgentInfo {
    private static final int AGENT_PORT = 8443;

    private String hostName;
    private String address;

    public AgentInfo(String hostName, String address) {
        this.hostName = hostName;
        this.address = address;
    }

    public String getHostName() {
        return hostName;
    }

    public String getAddress() {
        return address;
    }

    public String getUrl() {
        return "https://" + getAddress() + ":" + AGENT_PORT;
    }

    public String toString() {
        return "Agent \"" + hostName + "\" (" + address + ")";
    }
}
