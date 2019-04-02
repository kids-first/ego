package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.repository.GroupPermissionRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupPermissionService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.web.StringResponseOption;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.model.enums.JavaFields.APPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.DESCRIPTION;
import static bio.overture.ego.model.enums.JavaFields.NAME;
import static bio.overture.ego.model.enums.JavaFields.PERMISSIONS;
import static bio.overture.ego.model.enums.JavaFields.STATUS;
import static bio.overture.ego.model.enums.JavaFields.USERGROUPS;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.DISABLED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.model.enums.StatusType.REJECTED;
import static bio.overture.ego.utils.CollectionUtils.concatToSet;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.EntityGenerator.randomEnum;
import static bio.overture.ego.utils.EntityGenerator.randomEnumExcluding;
import static bio.overture.ego.utils.EntityTools.extractAppIds;
import static bio.overture.ego.utils.EntityTools.extractGroupIds;
import static bio.overture.ego.utils.EntityTools.extractIDs;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Streams.stream;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupControllerTest extends AbstractControllerTest {

  private boolean hasRunEntitySetup = false;
  private static final boolean ENABLE_LOGGING = true;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private GroupService groupService;
  @Autowired private UserService userService;
  @Autowired private ApplicationService applicationService;
  @Autowired private GroupPermissionRepository groupPermissionRepository;
  @Autowired private GroupPermissionService groupPermissionService;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    // Initial setup of entities (run once
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      entityGenerator.setupTestApplications();
      entityGenerator.setupTestGroups();
      hasRunEntitySetup = true;
    }
  }

  @Test
  public void addGroup() {
    val groupRequest = GroupRequest.builder().name("Wizards").status(PENDING).description("").build();
    createGroupPostRequestAnd(groupRequest)
        .assertOk();
  }

  @Test
  public void addUniqueGroup() {
    val group = entityGenerator.setupGroup("SameSame");
    val groupRequest =
        GroupRequest.builder()
            .name(group.getName())
            .status(group.getStatus())
            .description(group.getDescription())
            .build();

    createGroupPostRequestAnd(groupRequest)
        .assertConflict();
  }

  @Test
  public void getGroup() {
    // Groups created in setup
    val group = groupService.getByName("Group One");
    val groupId = group.getId();

    val actualBody = getGroupEntityGetRequestAnd(group)
        .assertOk()
        .assertHasBody()
        .getResponse()
        .getBody();

    val expectedBody =
        format(
            "{\"id\":\"%s\",\"name\":\"Group One\",\"description\":\"\",\"status\":\"PENDING\"}",
            groupId);

    assertThat(actualBody).isEqualTo(expectedBody);
  }

  @Test
  public void getGroupNotFound() {
    val response = initStringRequest()
        .endpoint("/groups/%s", UUID.randomUUID())
        .getAnd()
        .assertNotFound();
  }

  @Test
  public void listGroups() {

    val totalGroups = groupService.getRepository().count();

    // Get all groups

    val actualBody = listGroupsEndpointAnd()
        .queryParam("offset", 0)
        .queryParam("limit", totalGroups)
        .getAnd()
        .assertOk()
        .assertHasBody()
        .getResponse()
        .getBody();


    val expectedBody =
        format(
            "[{\"id\":\"%s\",\"name\":\"Group One\",\"description\":\"\",\"status\":\"PENDING\"}, {\"id\":\"%s\",\"name\":\"Group Two\",\"description\":\"\",\"status\":\"PENDING\"}, {\"id\":\"%s\",\"name\":\"Group Three\",\"description\":\"\",\"status\":\"PENDING\"}]",
            groupService.getByName("Group One").getId(),
            groupService.getByName("Group Two").getId(),
            groupService.getByName("Group Three").getId());

    assertThatJson(actualBody)
        .when(IGNORING_EXTRA_ARRAY_ITEMS, IGNORING_ARRAY_ORDER)
        .node("resultSet")
        .isEqualTo(expectedBody);
  }

  @Test
  public void deleteOne() {
    val group = entityGenerator.setupGroup("DeleteOne");
    val groupId = group.getId();

    // Users for test
    val userOne = entityGenerator.setupUser("TempGroup User");
    val userId = userOne.getId();

    // Application for test
    val appOne = entityGenerator.setupApplication("TempGroupApp");
    val appId = appOne.getId();

    // REST to get users/app in group
    val usersBody = singletonList(userOne);
    val appsBody = singletonList(appOne);

    addUsersToGroupPostRequestAnd(group, usersBody).assertOk();
    addApplicationsToGroupPostRequestAnd(group, appsBody).assertOk();

    // Check user-group relationship is there
    val userWithGroup = userService.getByName("TempGroupUser@domain.com");
    val expectedGroups = mapToSet(userWithGroup.getUserGroups(), UserGroup::getGroup);
    assertThat(extractGroupIds(expectedGroups)).contains(groupId);

    // Check app-group relationship is there
    val applicationWithGroup = applicationService.getByClientId("TempGroupApp");
    assertThat(extractGroupIds(applicationWithGroup.getGroups())).contains(groupId);

    deleteGroupDeleteRequestAnd(group).assertOk();

    // Check user-group relationship is also deleted
    val usersWithoutGroupBody = getGroupsForUserGetRequestAnd(userOne)
        .assertOk()
        .assertHasBody()
        .getResponse()
        .getBody();
    assertThat(usersWithoutGroupBody).doesNotContain(groupId.toString());

    // Check user-group relationship is also deleted
    val applicationWithoutGroupBody = getGroupsForApplicationGetRequestAnd(appOne)
        .assertOk()
        .assertHasBody()
        .getResponse()
        .getBody();
    assertThat(applicationWithoutGroupBody).doesNotContain(groupId.toString());

    // Check group is deleted
    getGroupEntityGetRequestAnd(group).assertNotFound();
  }

  // TODO: [rtisma] will eventually be fixed when properly using query by Specification, which will
  // allow for runtime base queries. This will allow us to define fetch strategy at run time
  @Test
  public void addUsersToGroup() {

    val group = entityGenerator.setupGroup("GroupWithUsers");

    val userOne = userService.getByName("FirstUser@domain.com");
    val userTwo = userService.getByName("SecondUser@domain.com");

    val body = asList(userOne, userTwo);
    addUsersToGroupPostRequestAnd(group, body).assertOk();

    // Check that Group is associated with Users
    val groupWithUsers = groupService.getByName("GroupWithUsers");
    assertThat(extractIDs(mapToSet(groupWithUsers.getUserGroups(), UserGroup::getUser)))
        .contains(userOne.getId(), userTwo.getId());

    // Check that each user is associated with the group
    val userOneWithGroups = userService.getByName("FirstUser@domain.com");
    val userTwoWithGroups = userService.getByName("SecondUser@domain.com");

    assertThat(mapToSet(userOneWithGroups.getUserGroups(), UserGroup::getGroup)).contains(group);
    assertThat(mapToSet(userTwoWithGroups.getUserGroups(), UserGroup::getGroup)).contains(group);
  }

  @Test
  @SneakyThrows
  public void deleteUserFromGroup() {
    val group = entityGenerator.setupGroup("RemoveGroupUsers");
    val groupId = group.getId();
    val deleteUser = entityGenerator.setupUser("Delete This");
    val remainUser = entityGenerator.setupUser("Keep This");

    val body = asList(deleteUser, remainUser);

    addUsersToGroupPostRequestAnd(group, body  ).assertOk();

    getUsersForGroupGetRequestAnd(group)
        .assertPageResultsOfType(User.class)
        .hasSize(2);

    deleteUsersFromGroupDeleteRequestAnd(group, asList(deleteUser)).assertOk();

    val actualUsersForGroup = getUsersForGroupGetRequestAnd(group)
        .extractPageResults(User.class);
    assertThat(actualUsersForGroup).hasSize(1);
    assertThat(actualUsersForGroup.stream().findAny().get().getId()).isEqualTo(remainUser.getId());
  }

  @Test
  public void addAppsToGroup() {

    val group = entityGenerator.setupGroup("GroupWithApps");

    val appOne = applicationService.getByClientId("111111");
    val appTwo = applicationService.getByClientId("222222");

    val body = asList(appOne, appTwo);
    addApplicationsToGroupPostRequestAnd(group, body).assertOk();

    // Check that Group is associated with Users
    val groupWithApps = groupService.getByName("GroupWithApps");
    assertThat(extractAppIds(groupWithApps.getApplications()))
        .contains(appOne.getId(), appTwo.getId());

    // Check that each user is associated with the group
    val appOneWithGroups = applicationService.getByClientId("111111");
    val appTwoWithGroups = applicationService.getByClientId("222222");

    assertThat(appOneWithGroups.getGroups()).contains(group);
    assertThat(appTwoWithGroups.getGroups()).contains(group);
  }

  @Test
  @SneakyThrows
  public void deleteAppFromGroup() {
    val group = entityGenerator.setupGroup("RemoveGroupApps");
    val groupId = group.getId();
    val deleteApp = entityGenerator.setupApplication("DeleteThis");
    val remainApp = entityGenerator.setupApplication("KeepThis");

    val body = asList(deleteApp, remainApp);
    addApplicationsToGroupPostRequestAnd(group, body).assertOk();

    getApplicationsForGroupGetRequestAnd(group)
        .assertPageResultsOfType(Application.class)
        .hasSize(2);

    deleteApplicationsFromGroupDeleteRequestAnd(group, deleteApp).assertOk();

    val actualApps = getApplicationsForGroupGetRequestAnd(group)
        .extractPageResults(Application.class);
    assertThat(actualApps).hasSize(1);
    assertThat(actualApps.stream().findAny().get().getId()).isEqualTo(remainApp.getId());
  }

  @Test
  public void createGroup_NonExisting_Success() {
    val r = GroupRequest.builder()
        .name(generateNonExistentName(groupService))
        .status(APPROVED)
        .build();

    val group1 = createGroupPostRequestAnd(r)
        .extractOneEntity(Group.class);

    getGroupEntityGetRequestAnd(group1)
        .assertOk()
        .assertHasBody();

    assertThat(r).isEqualToComparingFieldByField(group1);
  }

  @Test
  public void createGroup_NameAlreadyExists_Conflict() {
    val existingGroup = entityGenerator.generateRandomGroup();
    val createRequest =
        GroupRequest.builder().name(existingGroup.getName()).status(APPROVED).build();

    createGroupPostRequestAnd(createRequest).assertConflict();
  }

  @Test
  public void deleteGroup_NonExisting_Conflict() {
    val nonExistingId = generateNonExistentId(groupService);
    deleteGroupDeleteRequestAnd(nonExistingId).assertNotFound();
  }

  @Test
  public void deleteGroupAndRelationshipsOnly_AlreadyExisting_Success() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Add Applications to Group0
    addApplicationsToGroupPostRequestAnd(group0, data.getApplications()).assertOk();

    // Assert the applications were add to Group0
    getApplicationsForGroupGetRequestAnd(group0)
        .assertPageResultsOfType(Application.class)
        .hasSize(data.getApplications().size());

    // Add Users to Group0
    addUsersToGroupPostRequestAnd(group0, data.getUsers()).assertOk();

    // Assert the users were added to Group0
    getUsersForGroupGetRequestAnd(group0)
        .assertPageResultsOfType(User.class)
        .hasSize(data.getUsers().size());

    // Add Permissions to Group0
    addGroupPermissionToGroupPostRequestAnd(group0, data.getPolicies().get(0), DENY).assertOk();

    addGroupPermissionToGroupPostRequestAnd(group0, data.getPolicies().get(1), WRITE).assertOk();

    // Assert the permissions were added to Group0
    getGroupPermissionsForGroupGetRequestAnd(group0)
        .assertPageResultsOfType(GroupPermission.class)
        .hasSize(2);

    // Delete the group
    deleteGroupDeleteRequestAnd(group0).assertOk();

    // Assert the group was deleted
    getGroupEntityGetRequestAnd(group0).assertNotFound();

    // Assert no group permissions for the group
    val results = groupPermissionRepository.findAllByOwner_Id(group0.getId());
    assertThat(results).hasSize(0);

    // Assert getGroupUsers returns NOT_FOUND
    getUsersForGroupGetRequestAnd(group0).assertNotFound();

    // Assert getGroupApplications returns NotFound
    getApplicationsForGroupGetRequestAnd(group0).assertNotFound();

    // Assert all users still exist
    data.getUsers()
        .forEach( u -> getUserEntityGetRequestAnd(u).assertOk());

    // Assert all applications still exist
    data.getApplications()
        .forEach( a -> getApplicationGetRequestAnd(a).assertOk() );

    // Assert all policies still exist
    data.getPolicies()
        .forEach( p -> getPolicyGetRequestAnd(p).assertOk() );
  }

  @Test
  public void getGroups_FindAllQuery_Success() {
    val expectedGroups = repeatedCallsOf(() -> entityGenerator.generateRandomGroup(), 4);
    val numGroups = groupService.getRepository().count();
    val actualGroups =
        initStringRequest()
            .endpoint("/groups")
            .queryParam("limit", numGroups)
            .queryParam("offset", 0)
            .getAnd()
            .assertOk()
            .assertHasBody()
            .map(x -> StringResponseOption.extractPageResultSetFromResponse(x, Group.class));
    assertThat(actualGroups).containsAll(expectedGroups);
  }

  @Test
  public void getGroups_FindSomeQuery_Success() {
    val g1 =
        StringResponseOption.extractOneEntityFromResponse(
            createGroupPostRequest(
                GroupRequest.builder()
                    .name("abc11")
                    .status(APPROVED)
                    .description("blueberry banana")
                    .build()),
            Group.class);

    val g2 =
        StringResponseOption.extractOneEntityFromResponse(
            createGroupPostRequest(
                GroupRequest.builder()
                    .name("abc21")
                    .status(APPROVED)
                    .description("blueberry orange")
                    .build()),
            Group.class);

    val g3 =
        StringResponseOption.extractOneEntityFromResponse(
            createGroupPostRequest(
                GroupRequest.builder()
                    .name("abc22")
                    .status(REJECTED)
                    .description("orange banana")
                    .build()),
            Group.class);

    val numGroups = groupPermissionRepository.count();

    val r1 =
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "abc")
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .logging()
            .getAnd()
            .assertOk()
            .assertHasBody()
            .map(x -> StringResponseOption.extractPageResultSetFromResponse(x, Group.class));
    assertThat(r1).containsExactlyInAnyOrder(g1, g2, g3);

    val r2 =
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "abc2")
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .getAnd()
            .assertOk()
            .assertHasBody()
            .map(x -> StringResponseOption.extractPageResultSetFromResponse(x, Group.class));
    assertThat(r2).containsExactlyInAnyOrder(g3, g2);

    val r3 =
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "abc")
            .queryParam("status", REJECTED)
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .getAnd()
            .assertOk()
            .assertHasBody()
            .map(x -> StringResponseOption.extractPageResultSetFromResponse(x, Group.class));
    val rejectedGroups =
        r3.stream().filter(x -> x.getStatus() == REJECTED).collect(toImmutableSet());
    assertThat(rejectedGroups.size()).isGreaterThanOrEqualTo(1);

    val r4 =
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "blueberry")
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .getAnd()
            .assertOk()
            .assertHasBody()
            .map(x -> StringResponseOption.extractPageResultSetFromResponse(x, Group.class));
    assertThat(r4).contains(g1, g2);

    val r5 =
        StringResponseOption.extractPageResultSetFromResponse(
            initStringRequest()
                .endpoint("/groups")
                .queryParam("query", "orange")
                .queryParam("offset", 0)
                .queryParam("length", numGroups)
                .get(),
            Group.class);
    assertThat(r5).contains(g3, g2);

    val r6 =
        StringResponseOption.extractPageResultSetFromResponse(
            initStringRequest()
                .endpoint("/groups")
                .queryParam("query", "orange")
                .queryParam("status", REJECTED)
                .queryParam("offset", 0)
                .queryParam("length", numGroups)
                .get(),
            Group.class);
    assertThat(r6).contains(g3);

    val r7 =
        StringResponseOption.extractPageResultSetFromResponse(
            initStringRequest()
                .endpoint("/groups")
                .queryParam("query", "blue")
                .queryParam("offset", 0)
                .queryParam("length", numGroups)
                .get(),
            Group.class);
    assertThat(r7).contains(g1);
  }

  @Test
  public void addUsersToGroup_NonExistentGroup_NotFound() {
    val data = generateUniqueTestGroupData();
    val nonExistentId = generateNonExistentId(groupService);
    val nonExistentGroup = Group.builder().id(nonExistentId).build();
    val r1 = addUsersToGroupPostRequest(nonExistentGroup, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void addUsersToGroup_AllExistingUnassociatedUsers_Success() {
    // Generate test data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getUsersForGroupGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = StringResponseOption.extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add the users to the group
    val r1 = addUsersToGroupPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert the users were added
    val r2 = getUsersForGroupGetRequest(group0);
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    val actualUsersAfter = StringResponseOption.extractPageResultSetFromResponse(r2, User.class);
    assertThat(actualUsersAfter).containsExactlyInAnyOrderElementsOf(data.getUsers());
  }

  @Test
  public void addUsersToGroup_SomeExistingUsersButAllUnassociated_NotFound() {
    // Setup data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val existingUserIds = convertToIds(data.getUsers());
    val someNonExistingUserIds = repeatedCallsOf(() -> generateNonExistentId(userService), 3);
    val mergedUserIds = concatToSet(existingUserIds, someNonExistingUserIds);

    // Attempt to add nonexistent users to the group
    val r1 =
        initStringRequest().endpoint("/groups/%s/users", group0.getId()).body(mergedUserIds).post();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void addUsersToGroup_AllExsitingUsersButSomeAlreadyAssociated_Conflict() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getUsersForGroupGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = StringResponseOption.extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add some new unassociated users
    val someUsers = newArrayList(data.getUsers().get(0));
    val r1 = addUsersToGroupPostRequest(group0, someUsers);
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert that adding already associated users returns a conflict
    val r2 = addUsersToGroupPostRequest(group0, data.getUsers());
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
  }

  @Test
  public void removeUsersFromGroup_AllExistingAssociatedUsers_Success() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getUsersForGroupGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = StringResponseOption.extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add users to group
    val r1 = addUsersToGroupPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Delete all users
    val r2 = deleteUsersFromGroupDeleteRequest(group0, data.getUsers());
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    // Assert there are no users for the group
    val r3 = getUsersForGroupGetRequest(group0);
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    val actualUsersAfter = StringResponseOption.extractPageResultSetFromResponse(r3, User.class);
    assertThat(actualUsersAfter).isEmpty();
  }

  @Test
  public void removeUsersFromGroup_AllExistingUsersButSomeNotAssociated_NotFound() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getUsersForGroupGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = StringResponseOption.extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add some users to group
    val r1 = addUsersToGroupPostRequest(group0, newArrayList(data.getUsers().get(0)));
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Delete all users
    val r2 = deleteUsersFromGroupDeleteRequest(group0, data.getUsers());
    assertThat(r2.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void removeUsersFromGroup_SomeNonExistingUsersButAllAssociated_NotFound() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getUsersForGroupGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = StringResponseOption.extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add all users to group
    val r1 = addUsersToGroupPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Create list of userIds to delete, including one non existent id
    val userIdsToDelete = data.getUsers().stream().map(Identifiable::getId).collect(toList());
    userIdsToDelete.add(generateNonExistentId(userService));

    // Delete existing associated users and non-existing users, and assert a not found error
    val r2 =
        initStringRequest()
            .endpoint("groups/%s/users/%s", group0.getId(), COMMA.join(userIdsToDelete))
            .delete();
    assertThat(r2.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void removeUsersFromGroup_NonExistentGroup_NotFound() {
    val data = generateUniqueTestGroupData();
    val existingUserIds = convertToIds(data.getUsers());
    val nonExistentId = generateNonExistentId(groupService);

    val r1 =
        initStringRequest()
            .endpoint("groups/%s/users/%s", nonExistentId, COMMA.join(existingUserIds))
            .delete();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void getUsersFromGroup_FindAllQuery_Success() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert without using a controller, there are no users for the group
    val beforeGroup = groupService.getWithRelationships(group0.getId());
    assertThat(beforeGroup.getUserGroups()).isEmpty();

    // Add users to group
    val r1 = addUsersToGroupPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert without using a controller, there are users for the group
    val afterGroup = groupService.getWithRelationships(group0.getId());
    val expectedUsers = mapToSet(afterGroup.getUserGroups(), UserGroup::getUser);
    assertThat(expectedUsers).containsExactlyInAnyOrderElementsOf(data.getUsers());

    // Get user for a group using a controller
    val r2 = initStringRequest().endpoint("groups/%s/users", group0.getId()).get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    val actualUsers = StringResponseOption.extractPageResultSetFromResponse(r2, User.class);
    assertThat(actualUsers).containsExactlyInAnyOrderElementsOf(data.getUsers());
  }

  @Test
  public void getUsersFromGroup_NonExistentGroup_NotFound() {
    val nonExistentId = generateNonExistentId(groupService);
    val r1 = initStringRequest().endpoint("groups/%s/users", nonExistentId).get();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void getUsersFromGroup_FindSomeQuery_Success() {

    // Create users and groups
    val g1 = entityGenerator.generateRandomGroup();
    val u1 = entityGenerator.setupUser("blueberry banana");
    val u2 = entityGenerator.setupUser("blueberry orange");
    val u3 = entityGenerator.setupUser("banana orange");

    // Update their status
    u1.setStatus(APPROVED);
    u2.setStatus(APPROVED);
    u3.setStatus(DISABLED);

    // Add users to group
    val r1 = addUsersToGroupPostRequest(g1, newArrayList(u1, u2, u3));
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    val numGroups = groupRepository.count();

    // Search users
    val actualUsers2 = initStringRequest()
        .endpoint("groups/%s/users", g1.getId())
        .logging()
        .queryParam("query", "orange")
        .queryParam("status", DISABLED)
        .queryParam("offset", 0)
        .queryParam("length", numGroups)
        .getAnd()
        .assertPageResultsOfType(User.class)
        .contains(u3);

    val r3 =
        initStringRequest()
            .endpoint("groups/%s/users", g1.getId())
            .queryParam("query", "orange")
            .queryParam("status", APPROVED)
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .get();
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    val actualUsers3 = StringResponseOption.extractPageResultSetFromResponse(r3, User.class);
    assertThat(actualUsers3).contains(u2);

    val r4 =
        initStringRequest()
            .endpoint("groups/%s/users", g1.getId())
            .queryParam("status", APPROVED)
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    val actualUsers4 = StringResponseOption.extractPageResultSetFromResponse(r4, User.class);
    assertThat(actualUsers4).contains(u1, u2);

    val r5 =
        initStringRequest()
            .endpoint("groups/%s/users", g1.getId())
            .queryParam("query", "blueberry")
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .get();
    assertThat(r5.getStatusCode()).isEqualTo(OK);
    val actualUsers5 = StringResponseOption.extractPageResultSetFromResponse(r5, User.class);
    assertThat(actualUsers5).contains(u1, u2);

    val r6 =
        initStringRequest()
            .endpoint("groups/%s/users", g1.getId())
            .queryParam("query", "banana")
            .queryParam("offset", 0)
            .queryParam("length", numGroups)
            .get();
    assertThat(r6.getStatusCode()).isEqualTo(OK);
    val actualUsers6 = StringResponseOption.extractPageResultSetFromResponse(r6, User.class);
    assertThat(actualUsers6).contains(u1, u3);
  }

  @Test
  public void getGroup_ExistingGroup_Success() {
    val group = entityGenerator.generateRandomGroup();
    assertThat(groupService.isExist(group.getId())).isTrue();
    val r1 = getGroupEntityGetRequest(group);
    assertThat(r1.getStatusCode()).isEqualTo(OK);
  }

  @Test
  public void getGroup_NonExistentGroup_Success() {
    val nonExistentId = generateNonExistentId(groupService);
    val r1 = initStringRequest().endpoint("groups/%s", nonExistentId).get();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void UUIDValidation_MalformedUUID_Conflict() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val badUUID = "123sksk";

    initStringRequest().endpoint("/groups/%s", badUUID).deleteAnd().assertBadRequest();

    initStringRequest().endpoint("/groups/%s", badUUID).getAnd().assertBadRequest();

    initStringRequest().endpoint("/groups/%s/applications", badUUID).getAnd().assertBadRequest();

    initStringRequest().endpoint("/groups/%s/applications", badUUID).postAnd().assertBadRequest();

    val appIds = mapToList(data.getApplications(), x -> x.getId().toString());
    appIds.add(badUUID);

    // Test when an id in the payload is not a uuid
    initStringRequest()
        .endpoint("/groups/%s/applications", group0.getId())
        .body(appIds)
        .postAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/applications/%s", badUUID, data.getApplications().get(0).getId())
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/applications/%s", group0.getId(), COMMA.join(appIds))
        .deleteAnd()
        .assertBadRequest();

    initStringRequest().endpoint("groups/%s/permissions", badUUID).getAnd().assertBadRequest();

    initStringRequest().endpoint("groups/%s/permissions", badUUID).postAnd().assertBadRequest();

    // Test when an id in the payload is not a uuid
    val body =
        MAPPER
            .createArrayNode()
            .add(
                MAPPER
                    .createObjectNode()
                    .put("mask", READ.toString())
                    .put("policyId", data.getPolicies().get(0).getId().toString()))
            .add(MAPPER.createObjectNode().put("mask", READ.toString()).put("policyId", badUUID));
    initStringRequest()
        .endpoint("groups/%s/permissions", group0.getId())
        .body(body)
        .postAnd()
        .assertBadRequest();

    val r2 = addGroupPermissionToGroupPostRequest(group0, data.getPolicies().get(0), READ);
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    val r3 = getGroupPermissionsForGroupGetRequest(group0);
    val actualPermissions = StringResponseOption.extractPageResultSetFromResponse(r3, GroupPermission.class);
    assertThat(actualPermissions).hasSize(1);
    val existingPermissionId = actualPermissions.get(0).getId();

    initStringRequest()
        .endpoint("groups/%s/permissions/%s", badUUID, existingPermissionId)
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("groups/%s/permissions/%s", group0.getId(), badUUID + "," + existingPermissionId)
        .deleteAnd()
        .assertBadRequest();

    initStringRequest().endpoint("/groups/%s/users", badUUID).getAnd().assertBadRequest();

    initStringRequest().endpoint("/groups/%s/users", badUUID).postAnd().assertBadRequest();

    val userIds = mapToList(data.getUsers(), x -> x.getId().toString());
    userIds.add(badUUID);

    // Test when an id in the payload is not a uuid
    initStringRequest()
        .endpoint("/groups/%s/users", group0.getId())
        .body(userIds)
        .postAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/users/%s", badUUID, data.getUsers().get(0).getId())
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/users/%s", group0.getId(), COMMA.join(userIds))
        .deleteAnd()
        .assertBadRequest();
  }

  @Test
  public void updateGroup_ExistingGroup_Success() {
    val g = entityGenerator.generateRandomGroup();

    val updateRequest1 =
        GroupRequest.builder()
            .name(generateNonExistentName(groupService))
            .status(null)
            .description(null)
            .build();

    val updatedGroup1 =
        StringResponseOption.extractOneEntityFromResponse(
            initStringRequest()
                .endpoint("/groups/%s", g.getId())
                .body(updateRequest1)
                .putAnd()
                .assertOk()
                .assertHasBody()
                .getResponse(),
            Group.class);
    assertThat(updatedGroup1)
        .isEqualToIgnoringGivenFields(g, NAME, PERMISSIONS, APPLICATIONS, USERGROUPS);
    assertThat(updatedGroup1.getName()).isEqualTo(updateRequest1.getName());

    val updateRequest2 =
        GroupRequest.builder()
            .name(null)
            .status(randomEnumExcluding(StatusType.class, g.getStatus()))
            .description(null)
            .build();
    val updatedGroup2 =
        StringResponseOption.extractOneEntityFromResponse(
            initStringRequest()
                .endpoint("/groups/%s", g.getId())
                .body(updateRequest2)
                .putAnd()
                .assertOk()
                .assertHasBody()
                .getResponse(),
            Group.class);
    assertThat(updatedGroup2)
        .isEqualToIgnoringGivenFields(updatedGroup1, STATUS, PERMISSIONS, APPLICATIONS, USERGROUPS);
    assertThat(updatedGroup2.getStatus()).isEqualTo(updateRequest2.getStatus());

    val description = "my description";
    val updateRequest3 =
        GroupRequest.builder().name(null).status(null).description(description).build();
    val updatedGroup3 =
        StringResponseOption.extractOneEntityFromResponse(
            initStringRequest()
                .endpoint("/groups/%s", g.getId())
                .body(updateRequest3)
                .putAnd()
                .assertOk()
                .assertHasBody()
                .getResponse(),
            Group.class);
    assertThat(updatedGroup3)
        .isEqualToIgnoringGivenFields(
            updatedGroup2, DESCRIPTION, PERMISSIONS, APPLICATIONS, USERGROUPS);
    assertThat(updatedGroup3.getDescription()).isEqualTo(updateRequest3.getDescription());
  }

  @Test
  public void updateGroup_NonExistentGroup_NotFound() {
    val nonExistentId = generateNonExistentId(groupService);
    val updateRequest = GroupRequest.builder().build();
    initStringRequest()
        .endpoint("/groups/%s", nonExistentId)
        .body(updateRequest)
        .putAnd()
        .assertNotFound();
  }

  @Test
  public void updateGroup_NameAlreadyExists_Conflict() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val group1 = data.getGroups().get(1);
    val updateRequest = GroupRequest.builder().name(group1.getName()).build();
    initStringRequest()
        .endpoint("groups/%s", group0.getId())
        .body(updateRequest)
        .logging()
        .putAnd()
        .assertConflict();
  }

  @Test
  public void statusValidation_MalformedStatus_Conflict() {
    val invalidStatus = "something123";
    val match = stream(StatusType.values()).anyMatch(x -> x.toString().equals(invalidStatus));
    assertThat(match).isFalse();

    val data = generateUniqueTestGroupData();
    val group = data.getGroups().get(0);

    // createGroup: POST /groups
    val createRequest =
        MAPPER
            .createObjectNode()
            .put("name", generateNonExistentName(groupService))
            .put("status", invalidStatus);
    initStringRequest()
        .logging()
        .endpoint("/groups")
        .body(createRequest)
        .postAnd()
        .assertBadRequest();

    // updateGroup:  PUT /groups
    val updateRequest = MAPPER.createObjectNode().put("status", invalidStatus);
    initStringRequest()
        .logging()
        .endpoint("/groups/%s", group.getId())
        .body(updateRequest)
        .putAnd()
        .assertBadRequest();

    // getGroupsApplications:  GET /groups/{groupId}/applications
    //    initStringRequest()
    //        .logging()
    //        .endpoint("/groups/%s/applications", group.getId())
    //        .queryParam("status", invalidStatus)
    //        .getAnd()
    //        .assertBadRequest();

    // getScopes:  GET /groups/{groupId}/permissions
    //    initStringRequest()
    //        .logging()
    //        .endpoint("/groups/%s/permissions", group.getId())
    //        .queryParam("status", invalidStatus)
    //        .getAnd()
    //        .assertBadRequest();

    // getGroupsUsers:  GET /groups/{groupId}/users
    //    initStringRequest()
    //        .logging()
    //        .endpoint("/groups/%s/users", group.getId())
    //        .queryParam("status", invalidStatus)
    //        .getAnd()
    //        .assertBadRequest();
  }

  @Test
  public void getScopes_FindAllQuery_Success() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert without using a controller, there are no users for the group
    val beforeGroup = groupService.getWithRelationships(group0.getId());
    assertThat(beforeGroup.getPermissions()).isEmpty();

    // Add policies to group
    data.getPolicies()
        .forEach(
            p -> {
              val randomMask = randomEnum(AccessLevel.class);
              val r1 = addGroupPermissionToGroupPostRequest(group0, p, randomMask);
              assertThat(r1.getStatusCode()).isEqualTo(OK);
            });

    // Assert without using a controller, there are users for the group
    val afterGroup = groupService.getWithRelationships(group0.getId());
    assertThat(afterGroup.getPermissions()).hasSize(2);

    // Get permissions for a group using a controller
    val r2 =
        initStringRequest()
            .endpoint("/groups/%s/permissions", group0.getId())
            .getAnd()
            .assertOk()
            .getResponse();
    val actualPermissions = StringResponseOption.extractPageResultSetFromResponse(r2, GroupPermission.class);
    assertThat(actualPermissions).containsExactlyInAnyOrderElementsOf(afterGroup.getPermissions());
  }

  @Test
  @Ignore
  public void getScopes_FindSomeQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'getScopes_FindSomeQuery_Success'");
  }

  @Test
  public void addAppsToGroup_NonExistentGroup_NotFound() {
    val data = generateUniqueTestGroupData();
    val nonExistentId = generateNonExistentId(groupService);
    val appIds = convertToIds(data.getApplications());

    initStringRequest()
        .endpoint("/groups/%s/applications", nonExistentId)
        .body(appIds)
        .postAnd()
        .assertNotFound();
  }

  @Test
  public void addAppsToGroup_AllExistingUnassociatedApps_Success() {
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val appIds = convertToIds(data.getApplications());

    // Assert without using the controller, that the group is not associated with any apps
    val beforeGroup = groupService.getWithRelationships(group0.getId());
    assertThat(beforeGroup.getApplications()).isEmpty();

    // Add applications to the group
    initStringRequest()
        .endpoint("/groups/%s/applications", group0.getId())
        .body(appIds)
        .postAnd()
        .assertOk();

    // Assert without usign the controller, that the group IS associated with all the apps
    val afterGroup = groupService.getWithRelationships(group0.getId());
    assertThat(afterGroup.getApplications())
        .containsExactlyInAnyOrderElementsOf(data.getApplications());
  }

  @Test
  public void addAppsToGroup_SomeExistingAppsButAllUnassociated_NotFound() {

    // Setup data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val existingAppIds = convertToIds(data.getApplications());
    val nonExistingAppIds = repeatedCallsOf(() -> generateNonExistentId(applicationService), 3);
    val appIdsToAssociate = concatToSet(existingAppIds, nonExistingAppIds);

    // Add some existing and non-existing app ids to an existing group
    initStringRequest()
        .endpoint("/groups/%s/applications", group0.getId())
        .body(appIdsToAssociate)
        .postAnd()
        .assertNotFound();
  }

  @Test
  public void removeAppsFromGroup_AllExistingAssociatedApps_Success() {
    // Setup data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Add all apps to the groupo
    initStringRequest()
        .endpoint("/groups/%s/applications", group0.getId())
        .body(convertToIds(data.getApplications()))
        .postAnd()
        .assertOk();

    // Assert the group has all the apps
    val actualApplications =
        initStringRequest()
            .endpoint("/groups/%s/applications", group0.getId())
            .getAnd()
            .assertOk()
            .assertHasBody()
            .map(response -> StringResponseOption.extractPageResultSetFromResponse(response, Application.class));
    assertThat(actualApplications).containsExactlyInAnyOrderElementsOf(data.getApplications());

    // Remove all apps
    val appIdsToRemove = convertToIds(data.getApplications());
    initStringRequest()
        .endpoint("/groups/%s/applications/%s", group0.getId(), COMMA.join(appIdsToRemove))
        .deleteAnd()
        .assertOk();

    // Assert the group has 0 apps
    val actualApplications2 =
        initStringRequest()
            .endpoint("/groups/%s/applications", group0.getId())
            .getAnd()
            .assertOk()
            .assertHasBody()
            .map(response -> StringResponseOption.extractPageResultSetFromResponse(response, Application.class));
    assertThat(actualApplications2).isEmpty();
  }

  @Test
  public void removeAppsFromGroup_AllExistingAppsButSomeNotAssociated_NotFound() {
    // Setup data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val app0Id = data.getApplications().get(0).getId();
    val app1Id = data.getApplications().get(1).getId();

    // Add app0 to the group0
    initStringRequest()
        .endpoint("/groups/%s/applications", group0.getId())
        .body(newArrayList(app0Id))
        .postAnd()
        .assertOk();

    // Remove associated and non-associated apps from the group, however all are existing
    val appIdsToRemove = newArrayList(app0Id, app1Id);
    initStringRequest()
        .endpoint("/groups/%s/applications/%s", group0.getId(), COMMA.join(appIdsToRemove))
        .deleteAnd()
        .assertNotFound();
  }

  @Test
  public void removeAppsFromGroup_SomeNonExistingAppsButAllAssociated_NotFound() {

    // Setup data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val existingAppIds = convertToIds(data.getApplications());
    val nonExistingAppIds = repeatedCallsOf(() -> generateNonExistentId(applicationService), 3);
    val appIdsToDisassociate = concatToSet(existingAppIds, nonExistingAppIds);

    // Add all existing apps to group
    initStringRequest()
        .endpoint("/groups/%s/applications", group0.getId())
        .body(existingAppIds)
        .postAnd()
        .assertOk();

    // Attempt to disassociate existing associated apps and non-exsiting apps from the group, and
    // fail
    initStringRequest()
        .endpoint("/groups/%s/applications/%s", group0.getId(), COMMA.join(appIdsToDisassociate))
        .deleteAnd()
        .assertNotFound();
  }

  @Test
  public void removeAppsFromGroup_NonExistentGroup_NotFound() {
    val nonExistentId = generateNonExistentId(groupService);
    val data = generateUniqueTestGroupData();
    val existingApplicationIds = convertToIds(data.getApplications());

    // Assert that the group does not exist
    initStringRequest()
        .endpoint("/groups/%s/applications/%s", nonExistentId, COMMA.join(existingApplicationIds))
        .deleteAnd()
        .assertNotFound();
  }

  @Test
  @Ignore
  public void getAppsFromGroup_FindAllQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'getAppsFromGroup_FindAllQuery_Success'");
  }

  @Test
  public void getAppsFromGroup_NonExistentGroup_NotFound() {
    val nonExistentId = generateNonExistentId(groupService);

    // Attempt to get applications for non existent group, and fail
    initStringRequest()
        .endpoint("/groups/%s/applications", nonExistentId)
        .getAnd()
        .assertNotFound();
  }

  @Test
  @Ignore
  public void getAppsFromGroup_FindSomeQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'getAppsFromGroup_FindSomeQuery_Success'");
  }

  @SneakyThrows
  private TestGroupData generateUniqueTestGroupData() {
    val groups = repeatedCallsOf(() -> entityGenerator.generateRandomGroup(), 2);
    val applications = repeatedCallsOf(() -> entityGenerator.generateRandomApplication(), 2);
    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), 2);
    val policies = repeatedCallsOf(() -> entityGenerator.generateRandomPolicy(), 2);

    return TestGroupData.builder()
        .groups(groups)
        .applications(applications)
        .users(users)
        .policies(policies)
        .build();
  }

  private ResponseEntity<String> addApplicationsToGroupPostRequest(
      Group g, Collection<Application> applications) {
    return addApplicationsToGroupPostRequestAnd(g, applications).getResponse();
  }

  private ResponseEntity<String> addUsersToGroupPostRequest(Group g, Collection<User> users) {
    return addUsersToGroupPostRequestAnd(g, users).getResponse();
  }

  private ResponseEntity<String> getUsersForGroupGetRequest(Group g) {
    return getUsersForGroupGetRequestAnd(g).getResponse();
  }


  private ResponseEntity<String> getApplicationsForGroupGetRequest(Group g) {
    return getApplicationsForGroupGetRequestAnd(g).getResponse();
  }

  private ResponseEntity<String> getGroupPermissionsForGroupGetRequest(Group g) {
    return getGroupPermissionsForGroupGetRequestAnd(g).getResponse();
  }

  private ResponseEntity<String> deleteUsersFromGroupDeleteRequest(
      Group g, Collection<User> users) {
    return deleteUsersFromGroupDeleteRequestAnd(g, users).getResponse();
  }

  private ResponseEntity<String> deleteGroupDeleteRequest(Group g) {
    return deleteGroupDeleteRequestAnd(g).getResponse();
  }

  private ResponseEntity<String> getGroupEntityGetRequest(Group g) {
    return getGroupEntityGetRequestAnd(g).getResponse();
  }

  private ResponseEntity<String> createGroupPostRequest(GroupRequest g) {
    return createGroupPostRequestAnd(g).getResponse();
  }

  private ResponseEntity<String> getUserEntityGetRequest(User u) {
    return getUserEntityGetRequestAnd(u).getResponse();
  }

  private ResponseEntity<String> getApplicationGetRequest(Application a) {
    return getApplicationGetRequestAnd(a).getResponse();
  }

  private ResponseEntity<String> getPolicyGetRequest(Policy p) {
    return getPolicyGetRequestAnd(p).getResponse();
  }

  private ResponseEntity<String> addGroupPermissionToGroupPostRequest(
      Group g, Policy p, AccessLevel mask) {
    return addGroupPermissionToGroupPostRequestAnd(g, p, mask).getResponse();
  }


  @lombok.Value
  @Builder
  public static class TestGroupData {
    @NonNull private final List<Group> groups;
    @NonNull private final List<Application> applications;
    @NonNull private final List<User> users;
    @NonNull private final List<Policy> policies;
  }

  @Autowired private GroupRepository groupRepository;
}
