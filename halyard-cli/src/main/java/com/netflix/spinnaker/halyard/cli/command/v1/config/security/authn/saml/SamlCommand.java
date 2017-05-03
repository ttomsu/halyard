/*
 * Copyright 2017 Google, Inc.
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
 */

package com.netflix.spinnaker.halyard.cli.command.v1.config.security.authn.saml;

import com.beust.jcommander.Parameters;
import com.netflix.spinnaker.halyard.cli.command.v1.config.AbstractConfigCommand;
import com.netflix.spinnaker.halyard.cli.command.v1.config.security.EnableableCommand;
import com.netflix.spinnaker.halyard.cli.command.v1.config.security.authn.AuthenticationMethod;
import com.netflix.spinnaker.halyard.cli.command.v1.config.security.authn.EnableDisableAuthnMethod;
import com.netflix.spinnaker.halyard.cli.command.v1.config.security.authn.ExecutableAuthnMethod;
import com.netflix.spinnaker.halyard.config.model.v1.security.AuthnMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;

@Data
@EqualsAndHashCode(callSuper = false)
@Parameters(separators = "=")
public class SamlCommand extends AbstractConfigCommand implements EnableableCommand,
                                                                  AuthenticationMethod {
  private String commandName = "saml";
  private String description = "Configure the SAML method for authenticating.";
  private AuthnMethod.Method method = AuthnMethod.Method.SAML;

  @Delegate
  private EnableDisableAuthnMethod enableDisableAuthnMethod = new EnableDisableAuthnMethod(method);

  public SamlCommand() {
    super();
    registerSubcommand(new SamlEditCommand());
  }

  @Override
  protected void executeThis() {
    new ExecutableAuthnMethod().executeThis(getCurrentDeployment(), method.id, !noValidate);
  }
}
