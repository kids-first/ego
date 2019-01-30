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

package bio.overture.ego.controller;

import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static java.lang.String.format;

import bio.overture.ego.model.dto.TokenResponse;
import bio.overture.ego.model.dto.TokenScopeResponse;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.security.ApplicationScoped;
import bio.overture.ego.service.TokenService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/o")
public class TokenController {

  private final TokenService tokenService;

  @Autowired
  public TokenController(@NonNull TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @ApplicationScoped()
  @RequestMapping(method = RequestMethod.POST, value = "/check_token")
  @ResponseStatus(value = HttpStatus.MULTI_STATUS)
  @SneakyThrows
  public @ResponseBody TokenScopeResponse checkToken(
      @RequestHeader(value = "Authorization") final String authToken,
      @RequestParam(value = "token") final String token) {

    return tokenService.checkToken(authToken, token);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/issue_token")
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody TokenResponse issueToken(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "user_id") UUID user_id,
      @RequestParam(value = "scopes") ArrayList<String> scopes,
      @RequestParam(value = "applications", required = false) ArrayList<UUID> applications) {
    val scopeNames = mapToList(scopes, s -> new ScopeName(s));
    val t = tokenService.issueToken(user_id, scopeNames, applications);
    Set<String> issuedScopes = mapToSet(t.scopes(), x -> x.toString());
    TokenResponse response =
        new TokenResponse(t.getName(), issuedScopes, t.getSecondsUntilExpiry());
    return response;
  }

  @RequestMapping(method = RequestMethod.POST, value = "/revoke_token")
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody String revokeToken(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "user_id") UUID user_id,
      @RequestParam(value = "token") final String token) {
    tokenService.revokeToken(user_id, token);
    return format("Token '%s' is successfully revoked!", token);
  }

  @ResponseBody
  List<TokenResponse> listTokens(@RequestHeader(value = "Authorization") String authorization) {
    return null;
  }

  @ExceptionHandler({InvalidTokenException.class})
  public ResponseEntity<Object> handleInvalidTokenException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("ID ScopedAccessToken not found.:%s", ex.toString()));
    return new ResponseEntity<>(
        format("{\"error\": \"Invalid ID ScopedAccessToken provided:'%s'\"}", ex.toString()),
        new HttpHeaders(),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({InvalidScopeException.class})
  public ResponseEntity<Object> handleInvalidScopeException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("Invalid PolicyIdStringWithMaskName: %s", ex.getMessage()));
    return new ResponseEntity<>(
        "{\"error\": \"%s\"}".format(ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({UsernameNotFoundException.class})
  public ResponseEntity<Object> handleUserNotFoundException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("User not found: %s", ex.getMessage()));
    return new ResponseEntity<>(
        "{\"error\": \"%s\"}".format(ex.getMessage()), HttpStatus.BAD_REQUEST);
  }
}
