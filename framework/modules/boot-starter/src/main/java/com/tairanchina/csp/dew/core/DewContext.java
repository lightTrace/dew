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

package com.tairanchina.csp.dew.core;

import com.ecfront.dew.common.$;
import com.tairanchina.csp.dew.Dew;
import com.tairanchina.csp.dew.core.auth.dto.OptInfo;

import java.util.Optional;

/**
 * Dew 上下文处理
 */
public class DewContext {

    private static final ThreadLocal<DewContext> CONTEXT = new ThreadLocal<>();

    private static Class optInfoClazz = OptInfo.class;

    /**
     * 当次请求的ID
     */
    private String id;
    /**
     * 请求来源IP
     */
    private String sourceIP;
    /**
     * 请求最初的URL
     */
    private String requestUri;
    /**
     * 请求对应的token
     */
    private String token;

    private Optional innerOptInfo = Optional.empty();

    public static <E extends OptInfo> Class<E> getOptInfoClazz() {
        return optInfoClazz;
    }

    /**
     * 设置自定义的OptInfo
     *
     * @param optInfoClazz
     */
    public static <E extends OptInfo> void setOptInfoClazz(Class<E> optInfoClazz) {
        DewContext.optInfoClazz = optInfoClazz;
    }

    public <E extends OptInfo> Optional<E> optInfo() {
        if (innerOptInfo.isPresent()) {
            return innerOptInfo;
        }
        if (token != null && !token.isEmpty()) {
            innerOptInfo = Dew.auth.getOptInfo(token);
        } else {
            innerOptInfo = Optional.empty();
        }
        return innerOptInfo;
    }

    public static DewContext getContext() {
        DewContext cxt = CONTEXT.get();
        if (cxt == null) {
            cxt = new DewContext();
            cxt.id = $.field.createUUID();
            cxt.sourceIP = Dew.Info.ip;
            cxt.requestUri = "";
            cxt.token = "";
            setContext(cxt);
        }
        return cxt;
    }

    public static boolean exist() {
        return CONTEXT.get() != null;
    }

    public static void setContext(DewContext _context) {
        if (_context.token == null) {
            _context.token = "";
        }
        CONTEXT.set(_context);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public void setInnerOptInfo(Optional innerOptInfo) {
        this.innerOptInfo = innerOptInfo;
    }

}
