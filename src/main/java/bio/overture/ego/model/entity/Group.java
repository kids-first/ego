/*
 * Copyright (c) 2018. The Ontario Institute for Cancer Research. All rights reserved.
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

package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.utils.Converters.nullToEmptyList;
import static bio.overture.ego.utils.Converters.nullToEmptySet;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = Tables.GROUP)
@JsonView(Views.REST.class)
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = {"users", "applications", "permissions"})
@JsonPropertyOrder({"id", "name", "description", "status", "applications", "groupPermissions"})
public class Group implements PolicyOwner {

  @JsonIgnore
  @JoinColumn(name = Fields.OWNER)
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  protected Set<Policy> policies;

  @JsonIgnore
  @JoinColumn(name = Fields.GROUPID_JOIN)
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  protected List<GroupPermission> permissions;

  @Id
  @GeneratedValue(generator = "group_uuid")
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(name = "group_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  UUID id;

  @NotNull
  @Column(name = Fields.NAME)
  String name;

  @Column(name = Fields.DESCRIPTION)
  String description;

  @NotNull
  @Column(name = Fields.STATUS)
  String status;

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = Tables.GROUP_APPLICATION,
      joinColumns = {@JoinColumn(name = Fields.GROUPID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.APPID_JOIN)})
  @JsonIgnore
  Set<Application> applications;

  @JsonIgnore
  @ManyToMany(
      mappedBy = Fields.GROUPS,
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  Set<User> users;

  @JsonIgnore
  public Set<User> getUsers(){
    users = nullToEmptySet(users);
    return users;
  }

  @JsonIgnore
  public Set<Application> getApplications(){
    applications = nullToEmptySet(applications);
    return applications;
  }

  @JsonIgnore
  public List<GroupPermission> getPermissions(){
    permissions = nullToEmptyList(permissions);
    return permissions;
  }

  @JsonIgnore
  public Set<Policy> getPolicies(){
    policies = nullToEmptySet(policies);
    return policies;
  }

}
