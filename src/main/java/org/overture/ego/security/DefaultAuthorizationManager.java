/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
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

package org.overture.ego.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

/*
  Default Authorization Manager allows working without actual auth headers.
  Meant to be used for development environment.
 */
@Slf4j
public class DefaultAuthorizationManager implements AuthorizationManager {

  @Override
  public boolean authorize(Authentication authentication) {
    return true;
  }

  @Override
  public boolean authorizeWithAdminRole(Authentication authentication) {
    return true;
  }

  @Override
  public boolean authorizeWithGroup(Authentication authentication, String groupName) {
    return true;
  }

  @Override
  public boolean authorizeWithApplication(Authentication authentication, String appName) {
    return true;
  }
}
