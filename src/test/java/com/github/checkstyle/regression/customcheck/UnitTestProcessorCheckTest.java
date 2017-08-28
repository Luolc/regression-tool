////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2017 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.checkstyle.regression.customcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.github.checkstyle.regression.data.ImmutableProperty;
import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.github.checkstyle.regression.data.ModuleInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class UnitTestProcessorCheckTest {
    private List<ModuleExtractInfo> modules;

    @Before
    public void setUp() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
            gsonBuilder.registerTypeAdapterFactory(factory);
        }
        final Gson gson = gsonBuilder.create();
        final String url = "https://gist.githubusercontent.com/Luolc/783aebf09efa1c738f9d535316e97895/raw/8082142d30414ae18ec9fceb260e10ebceb0c89f/checkstyle_modules_e51f9458.json";
        try (InputStreamReader reader = new InputStreamReader(new URL(url).openStream(), Charset.forName("UTF-8"))) {
            modules = gson.fromJson(reader, new TypeToken<List<ModuleExtractInfo>>() {
            }.getType());
        }
        catch (Exception ex) {
            // ignore
        }
        assertNotNull(modules);
    }

    private static String getInputPath(String name) {
        return "src/test/resources/com/github/checkstyle/regression/customcheck/"
                + "unittestprocessorcheck/" + name;
    }

    @Test
    public void testModuleConfigAsLocalVariable() throws Exception {
        UnitTestProcessorCheck.clearUnitTestToPropertiesMap();
        CustomCheckProcessor.process(
                getInputPath("InputModuleConfigAsLocalVariableModuleTest.java"),
                UnitTestProcessorCheck.class);
        final Map<String, Set<ModuleInfo.Property>> map =
                UnitTestProcessorCheck.getUnitTestToPropertiesMap();

        assertEquals("The size of UTs is wrong", 7, map.size());
        assertPropertiesEquals(map, "testDefault");
        assertPropertiesEquals(map, "testFormat",
                ImmutableProperty.builder().name("format").value("^$").build());
        assertPropertiesEquals(map, "testMethodsAndLambdas",
                ImmutableProperty.builder().name("max").value("1").build());
        assertPropertiesEquals(map, "testLambdasOnly",
                ImmutableProperty.builder().name("tokens").value("LAMBDA").build());
        assertPropertiesEquals(map, "testMethodsOnly",
                ImmutableProperty.builder().name("tokens").value("METHOD_DEF").build());
        assertPropertiesEquals(map, "testWithReturnOnlyAsTokens",
                ImmutableProperty.builder().name("tokens").value("LITERAL_RETURN").build());
        assertPropertiesEquals(map, "testMaxForVoid",
                ImmutableProperty.builder().name("max").value("2").build(),
                ImmutableProperty.builder().name("maxForVoid").value("0").build());
    }

    @Test
    public void testAllModuleUnitTests() throws Exception {
        final String repoPath = "D:\\personal\\develop\\java\\checkstyle\\checkstyle";
        for (ModuleExtractInfo module : modules) {
            if (Arrays.asList(
//                    "AbstractClassNameCheck",
//                    "AtclauseOrderCheck",
                    "FileTabCharacterCheck",
                    "EmptyForIteratorPadCheck",
                    "EmptyForInitializerPadCheck",
                    "ConstantNameCheck",
                    "ClassFanOutComplexityCheck",
                    "BeforeExecutionExclusionFileFilter",
                    "Checker")
                    .contains(module.name())) {
                continue;
            }
            System.out.println(module.fullName());
            final File file = new File(repoPath, "src/test/java/" + module.fullName().replaceAll("\\.", "/") + "Test.java");
            UnitTestProcessorCheck.setModuleName(module.fullName());
            UnitTestProcessorCheck.clearUnitTestToPropertiesMap();
            CustomCheckProcessor.process(
                    file.getAbsolutePath(),
                    UnitTestProcessorCheck.class);
            final Map<String, Set<ModuleInfo.Property>> map =
                    UnitTestProcessorCheck.getUnitTestToPropertiesMap();
            final Set<String> expected = module.properties().stream()
                    .map(ModuleExtractInfo.ModuleProperty::name).collect(Collectors.toSet());
            final Set<String> actual = map.values().stream()
                    .flatMap(Collection::stream)
                    .map(ModuleInfo.Property::name)
                    .collect(Collectors.toSet());
            expected.remove("tokens");
            actual.remove("tokens");
            assertEquals(expected, actual);
            int a = 1;
        }
    }

    private static void assertPropertiesEquals(Map<String, Set<ModuleInfo.Property>> map,
                                               String key, ModuleInfo.Property... properties) {
        assertEquals("properties is wrong from UT:" + key,
                Arrays.stream(properties).collect(Collectors.toSet()), map.get(key));
    }
}