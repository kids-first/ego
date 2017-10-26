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

package org.overture.ego.repository.mapper;

import lombok.val;
import org.overture.ego.model.QueryInfo;
import org.overture.ego.model.entity.Application;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ApplicationMapper implements ResultSetMapper<Application> {
  @Override
  public Application map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    val app = Application.builder().id(Integer.parseInt(resultSet.getString("appId")))
        .name(resultSet.getString("appName"))
        .clientId(resultSet.getString("clientId"))
        .clientSecret(resultSet.getString("clientSecret"))
        .description(resultSet.getString("description"))
        .redirectUri(resultSet.getString("redirectUri"))
        .status(resultSet.getString("status")).build();
    app.setTotal(resultSet);
    return app;
  }

  // TODO: id is being converted to name and default sort is name - switch it to id after converting id to string
  public static String sanitizeSortField(String sort){
    if(sort.isEmpty()) return "appname";
    if("id".equals(sort.toLowerCase())) return "appname";
    if("name".equals(sort.toLowerCase())) return "appname";//TODO: change back to id after id is string
    // set default sort if sort order is not one of the field names
    if(isSortFieldValid(sort) == false){
      return "appname";
    } else return sort;
  }

  private static boolean isSortFieldValid(String sortField){
    sortField = sortField.toLowerCase();
    return ("appid".equals(sortField)        ||
            "appname".equals(sortField)      ||
            "clientid".equals(sortField)     ||
            "clientsecret".equals(sortField) ||
            "redirecturi".equals(sortField)  ||
            "description".equals(sortField)  ||
            "status".equals(sortField));
  }
}
