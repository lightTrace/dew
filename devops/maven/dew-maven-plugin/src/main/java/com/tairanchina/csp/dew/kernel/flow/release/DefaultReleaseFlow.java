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

package com.tairanchina.csp.dew.kernel.flow.release;

import com.ecfront.dew.common.$;
import com.tairanchina.csp.dew.helper.DockerHelper;
import com.tairanchina.csp.dew.helper.KubeHelper;
import com.tairanchina.csp.dew.helper.YamlHelper;
import com.tairanchina.csp.dew.kernel.Dew;
import com.tairanchina.csp.dew.kernel.flow.BasicFlow;
import com.tairanchina.csp.dew.kernel.resource.KubeConfigMapBuilder;
import com.tairanchina.csp.dew.kernel.resource.KubeDeploymentBuilder;
import com.tairanchina.csp.dew.kernel.resource.KubeServiceBuilder;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Service;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DefaultReleaseFlow extends BasicFlow {

    public boolean process() throws ApiException, IOException, MojoExecutionException {
        Dew.log.info("Building kubernetes resources");
        Map<String, Object> deployResult = buildResources();
        release(deployResult, Dew.Config.getCurrentProject().getGitCommit(), false);
        if (Dew.Config.getCurrentProject().getApp().getRevisionHistoryLimit() > 0) {
            Dew.log.debug("Delete old version from kubernetes resources and docker images");
            removeOldVersions(Dew.Config.getCurrentProject().getApp().getRevisionHistoryLimit());
        }
        return true;
    }

    public void release(Map<String, Object> deployResult, String gitCommit, boolean reRelease) throws ApiException, IOException {
        appendVersionInfo(deployResult, gitCommit, reRelease);
        Dew.log.debug("Add version to ConfigMap");
        Dew.log.info("Publishing kubernetes resources");
        deployResources(deployResult);
        Dew.log.debug("Enabled version");
        enabledVersionInfo(gitCommit);
    }

    private Map<String, Object> buildResources() {
        ExtensionsV1beta1Deployment deployment = new KubeDeploymentBuilder().build(Dew.Config.getCurrentProject());
        V1Service service = new KubeServiceBuilder().build(Dew.Config.getCurrentProject());
        return new HashMap<String, Object>() {{
            put(KubeHelper.RES.DEPLOYMENT.getVal(), deployment);
            put(KubeHelper.RES.SERVICE.getVal(), service);
        }};
    }

    private void deployResources(Map<String, Object> kubeResources) throws ApiException, IOException {
        V1Service service = (V1Service) kubeResources.get(KubeHelper.RES.SERVICE.getVal());
        ExtensionsV1beta1Deployment deployment = (ExtensionsV1beta1Deployment) kubeResources.get(KubeHelper.RES.DEPLOYMENT.getVal());
        KubeHelper.apply(deployment, Dew.Config.getCurrentProject().getId());
        // 等待一段时间以清除之前的状态
        try {
            // TODO 更优雅的处理
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        CountDownLatch cdl = new CountDownLatch(1);
        ExtensionsV1beta1Deployment deploymentRes = (ExtensionsV1beta1Deployment) kubeResources.get(KubeHelper.RES.DEPLOYMENT.getVal());
        String watchId = KubeHelper.watch((coreApi, extensionsApi, rbacAuthorizationApi,autoscalingApi)
                        -> extensionsApi.listNamespacedDeploymentCall(deploymentRes.getMetadata().getNamespace(),
                null, null, null, null,
                "app=" + deploymentRes.getMetadata().getName(), 1, null, null, Boolean.TRUE, null, null),
                resp -> {
                    if (resp.object.getStatus().getReadyReplicas() != null
                            && resp.object.getStatus().getReadyReplicas().intValue() == resp.object.getSpec().getReplicas()) {
                        cdl.countDown();
                    }
                },
                ExtensionsV1beta1Deployment.class,
                Dew.Config.getCurrentProject().getId());
        try {
            cdl.await(30, TimeUnit.MINUTES);
            KubeHelper.stopWatch(watchId, Dew.Config.getCurrentProject().getId());
            if (!KubeHelper.exist(service.getMetadata().getName(), service.getMetadata().getNamespace(), KubeHelper.RES.SERVICE, Dew.Config.getCurrentProject().getId())) {
                KubeHelper.create(service, Dew.Config.getCurrentProject().getId());
            } else {
                KubeHelper.patch(service.getMetadata().getName(), new KubeServiceBuilder().buildPatch(service), service.getMetadata().getNamespace(), KubeHelper.RES.SERVICE, Dew.Config.getCurrentProject().getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Dew.log.error("Publish error,maybe timeout.", e);
            throw new RuntimeException(e);
        }
    }

    private void appendVersionInfo(Map<String, Object> kubeResources, String gitCommit, boolean reRelease) throws ApiException {
        Map<String, String> resources = kubeResources.entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> {
                            try {
                                return $.security.encodeStringToBase64(KubeHelper.toString(entry.getValue()), "UTF-8");
                            } catch (UnsupportedEncodingException ignore) {
                                return "";
                            }
                        }));
        V1ConfigMap currVerConfigMap = new KubeConfigMapBuilder().build(
                getVersionName(gitCommit), new HashMap<String, String>() {{
                    put(FLAG_VERSION_APP, Dew.Config.getCurrentProject().getAppName());
                    put(FLAG_KUBE_RESOURCE_GIT_COMMIT, gitCommit);
                    put(FLAG_VERSION_KIND, "version");
                    put(FLAG_VERSION_ENABLED, "false");
                    put(FLAG_VERSION_LAST_UPDATE_TIME, System.currentTimeMillis() + "");
                    put(FLAG_VERSION_RE_RELEASE, reRelease + "");
                }}, resources);
        KubeHelper.apply(currVerConfigMap, Dew.Config.getCurrentProject().getId());
    }

    private void enabledVersionInfo(String gitCommit) throws ApiException {
        KubeHelper.patch(getVersionName(gitCommit), new ArrayList<String>() {{
            add("{\"op\":\"replace\",\"path\":\"/metadata/labels/enabled\",\"value\":\"true\"}");
        }}, Dew.Config.getCurrentProject().getNamespace(), KubeHelper.RES.CONFIG_MAP, Dew.Config.getCurrentProject().getId());
    }

    private void removeOldVersions(int revisionHistoryLimit) throws ApiException, IOException {
        List<V1ConfigMap> verConfigMaps = getVersionHistory(false);
        int offset = revisionHistoryLimit;
        for (V1ConfigMap configMap : verConfigMaps) {
            boolean enabled = configMap.getMetadata().getLabels().get(FLAG_VERSION_ENABLED).equalsIgnoreCase("true");
            if (enabled) {
                offset--;
            }
            if (!enabled || offset <= 0) {
                String oldGitCommit = configMap.getMetadata().getLabels().get(FLAG_KUBE_RESOURCE_GIT_COMMIT);
                Dew.log.debug("Remove old version : " + configMap.getMetadata().getName());
                KubeHelper.delete(configMap.getMetadata().getName(), Dew.Config.getCurrentProject().getNamespace(), KubeHelper.RES.CONFIG_MAP, Dew.Config.getCurrentProject().getId());
                DockerHelper.Image.remove(Dew.Config.getCurrentProject().getImageName(oldGitCommit), Dew.Config.getCurrentProject().getId());
                DockerHelper.Registry.remove(Dew.Config.getCurrentProject().getImageName(oldGitCommit), Dew.Config.getCurrentProject().getId());
            }
        }
    }

    protected String getVersionName(String gitCommit) {
        return "ver." + Dew.Config.getCurrentProject().getAppName() + "." + gitCommit;
    }

}
