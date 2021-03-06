/*
 * Copyright 2019. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tairanchina.csp.dew.core.cluster.spi.rabbit.tracing;

import io.opentracing.tag.StringTag;

final class RabbitMqTracingTags {

  static final StringTag RABBITMQ = new StringTag("rabbitmq");
  static final StringTag MESSAGE_ID = new StringTag("messageid");
  static final StringTag ROUTING_KEY = new StringTag("routingkey");
  static final StringTag CONSUMER_QUEUE = new StringTag("consumerqueue");
  static final StringTag EXCHANGE = new StringTag("exchange");
  static final String SPAN_KIND_PRODUCER = RABBITMQ.getKey() + "-send";
  static final String SPAN_KIND_CONSUMER = RABBITMQ.getKey() + "-receive";

}
