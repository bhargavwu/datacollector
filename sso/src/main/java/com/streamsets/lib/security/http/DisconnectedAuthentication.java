/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamsets.lib.security.http;

import com.streamsets.datacollector.util.Configuration;
import com.streamsets.pipeline.api.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DisconnectedAuthentication implements Authentication {
  private static final Logger LOG = LoggerFactory.getLogger(DisconnectedAuthentication.class);

  private final File file;
  private DisconnectedSessionHandler sessionHandler;
  private PasswordHasher passwordHasher;
  private DisconnectedSecurityInfo info;

  public DisconnectedAuthentication(File file) {
    this.file = file;
    reset();
  }

  public void reset() {
    this.sessionHandler = new DisconnectedSessionHandler();
    this.passwordHasher = new PasswordHasher(new Configuration());
    try {
      if (file.exists()) {
        LOG.info("Loaded credentials file '{}'", file.getAbsolutePath());
        info = DisconnectedSecurityInfo.fromJsonFile(file);
      } else {
        LOG.warn("Credentials file '{}' does not exist", file.getAbsolutePath());
        info = null;
      }
    } catch (IOException ex) {
      LOG.error("Could not read disconnected security info file '{}': {}",
          file.getAbsoluteFile(),
          ex.toString(),
          ex);
      info = null;
    }
  }

  @Override
  public SSOPrincipal validateUserCredentials(String userName, String password, String ipAddress) {
    SSOPrincipalJson principal = null;
    if (info != null) {
      DisconnectedSecurityInfo.Entry entry = info.getEntry(userName);
      if (entry != null) {
        if (passwordHasher.verify(entry.getPasswordHash(), userName, password)) {
          principal = new SSOPrincipalJson();
          principal.setPrincipalId(userName);
          principal.setPrincipalName("-");
          principal.setEmail("-");
          principal.setOrganizationId(userName.substring(userName.indexOf("@") + 1)); // org is extracted from username
          principal.setOrganizationName("-");
          principal.getRoles().addAll(entry.getRoles());
          principal.setTokenStr(UUID.randomUUID().toString());
          principal.setExpires(-1);
          principal.setRequestIpAddress(ipAddress);
          principal.lock();
        }
      }
      if (principal != null) {
        LOG.info("Successful disconnected authentication for '{}' from '{}'", userName, ipAddress);
      } else {
        LOG.info("Failed disconnected authentication for '{}' from '{}'", userName, ipAddress);
      }
    } else {
      LOG.warn("No credentials available, check earlier WARN or ERROR messages");
    }
    return principal;
  }

  @Override
  public void registerSession(SSOPrincipal principal) {
    sessionHandler.add(principal);
  }

  public DisconnectedSessionHandler getSessionHandler() {
    return sessionHandler;
  }

}
