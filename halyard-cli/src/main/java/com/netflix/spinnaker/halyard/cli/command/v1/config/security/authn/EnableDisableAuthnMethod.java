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

package com.netflix.spinnaker.halyard.cli.command.v1.config.security.authn;

import com.beust.jcommander.Parameters;
import com.netflix.spinnaker.halyard.cli.command.v1.NestableCommand;
import com.netflix.spinnaker.halyard.cli.command.v1.config.AbstractConfigCommand;
import com.netflix.spinnaker.halyard.cli.command.v1.config.security.EnableableCommand;
import com.netflix.spinnaker.halyard.cli.services.v1.Daemon;
import com.netflix.spinnaker.halyard.cli.services.v1.OperationHandler;
import com.netflix.spinnaker.halyard.config.model.v1.security.AuthnMethod;
import lombok.AllArgsConstructor;

public class EnableDisableAuthnMethod implements EnableableCommand {

  private AuthnMethod.Method method;

  public EnableDisableAuthnMethod(AuthnMethod.Method method) {
    this.method = method;
  }

  @Override
  public NestableCommand enableCommand() {
    return new AuthnMethodEnableDisableCommand(true);
  }

  @Override
  public NestableCommand disableCommand() {
    return new AuthnMethodEnableDisableCommand(false);
  }

  @AllArgsConstructor
  @Parameters(separators = "=")
  class AuthnMethodEnableDisableCommand extends AbstractConfigCommand {

    private boolean enabled;

    @Override
    public String getCommandName() {
      return enabled ? "enable" : "disable";
    }

    private String subjunctivePerfectAction() {
      return enabled ? "enabled" : "disabled";
    }

    private String indicativePastPerfectAction() {
      return enabled ? "enabled" : "disabled";
    }

    @Override
    public String getDescription() {
      return "Set the " + method.id + " method as " + subjunctivePerfectAction();
    }

    @Override
    protected void executeThis() {
      String currentDeployment = getCurrentDeployment();
      String methodName = method.id;
      new OperationHandler<Void>()
          .setSuccessMessage("Successfully " + indicativePastPerfectAction() + " " + methodName)
          .setFailureMesssage("Failed to " + getCommandName() + " " + methodName)
          .setOperation(Daemon.setAuthnMethodEnabled(currentDeployment, methodName, !noValidate, enabled))
          .get();
    }
  }
}
