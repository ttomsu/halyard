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

import com.netflix.spinnaker.halyard.cli.services.v1.Daemon;
import com.netflix.spinnaker.halyard.cli.services.v1.OperationHandler;
import com.netflix.spinnaker.halyard.config.model.v1.security.AuthnMethod;

public class ExecutableAuthnMethodEdit<T extends AuthnMethod> {

  private AuthenticationMethod.Editable subject;

  public ExecutableAuthnMethodEdit(AuthenticationMethod.Editable subject) {
    this.subject = subject;
  }

  public void executeThis(String currentDeployment, String method, boolean validate) {
    // Disable validation here, since we don't want an illegal config to prevent us from fixing it.
    AuthnMethod authnMethod = new OperationHandler<AuthnMethod>()
        .setOperation(Daemon.getAuthnMethod(currentDeployment, method, false))
        .setFailureMesssage("Failed to get " + method + " method.")
        .get();

    new OperationHandler<Void>()
        .setOperation(Daemon.setAuthnMethod(currentDeployment, method, validate, subject.editAuthnMethod((T) authnMethod)))
        .setFailureMesssage("Failed to edit " + method + " method.")
        .setSuccessMessage("Successfully edited " + method + " method.")
        .get();
  }
}
