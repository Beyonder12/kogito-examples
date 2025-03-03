/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kogito.serverless.examples.services;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import org.kogito.serverless.examples.input.Country;

@ApplicationScoped
public class PopulationService {

    private Map<String, String> populations = new HashMap<>();

    public PopulationService() {
        populations.put("Brazil", "211,000,000");
        populations.put("USA", "6,000,000");
        populations.put("Serbia", "6,945,000");
        populations.put("Germany", "83,020,000");
        populations.put("N/A", "N/A");
    }

    public Country getPopulation(Country country) {
        country.setPopulation(populations.get(country.getName()));
        return country;
    }
}
