/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.Before;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class TimeSeriesMapToTest {

    private Network network;

    private MappingParameters mappingParameters = MappingParameters.load();

    @Before
    public void setUp() throws Exception {
        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
    }

    @Test
    public void mapToDefaultVariableTest() throws Exception {

        Map<String, MappingVariable> results = new HashMap<>();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 10d, 11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 0), false, false, mappingParameters.getToleranceThreshold());

        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable identifiable, MappingVariable variable, double equipmentValue) {
                if (!timeSeriesName.isEmpty()) {
                    results.put(identifiable.getId(), variable);
                }
            }
        };
        mapper.mapToNetwork(store, parameters, ImmutableList.of(observer));

        assertEquals(3, results.size());
        assertEquals(ImmutableMap.of("FSSV.O11_G", EquipmentVariable.targetP, "FSSV.O11_L", EquipmentVariable.p0, "HVDC1", EquipmentVariable.activePowerSetpoint), results);
    }

    @Test
    public void mapToVariableTest() throws Exception {

        Map<String, List<MappingVariable>> results = new HashMap<>();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable targetP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable targetQ",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O12_G\"",
                "    }",
                "    variable minP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FVALDI11_G\"",
                "    }",
                "    variable maxP",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variable p0",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variable q0",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L\"",
                "    }",
                "    variable fixedActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L2\"",
                "    }",
                "    variable variableActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L\"",
                "    }",
                "    variable fixedReactivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L2\"",
                "    }",
                "    variable variableReactivePower",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "    variable activePowerSetpoint",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts2'",
                "    filter {",
                "        hvdcLine.id==\"HVDC2\"",
                "    }",
                "    variable minP",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC2\"",
                "    }",
                "    variable maxP",
                "}",
                "mapToPsts {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        pst.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("ts1", index, 10d, 11d),
                TimeSeries.createDouble("ts2", index, -10d, -11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 0), false, false, mappingParameters.getToleranceThreshold());

        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable identifiable, MappingVariable variable, double equipmentValue) {
                if (!timeSeriesName.isEmpty()) {
                    if (results.containsKey(identifiable.getId())) {
                        results.get(identifiable.getId()).add(variable);
                    } else {
                        List<MappingVariable> list = new ArrayList<>();
                        list.add(variable);
                        results.put(identifiable.getId(), list);
                    }
                }
            }
        };
        mapper.mapToNetwork(store, parameters, ImmutableList.of(observer));

        assertEquals(9, results.size());
        assertEquals(2, results.get("FSSV.O11_G").size());
        assertTrue(results.get("FSSV.O11_G").containsAll(ImmutableList.of(EquipmentVariable.targetP, EquipmentVariable.targetQ)));
        assertEquals(1, results.get("FSSV.O12_G").size());
        assertTrue(results.get("FSSV.O12_G").containsAll(ImmutableList.of(EquipmentVariable.minP)));
        assertEquals(1, results.get("FVALDI11_G").size());
        assertTrue(results.get("FVALDI11_G").containsAll(ImmutableList.of(EquipmentVariable.maxP)));
        assertEquals(2, results.get("FSSV.O11_L").size());
        assertTrue(results.get("FSSV.O11_L").containsAll(ImmutableList.of(EquipmentVariable.p0, EquipmentVariable.q0)));
        assertEquals(2, results.get("FVALDI11_L").size());
        assertTrue(results.get("FVALDI11_L").containsAll(ImmutableList.of(EquipmentVariable.fixedActivePower, EquipmentVariable.fixedReactivePower)));
        assertEquals(2, results.get("FVALDI11_L").size());
        assertTrue(results.get("FVALDI11_L2").containsAll(ImmutableList.of(EquipmentVariable.variableActivePower, EquipmentVariable.variableReactivePower)));
        assertEquals(1, results.get("HVDC1").size());
        assertTrue(results.get("HVDC1").containsAll(ImmutableList.of(EquipmentVariable.activePowerSetpoint)));
        assertEquals(2, results.get("HVDC2").size());
        assertTrue(results.get("HVDC2").containsAll(ImmutableList.of(EquipmentVariable.minP, EquipmentVariable.maxP)));
        assertEquals(1, results.get("FP.AND1  FTDPRA1  1").size());
        assertTrue(results.get("FP.AND1  FTDPRA1  1").containsAll(ImmutableList.of(EquipmentVariable.currentTap)));
    }

    @Test
    public void mapToGeneratorsWithDistributionKeyAllEqualToZeroTest() throws Exception {

        Map<String, MappingVariable> results = new HashMap<>();
        Map<String, Double> values = new HashMap<>();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FVALDI11_G\" || generator.id==\"FVALDI12_G\"",
                "    }",
                "    distributionKey {",
                "        generator.targetP",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 1000d, 2000d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 0), false, false, mappingParameters.getToleranceThreshold());

        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable identifiable, MappingVariable variable, double equipmentValue) {
                if (!timeSeriesName.isEmpty()) {
                    results.put(identifiable.getId(), variable);
                    values.put(identifiable.getId(), equipmentValue);
                }
            }
        };
        mapper.mapToNetwork(store, parameters, ImmutableList.of(observer));

        assertEquals(2, results.size());
        assertEquals(ImmutableMap.of("FVALDI11_G", EquipmentVariable.targetP, "FVALDI12_G", EquipmentVariable.targetP), results);
        assertEquals(2, values.size());
        assertEquals(ImmutableMap.of("FVALDI11_G", 500., "FVALDI12_G", 500.), values);
    }

    @Test
    public void mapToGeneratorsWithSpecificIgnoreLimitsTest() throws Exception {
        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id == \"FVALDI11_G\"",
                "    }",
                "}",
                "ignoreLimits { 'ts' + '1'}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 10000d, 20000d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        // assertions
        assertEquals(1, mappingConfig.getIgnoreLimitsTimeSeriesNames().size());
        assertEquals(ImmutableSet.of("ts1"), mappingConfig.getIgnoreLimitsTimeSeriesNames());

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 0), false, false, mappingParameters.getToleranceThreshold());

        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable identifiable, MappingVariable variable, double equipmentValue) {
                if (timeSeriesName.compareTo("ts1") == 0) {
                    assertEquals(0, point);
                    assertEquals("ts1", timeSeriesName);
                    assertEquals("FVALDI11_G", identifiable.getId());
                    assertEquals(EquipmentVariable.targetP, variable);
                    assertEquals(10000, equipmentValue, 0);
                }
            }
        };
        mapper.mapToNetwork(store, parameters, ImmutableList.of(observer));
    }
}