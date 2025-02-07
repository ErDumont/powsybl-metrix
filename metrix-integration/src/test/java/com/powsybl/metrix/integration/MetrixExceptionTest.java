/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.dsl.GroovyDslContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.exceptions.ContingenciesScriptLoadingException;
import com.powsybl.metrix.integration.exceptions.MappingScriptLoadingException;
import com.powsybl.metrix.integration.exceptions.MetrixScriptLoadingException;
import com.powsybl.metrix.integration.metrix.MetrixAnalysis;
import com.powsybl.metrix.integration.metrix.MetrixInputAnalysis;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class MetrixExceptionTest {

    private FileSystem fileSystem;

    private Path workspace;

    private ComputationManager computationManager;

    private MetrixAppLogger appLogger;

    private Path wrongDslFile;

    private Path emptyDslFile;

    private Network network;

    private void writeToDslFile(Path dslFile, String... lines) throws IOException {
        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), lines));
        }
    }

    @BeforeEach
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        workspace = Files.createDirectory(fileSystem.getPath("/tmp"));
        computationManager = new LocalComputationManager(workspace);
        appLogger = new MetrixAppLogger() {
            @Override
            public void log(String message, Object... args) {
            }

            @Override
            public MetrixAppLogger tagged(String tag) {
                return this;
            }
        };
        wrongDslFile = fileSystem.getPath("/wrongTest.dsl");
        emptyDslFile = fileSystem.getPath("/emptyTest.dsl");
        writeToDslFile(wrongDslFile, "wrongKeyword");
        writeToDslFile(emptyDslFile, "");
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void loadContingenciesScriptExceptionTest() {
        ContingenciesProvider provider = new GroovyDslContingenciesProvider(wrongDslFile);
        MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(new StringReader(""), provider, network, new MetrixDslData(), null);
        assertThrows(ContingenciesScriptLoadingException.class, () -> metrixInputAnalysis.runAnalysis());
    }

    @Test
    void loadMappingScriptExceptionTest() throws IOException {
        Path networkFile = fileSystem.getPath("/simpleNetwork.xml");
        NetworkXml.write(network, networkFile);
        NetworkSource networkSource = new DefaultNetworkSourceImpl(networkFile, computationManager);
        Reader mappingReader = Files.newBufferedReader(wrongDslFile, StandardCharsets.UTF_8);
        MetrixAnalysis metrixAnalysis = new MetrixAnalysis(networkSource, mappingReader, null, null, null,
            new ReadOnlyTimeSeriesStoreCache(), appLogger, null);
        assertThrows(MappingScriptLoadingException.class, () -> metrixAnalysis.runAnalysis(""));
    }

    @Test
    void loadMetrixScriptExceptionTest() throws IOException {
        Path networkFile = fileSystem.getPath("/simpleNetwork.xml");
        NetworkXml.write(network, networkFile);
        NetworkSource networkSource = new DefaultNetworkSourceImpl(networkFile, computationManager);
        Reader metrixDslReader = Files.newBufferedReader(wrongDslFile, StandardCharsets.UTF_8);
        Reader mappingReader = Files.newBufferedReader(emptyDslFile, StandardCharsets.UTF_8);
        MetrixAnalysis metrixAnalysis = new MetrixAnalysis(networkSource, mappingReader, metrixDslReader, null, null,
                new ReadOnlyTimeSeriesStoreCache(), appLogger, null);
        assertThrows(MetrixScriptLoadingException.class, () -> metrixAnalysis.runAnalysis(""));
    }
}
