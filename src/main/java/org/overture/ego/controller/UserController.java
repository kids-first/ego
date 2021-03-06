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

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.dto.PageDTO;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.model.entity.UserPermission;
import org.overture.ego.model.exceptions.PostWithIdentifierException;
import org.overture.ego.model.params.Scope;
import org.overture.ego.model.search.Filters;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.security.AdminScoped;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.overture.ego.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class UserController {
  /**
   * Dependencies
   */

  private final UserService userService;
  private final GroupService groupService;
  private final ApplicationService applicationService;

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "")
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
            value = "Results to retrieve"),
          @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
            value = "Index of first result to retrieve"),
          @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                  value = "Field to sort on"),
          @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
                  value = "Sorting order: ASC|DESC. Default order: DESC"),
          @ApiImplicitParam(name = "status", dataType = "string", paramType = "query",
                  value = "Filter by status. " +
                          "You could also specify filters on any field of the entity being queried as " +
                          "query parameters in this format: name=something")

  })
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Page of Users", response = PageDTO.class)
      }
  )

  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<User> getUsersList(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @ApiParam(value="Query string compares to Users Name, Email, First Name, and Last Name fields.", required=false ) @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(userService.listUsers(filters, pageable));
    } else {
      return new PageDTO<>(userService.findUsers(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
          value = {
            @ApiResponse(code = 200, message = "Create new user", response = User.class),
            @ApiResponse(code = 400, message = PostWithIdentifierException.reason, response = User.class)
          }
  )
  public @ResponseBody
  User create(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @RequestBody(required = true) User userInfo) {
    if (userInfo.getId() != null) {
      throw new PostWithIdentifierException();
    }
    return userService.create(userInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "User Details", response = User.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  User getUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id) {
    return  userService.get(id);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Updated user info", response = User.class)
      }
  )
  public @ResponseBody
  User updateUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) User updatedUserInfo) {
    return userService.update(updatedUserInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId) {
    userService.delete(userId);
  }

  /*
   Permissions related endpoints
    */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/permissions")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
          value = "Results to retrieve"),
      @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
          value = "Index of first result to retrieve"),
      @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
          value = "Field to sort on"),
      @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
          value = "Sorting order: ASC|DESC. Default order: DESC")
  })
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Page of user permissions", response = PageDTO.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<UserPermission> getPermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id,
      Pageable pageable)
  {
    return new PageDTO<>(userService.getUserPermissions(id, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permissions")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Add user permissions", response = User.class)
      }
  )
  public @ResponseBody
  User addPermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id,
      @RequestBody(required = true) List<Scope> permissions
  ) {
    return userService.addUserPermissions(id, permissions);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/permissions/{permissionIds}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Delete user permissions")
      }
  )
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id,
      @PathVariable(value = "permissionIds", required = true) List<String> permissionIds) {
    userService.deleteUserPermissions(id,permissionIds);
  }

  /*
   Groups related endpoints
    */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
                  value = "Results to retrieve"),
          @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
                  value = "Index of first result to retrieve"),
          @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                  value = "Field to sort on"),
          @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
                  value = "Sorting order: ASC|DESC. Default order: DESC"),
          @ApiImplicitParam(name = "status", dataType = "string", paramType = "query",
                  value = "Filter by status. " +
                          "You could also specify filters on any field of the entity being queried as " +
                          "query parameters in this format: name=something")

  })
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of Groups of user", response = PageDTO.class)
          }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Group> getUsersGroups(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(groupService.findUserGroups(userId, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findUserGroups(userId, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/groups")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Add Groups to user", response = User.class)
          }
  )
  public @ResponseBody
  User addGroupsToUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestBody(required = true) List<String> groupIDs) {

    return userService.addUserToGroups(userId,groupIDs);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/groups/{groupIDs}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Delete Groups from User")
          }
  )
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteGroupFromUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @PathVariable(value = "groupIDs", required = true) List<String> groupIDs) {
    userService.deleteUserFromGroups(userId,groupIDs);
  }

  /*
  Applications related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/applications")
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
            value = "Results to retrieve"),
          @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
            value = "Index of first result to retrieve"),
          @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                  value = "Field to sort on"),
          @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
                  value = "Sorting order: ASC|DESC. Default order: DESC"),
          @ApiImplicitParam(name = "status", dataType = "string", paramType = "query",
                  value = "Filter by status. " +
                          "You could also specify filters on any field of the entity being queried as " +
                          "query parameters in this format: name=something")

  })
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of apps of user", response = PageDTO.class)
          }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Application> getUsersApplications(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(applicationService.findUserApps(userId, filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findUserApps(userId, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/applications")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Add Applications to user", response = User.class)
          }
  )
  public @ResponseBody
  User addAppsToUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestBody(required = true) List<String> appIDs) {
    return userService.addUserToApps(userId,appIDs);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/applications/{appIDs}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Delete Applications from User")
          }
  )
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteAppFromUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @PathVariable(value = "appIDs", required = true) List<String> appIDs) {
    userService.deleteUserFromApps(userId,appIDs);
  }

  @ExceptionHandler({ EntityNotFoundException.class })
  public ResponseEntity<Object> handleEntityNotFoundException(HttpServletRequest req, EntityNotFoundException ex) {
    log.error("User ID not found.");
    return new ResponseEntity<Object>("Invalid User ID provided.", new HttpHeaders(),
        HttpStatus.BAD_REQUEST);
  }
}
