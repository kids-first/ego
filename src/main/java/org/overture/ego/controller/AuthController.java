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

package org.overture.ego.controller;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.provider.facebook.FacebookTokenService;
import org.overture.ego.provider.google.GoogleTokenService;
import org.overture.ego.provider.orcid.ORCIDTokenService;
import org.overture.ego.token.IDToken;
import org.overture.ego.token.TokenService;
import org.overture.ego.token.signer.TokenSigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/oauth")
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthController {
    private TokenService tokenService;
    private GoogleTokenService googleTokenService;
    private FacebookTokenService facebookTokenService;
    private ORCIDTokenService orcidTokenService;
    private TokenSigner tokenSigner;

    @RequestMapping(method = RequestMethod.GET, value = "/google/token")
    @ResponseStatus(value = HttpStatus.OK)
    @SneakyThrows
    public @ResponseBody
    String exchangeGoogleTokenForAuth(
            @RequestHeader(value = "token") final String idToken) {
        if (!googleTokenService.validToken(idToken))
            throw new InvalidTokenException("Invalid user token:" + idToken);
        val authInfo = googleTokenService.decode(idToken);
        return tokenService.generateUserToken(authInfo);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/facebook/token")
    @ResponseStatus(value = HttpStatus.OK)
    @SneakyThrows
    public @ResponseBody
    String exchangeFacebookTokenForAuth(
            @RequestHeader(value = "token") final String idToken) {
        if (!facebookTokenService.validToken(idToken))
            throw new InvalidTokenException("Invalid user token:" + idToken);
        val authInfo = facebookTokenService.getAuthInfo(idToken);
        if (authInfo.isPresent()) {
            return tokenService.generateUserToken(authInfo.get());
        } else {
            throw new InvalidTokenException("Unable to generate auth token for this user");
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/orcid/token")
    @ResponseStatus(value = HttpStatus.OK)
    @SneakyThrows
    public @ResponseBody
    ResponseEntity<String> exchangeORCIDTokenForAuth(
            @RequestHeader(value = "code") final String code) {
        val result = orcidTokenService.getAuthInfo(code);
        if (result.isValid()) {
            return new ResponseEntity<>(tokenService.generateUserToken(result.getToken()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result.getError(), new HttpHeaders(),
                    HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/dev/token")
    @ResponseStatus(value = HttpStatus.OK)
    @ConditionalOnExpression("${enable.dev.token}")
    @SneakyThrows
    public @ResponseBody
    ResponseEntity<String> exchangeDevTokenForAuth(@RequestParam(value = "email") final String email,@RequestParam(value = "lastName") final String lastName,@RequestParam(value = "firstName") final String firstName) {
        val token = new IDToken(email, firstName, lastName);
        return new ResponseEntity<>(tokenService.generateUserToken(token), HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.GET, value = "/token/verify")
    @ResponseStatus(value = HttpStatus.OK)
    @SneakyThrows
    public @ResponseBody
    boolean verifyJWToken(
            @RequestHeader(value = "token") final String token) {
        if (StringUtils.isEmpty(token)) {
            throw new InvalidTokenException("Token is empty");
        }

        if (!tokenService.validateToken(token)) {
            throw new InvalidTokenException("Token failed validation");
        }
        return true;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/token/public_key")
    @ResponseStatus(value = HttpStatus.OK)
    public @ResponseBody
    String getPublicKey() {
        val pubKey = tokenSigner.getEncodedPublicKey();
        return pubKey.orElse("");
    }

    @ExceptionHandler({InvalidTokenException.class})
    public ResponseEntity<Object> handleInvalidTokenException(HttpServletRequest req, InvalidTokenException ex) {
        log.error("ID Token not found.");
        return new ResponseEntity<>("Invalid ID Token provided.", new HttpHeaders(),
                HttpStatus.BAD_REQUEST);
    }

}
