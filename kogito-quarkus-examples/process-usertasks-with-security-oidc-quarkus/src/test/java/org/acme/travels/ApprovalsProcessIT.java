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
package org.acme.travels;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.kogito.Model;
import org.kie.kogito.auth.IdentityProvider;
import org.kie.kogito.auth.IdentityProviders;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;
import org.kie.kogito.usertask.UserTaskInstance;
import org.kie.kogito.usertask.UserTasks;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class ApprovalsProcessIT {

    @Named("approvals")
    @Inject
    Process<? extends Model> approvalsProcess;

    @Inject
    UserTasks userTasks;

    @Test
    public void testApprovalProcess() {

        assertNotNull(approvalsProcess);

        Model m = approvalsProcess.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("traveller", new Traveller("John", "Doe", "john.doe@example.com", "American", new Address("main street", "Boston", "10005", "US")));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = approvalsProcess.createInstance(m);
        processInstance.start();
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        SecurityPolicy policy = SecurityPolicy.of(IdentityProviders.of("admin", Arrays.asList("managers")));

        processInstance.workItems(policy);

        List<WorkItem> workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());
        Map<String, Object> results = new HashMap<>();
        results.put("approved", true);
        processInstance.completeWorkItem(workItems.get(0).getId(), results, policy);

        policy = SecurityPolicy.of(IdentityProviders.of("admin", Arrays.asList("mgmt")));
        workItems = processInstance.workItems(policy);
        assertEquals(0, workItems.size());

        policy = SecurityPolicy.of(IdentityProviders.of("john", Arrays.asList("managers")));
        processInstance.workItems(policy);

        workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());

        results.put("approved", false);
        processInstance.completeWorkItem(workItems.get(0).getId(), results, policy);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.status());

        Model result = (Model) processInstance.variables();
        assertEquals(4, result.toMap().size());
        assertEquals(result.toMap().get("approver"), "manager");
        assertEquals(result.toMap().get("firstLineApproval"), true);
        assertEquals(result.toMap().get("secondLineApproval"), false);
    }

    @Test
    public void testApprovalProcessViaPhases() {

        assertNotNull(approvalsProcess);

        Model m = approvalsProcess.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("traveller", new Traveller("John", "Doe", "john.doe@example.com", "American", new Address("main street", "Boston", "10005", "US")));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = approvalsProcess.createInstance(m);
        processInstance.start();
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        SecurityPolicy policy = SecurityPolicy.of(IdentityProviders.of("admin", Arrays.asList("managers")));

        processInstance.workItems(policy);

        List<WorkItem> workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());

        IdentityProvider managerUser = IdentityProviders.of("manager");
        List<UserTaskInstance> userTasksInstances = userTasks.instances().findByIdentity(managerUser);
        userTasksInstances.forEach(ut -> {
            ut.setOutput("approved", true);
            ut.transition("complete", emptyMap(), managerUser);
        });

        policy = SecurityPolicy.of(IdentityProviders.of("admin", Arrays.asList("mgmt")));
        workItems = processInstance.workItems(policy);
        assertEquals(0, workItems.size());

        policy = SecurityPolicy.of(IdentityProviders.of("john", Arrays.asList("managers")));

        processInstance.workItems(policy);

        workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());

        IdentityProvider johnUser = IdentityProviders.of("john", List.of("managers"));
        userTasksInstances = userTasks.instances().findByIdentity(johnUser);
        userTasksInstances.forEach(ut -> {
            ut.transition("claim", emptyMap(), johnUser);
            ut.setOutput("approved", false);
            ut.transition("complete", emptyMap(), johnUser);
        });

        assertThat(approvalsProcess.instances().findById(processInstance.id())).isEmpty();

    }
}
