/* Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved. */
package oracle.fmwplatformqa.opatchautoqa.util;

import org.junit.Assert;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for accessing LCM agents
 */
public class AgentHelper {

    /**
     * Returns the info for all the agents created in this container.
     * Throws an assert fail if the agents can't be retrieved.
     * @return a list of agent info objects
     */
    public static List<AgentInfo> getAgentInfos() {
        List<AgentInfo> infos = new ArrayList<>();

        // text file /scratch/lcmagents contains a list of agent containers. each line is hostname/ipv4-address.

        Path path = Paths.get("/scratch", "lcmagents");
        try(BufferedReader reader = Files.newBufferedReader(path, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String tokens[] = line.split("/");
                String hostName = tokens[0];
                String address = tokens[1];
                infos.add(new AgentInfo(hostName, address));
            }
        }
        catch (Exception e) {
            Assert.fail("Error reading lcmagents: " + e);
        }

        return infos;
    }
}
