/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.studio.ops.ai.tool.handler.topic;

import org.apache.rocketmq.studio.common.domain.enums.TopicPerm;
import org.apache.rocketmq.studio.common.domain.enums.TopicType;
import org.apache.rocketmq.studio.instance.topic.MetadataService;
import org.apache.rocketmq.studio.instance.topic.TopicVO;
import org.apache.rocketmq.studio.ops.ai.tool.contract.common.ListOutput;
import org.apache.rocketmq.studio.ops.ai.tool.contract.topic.TopicListInput;
import org.apache.rocketmq.studio.ops.ai.tool.contract.topic.TopicListItem;
import org.apache.rocketmq.studio.ops.ai.tool.core.ToolExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicListToolHandlerTest {

    @Mock
    private MetadataService metadataService;

    @InjectMocks
    private TopicListToolHandler handler;

    @Test
    void listShouldProjectEveryTopicFieldForTheContextInstanceTest() {
        assertThat(handler.name()).isEqualTo("rmq.topic.list");
        when(metadataService.listTopics("instance-a", null, "NORMAL", "order")).thenReturn(List.of(topic()));

        ListOutput<TopicListItem> output = handler.execute(input("untrusted-instance", "order", "NORMAL"), context());

        assertThat(output.items()).containsExactly(new TopicListItem(
                "orders", "cluster-a", TopicType.NORMAL, 8, 8, TopicPerm.RW, 3L, 0.5d, 2));
    }

    @Test
    void listShouldIgnoreTheInstanceIdCarriedInTheToolInputTest() {
        // The instance is a control field: an untrusted tool input must not be able to pick another one.
        when(metadataService.listTopics("instance-a", null, null, null)).thenReturn(List.of());

        handler.execute(input("attacker-instance", null, null), context());

        verify(metadataService).listTopics("instance-a", null, null, null);
        verify(metadataService, never()).listTopics("attacker-instance", null, null, null);
    }

    @Test
    void listShouldReturnAnEmptyOutputWhenNoTopicMatchesTest() {
        when(metadataService.listTopics("instance-a", null, "NORMAL", "nope")).thenReturn(List.of());

        ListOutput<TopicListItem> output = handler.execute(input(null, "nope", "NORMAL"), context());

        assertThat(output.items()).isEmpty();
    }

    private static TopicListInput input(String instanceId, String topicName, String type) {
        return new TopicListInput(instanceId, topicName, type);
    }

    private static ToolExecutionContext context() {
        return ToolExecutionContext.of(
                "instance-a", null, Map.of("instanceId", "instance-a"));
    }

    private static TopicVO topic() {
        TopicVO topic = new TopicVO();
        topic.setName("orders");
        topic.setClusterId("cluster-a");
        topic.setInstanceId("instance-a");
        topic.setType(TopicType.NORMAL);
        topic.setWriteQueues(8);
        topic.setReadQueues(8);
        topic.setPerm(TopicPerm.RW);
        topic.setMessageCount(3L);
        topic.setTps(0.5d);
        topic.setConsumerGroupCount(2);
        return topic;
    }
}