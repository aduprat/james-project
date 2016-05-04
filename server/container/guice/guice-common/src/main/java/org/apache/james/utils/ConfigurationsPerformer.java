/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.james.lifecycle.api.Configurable;

import com.github.fge.lambdas.Throwing;
import com.google.inject.Inject;

@SuppressWarnings("rawtypes")
public class ConfigurationsPerformer {

    private final Set<ConfigurationPerformer> configurationPerformers;
    private final List<Class<? extends Configurable>> configurables;

    @Inject
    public ConfigurationsPerformer(Set<ConfigurationPerformer> configurationPerformers, Configurables configurables) {
        this.configurationPerformers = configurationPerformers;
        this.configurables = configurables.get();
    }

    public void initModules() throws Exception {
        
        Set<ConfigurationPerformer> processed = configurables.stream()
            .flatMap(configurable -> configurationPerformerFor(configurable, configurationPerformers))
            .peek(Throwing.consumer(x -> x.initModule()))
            .collect(Collectors.toSet());
        
        configurationPerformers.stream()
            .filter(x -> !processed.contains(x))
            .forEach(Throwing.consumer(x -> x.initModule()));
    }

    private Stream<ConfigurationPerformer> configurationPerformerFor(Class<? extends Configurable> configurable, Set<ConfigurationPerformer> configurationPerformers) {
        return configurationPerformers.stream().filter(x -> x.forClass().equals(configurable));
    }

}
