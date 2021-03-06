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

package com.tairanchina.csp.dew.example;

import com.ecfront.dew.common.$;
import org.junit.Test;

import java.io.IOException;

/**
 * desription:
 * Created by ding on 2018/2/1.
 */
public class WebTest {

    @Test
    public void testPre() throws IOException {
        for (int i = 0; i < 5000; i++) {
            $.http.get("http://127.0.0.1:8080/example");
        }
    }
}
