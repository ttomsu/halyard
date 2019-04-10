/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.netflix.spinnaker.halyard.deploy.spinnaker.v1.service.distributed.kubernetes.v2;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.config.secrets.EncryptedSecret;
import com.netflix.spinnaker.halyard.config.config.v1.secrets.SecretSessionManager;
import com.netflix.spinnaker.halyard.config.model.v1.providers.kubernetes.KubernetesAccount;
import com.netflix.spinnaker.halyard.core.error.v1.HalException;
import com.netflix.spinnaker.halyard.core.problem.v1.Problem;
import com.netflix.spinnaker.halyard.core.resource.v1.JinjaJarResource;
import com.netflix.spinnaker.halyard.core.resource.v1.TemplatedResource;
import io.fabric8.openshift.api.model.SecretSpec;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.spockframework.compiler.model.Spec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KubernetesV2Utils {
  private ObjectMapper mapper = new ObjectMapper();

  @Autowired
  private SecretSessionManager secretSessionManager;

  public List<String> kubectlPrefix(KubernetesAccount account) {
    List<String> command = new ArrayList<>();
    command.add("kubectl");

    if (account.usesServiceAccount()) {
      return command;
    }

    String context = account.getContext();
    if (context != null && !context.isEmpty()) {
      command.add("--context");
      command.add(context);
    }

    String kubeconfig;
    if (EncryptedSecret.isEncryptedSecret(account.getKubeconfigFile())) {
      kubeconfig = secretSessionManager.decryptAsFile(account.getKubeconfigFile());
    } else {
      kubeconfig = account.getKubeconfigFile();
    }
    if (kubeconfig != null && !kubeconfig.isEmpty()) {
      command.add("--kubeconfig");
      command.add(kubeconfig);
    }

    return command;
  }

  List<String> kubectlPodServiceCommand(KubernetesAccount account, String namespace, String service) {
    List<String> command = kubectlPrefix(account);

    if (StringUtils.isNotEmpty(namespace)) {
      command.add("-n=" + namespace);
    }

    command.add("get");
    command.add("po");

    command.add("-l=cluster=" + service);
    command.add("-o=jsonpath='{.items[0].metadata.name}'");

    return command;
  }

  List<String> kubectlConnectPodCommand(KubernetesAccount account, String namespace, String name, int port) {
    List<String> command = kubectlPrefix(account);

    if (StringUtils.isNotEmpty(namespace)) {
      command.add("-n=" + namespace);
    }

    command.add("port-forward");
    command.add(name);
    command.add(port + "");

    return command;
  }

  public SecretSpec createSecretSpec(String namespace, String clusterName, String name, List<ConfigPair> files) {
    SecretSpec spec = new SecretSpec();
    spec.resource = new JinjaJarResource("/kubernetes/manifests/secret.yml");
    return (SecretSpec) createConfigSpec(namespace, clusterName, name, files, spec, true /* base64Encode */);
  }

  public ConfigMapSpec createConfigMapSpec(String namespace, String clusterName, String name, List<ConfigPair> files) {
    ConfigMapSpec spec = new ConfigMapSpec();
    spec.resource = new JinjaJarResource("/kubernetes/manifests/configMap.yml");
    return (ConfigMapSpec) createConfigSpec(namespace, clusterName, name, files, spec, false /* base64Encode */);
  }

  public ConfigSpec createConfigSpec(String namespace, String clusterName, String name, List<ConfigPair> files, ConfigSpec spec, boolean base64Encode ) {
    Map<String, String> contentMap = new HashMap<>();
    for (ConfigPair pair: files) {
      String contents;
      if (pair.getContentString() != null) {
        if (base64Encode) {
          contents = new String(Base64.getEncoder().encode(pair.getContentString().getBytes()));
        } else {
          StringBuilder sb = new StringBuilder();
          BufferRecyclers.getJsonStringEncoder().quoteAsString(pair.getContentString(), sb);
          contents = sb.toString();
        }
      } else {
        try {
          if (base64Encode) {
            contents = new String(Base64.getEncoder().encode(IOUtils.toByteArray(new FileInputStream(pair.getContents()))));
          } else {
            String fileContent = IOUtils.toString(new FileInputStream(pair.getContents()));
            String rmJinja = StringUtils.removeAll(StringUtils.removeAll(fileContent, "\\{\\%"), "\\%\\}");

            StringBuilder sb = new StringBuilder();
            BufferRecyclers.getJsonStringEncoder().quoteAsString(rmJinja, sb);
            contents = sb.toString();
          }
        } catch (IOException e) {
          throw new HalException(Problem.Severity.FATAL, "Failed to read required config file: " + pair.getContents().getAbsolutePath() + ": " + e.getMessage(), e);
        }
      }

      contentMap.put(pair.getName(), contents);
    }

    spec.name = name + "-" + Math.abs(contentMap.hashCode());

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("files", contentMap);
    bindings.put("name", spec.name);
    bindings.put("namespace", namespace);
    bindings.put("clusterName", clusterName);

    spec.resource.extendBindings(bindings);

    return spec;
  }

  public String prettify(String input) {
    Yaml yaml = new Yaml(new SafeConstructor());
    return yaml.dump(yaml.load(input));
  }

  public Map<String, Object> parseManifest(String input) {
    Yaml yaml = new Yaml(new SafeConstructor());
    return mapper.convertValue(yaml.load(input), new TypeReference<Map<String, Object>>() {});
  }

  static private class ConfigSpec {
    TemplatedResource resource;
    String name;
  }

  static public class SecretSpec extends ConfigSpec {
  }

  static public class ConfigMapSpec extends ConfigSpec {
  }

  @Data
  static public class ConfigPair {
    File contents;
    String contentString;
    String name;

    public ConfigPair(File inputFile) {
      this(inputFile, inputFile);
    }

    public ConfigPair(File inputFile, File outputFile) {
      this.contents = inputFile;
      this.name = outputFile.getName();
    }

    public ConfigPair(String name, String contentString) {
      this.contentString = contentString;
      this.name = name;
    }
  }
}
