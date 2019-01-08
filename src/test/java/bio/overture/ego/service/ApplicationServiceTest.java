package bio.overture.ego.service;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.token.app.AppTokenClaims;
import bio.overture.ego.utils.EntityGenerator;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityGenerator.generateRandomNameNotIn;
import static bio.overture.ego.utils.EntityGenerator.generateRandomUUIDNotIn;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class ApplicationServiceTest extends AbstractNamedServiceTest<Application, UUID, ApplicationService> {


  private static final String APP1 = "Base ApplicationOne";
  private static final String APP2 = "Base ApplicationTwo";
  private static final String APP3 = "Base ApplicationThree";
  private static final Set<String> APPS = ImmutableSet.of(APP1, APP2, APP3);

  @Autowired private ApplicationService applicationService;
  @Autowired private UserService userService;
  @Autowired private GroupService groupService;
  @Autowired private EntityGenerator entityGenerator;

  @Before
  public void beforeTest(){
    val existingUsers = APPS.stream().map(entityGenerator::setupApplication).collect(toImmutableSet());
    val existingIds = existingUsers.stream().map(Application::getId).collect(toImmutableSet());
    val existingNames = existingUsers.stream().map(Application::getName).collect(toImmutableSet());
    val nonExistingId = generateRandomUUIDNotIn(existingIds);
    val nonExistingName = generateRandomNameNotIn(existingNames);
    val config = ServiceTestConfig.<Application,UUID, ApplicationService>builder()
        .entityType(Application.class)
        .existingEntities(existingUsers)
        .existingNames(existingNames)
        .nonExistingName(nonExistingName)
        .nonExistingId(nonExistingId)
        .service(applicationService)
        .build();
    this.setConfig(config);
  }

  // Create
  @Test
  public void testCreate() {
    val application = entityGenerator.setupApplication("123456");
    assertThat(application.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testCreateUniqueClientId() {
    val one = entityGenerator.setupApplication("111111");
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() -> applicationService.create(one));
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Get
  @Test
  public void testGet() {
    val application = entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.get(application.getId().toString());
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> applicationService.get(UUID.randomUUID().toString()));
  }

  @Test
  public void testGetByName() {
    entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.getByName("Application 123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetByNameAllCaps() {
    entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.getByName("APPLICATION 123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  @Ignore
  public void testGetByNameNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> applicationService.getByName("Application 123456"));
  }

  @Test
  public void testGetByClientId() {
    entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.getApplicationByClientId("123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  @Ignore
  public void testGetByClientIdNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> applicationService.getApplicationByClientId("123456"));
  }

  // List
  @Test
  public void testListAppsNoFilters() {
    entityGenerator.setupTestApplications();

    val applications =
        applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(5L);
  }

  @Test
  public void testListAppsNoFiltersEmptyResult() {
    val applications =
        applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListAppsFiltered() {
    entityGenerator.setupTestApplications();
    val clientIdFilter = new SearchFilter("clientId", "333333");
    val applications =
        applicationService.listApps(
            singletonList(clientIdFilter), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("333333");
  }

  @Test
  public void testListAppsFilteredEmptyResult() {
    entityGenerator.setupTestApplications();
    val clientIdFilter = new SearchFilter("clientId", "666666");
    val applications =
        applicationService.listApps(
            singletonList(clientIdFilter), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  // Find
  @Test
  public void testFindAppsNoFilters() {
    entityGenerator.setupTestApplications();
    val applications =
        applicationService.findApps(
            "222222", Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("222222");
  }

  @Test
  public void testFindAppsFiltered() {
    entityGenerator.setupTestApplications();
    val clientIdFilter = new SearchFilter("clientId", "333333");
    val applications =
        applicationService.findApps(
            "222222", singletonList(clientIdFilter), new PageableResolver().getPageable());
    // Expect empty list
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsNoQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = userService.getByName("SecondUser@domain.com");

    val application = applicationService.getApplicationByClientId("444444");

    user.associateWithApplication(application);
    userTwo.associateWithApplication(application);

    val applications =
        applicationService.findUserApps(
            user.getId().toString(), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("444444");
  }

  @Test
  public void testFindUsersAppsNoQueryNoFiltersNoUser() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applications =
        applicationService.findUserApps(
            user.getId().toString(), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsNoQueryNoFiltersEmptyUserString() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                applicationService.findUserApps(
                    "", Collections.emptyList(), new PageableResolver().getPageable()));
  }

  @Test
  public void testFindUsersAppsNoQueryFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getApplicationByClientId("111111");
    val applicationTwo = applicationService.getApplicationByClientId("555555");

    user.associateWithApplication(applicationOne);
    user.associateWithApplication(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "111111");

    val applications =
        applicationService.findUserApps(
            user.getId().toString(),
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("111111");
  }

  @Test
  public void testFindUsersAppsQueryAndFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getApplicationByClientId("333333");
    val applicationTwo = applicationService.getApplicationByClientId("444444");

    user.associateWithApplication(applicationOne);
    user.associateWithApplication(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications =
        applicationService.findUserApps(
            user.getId().toString(),
            "444444",
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getApplicationByClientId("222222");
    val applicationTwo = applicationService.getApplicationByClientId("444444");

    user.associateWithApplication(applicationOne);
    user.associateWithApplication(applicationTwo);

    val applications =
        applicationService.findUserApps(
            user.getId().toString(),
            "222222",
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("222222");
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupTwo = groupService.getByName("Group Two");

    val application = applicationService.getApplicationByClientId("111111");

    group.getApplications().add(application);
    groupTwo.getApplications().add(application);

    val applications =
        applicationService.findGroupApplications(
            group.getId().toString(),
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("111111");
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFiltersNoGroup() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val applications =
        applicationService.findGroupApplications(
            group.getId().toString(),
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFiltersEmptyGroupString() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                applicationService.findGroupApplications(
                    "", Collections.emptyList(), new PageableResolver().getPageable()));
  }

  @Test
  public void testFindGroupsAppsNoQueryFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val applicationOne = applicationService.getApplicationByClientId("222222");
    val applicationTwo = applicationService.getApplicationByClientId("333333");

    group.getApplications().add(applicationOne);
    group.getApplications().add(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications =
        applicationService.findGroupApplications(
            group.getId().toString(),
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("333333");
  }

  @Test
  public void testFindGroupsAppsQueryAndFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group Three");
    val applicationOne = applicationService.getApplicationByClientId("333333");
    val applicationTwo = applicationService.getApplicationByClientId("444444");

    group.getApplications().add(applicationOne);
    group.getApplications().add(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications =
        applicationService.findGroupApplications(
            group.getId().toString(),
            "444444",
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupsAppsQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val applicationOne = applicationService.getApplicationByClientId("444444");
    val applicationTwo = applicationService.getApplicationByClientId("555555");

    group.getApplications().add(applicationOne);
    group.getApplications().add(applicationTwo);

    val applications =
        applicationService.findGroupApplications(
            group.getId().toString(),
            "555555",
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("555555");
  }

  // Update
  @Test
  public void testUpdate() {
    val application = entityGenerator.setupApplication("123456");
    application.setName("New Name");
    val updated = applicationService.update(application);
    assertThat(updated.getName()).isEqualTo("New Name");
  }

  @Test
  public void testUpdateNonexistentEntity() {
    entityGenerator.setupApplication("123456");
    val nonExistentEntity =
        Application.builder()
            .clientId("123456")
            .name("DoesNotExist")
            .clientSecret("654321")
            .build();
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
        .isThrownBy(() -> applicationService.update(nonExistentEntity));
  }

  @Test
  public void testUpdateIdNotAllowed() {
    val application = entityGenerator.setupApplication("123456");
    application.setId(new UUID(12312912931L, 12312912931L));
    // New id means new non-existent policy or one that exists and is being overwritten
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> applicationService.update(application));
  }

  @Test
  @Ignore
  public void testUpdateClientIdNotAllowed() {
    //    entityGenerator.setupTestApplications();
    //    val application = applicationService.getByClientId("111111");
    //    application.setClientId("222222");
    //    val updated = applicationService.update(application);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  @Ignore
  public void testUpdateStatusNotInAllowedEnum() {
    //    entityGenerator.setupTestApplications();
    //    val application = applicationService.getByClientId("111111");
    //    application.setStatus("Junk");
    //    val updated = applicationService.update(application);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupTestApplications();

    val application = applicationService.getApplicationByClientId("222222");
    applicationService.delete(application.getId().toString());

    val applications =
        applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(4L);
    assertThat(applications.getContent()).doesNotContain(application);
  }

  @Test
  public void testDeleteNonExisting() {
    entityGenerator.setupTestApplications();
    assertThatExceptionOfType(EmptyResultDataAccessException.class)
        .isThrownBy(() -> applicationService.delete(UUID.randomUUID().toString()));
  }

  @Test
  public void testDeleteEmptyIdString() {
    entityGenerator.setupTestApplications();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> applicationService.delete(""));
  }

  // Special (LoadClient)
  @Test
  public void testLoadClientByClientId() {
    val application = entityGenerator.setupApplication("123456");
    application.setStatus("Approved");
    applicationService.update(application);

    val client = applicationService.loadClientByClientId("123456");

    assertThat(client.getClientId()).isEqualToIgnoringCase("123456");
    assertThat(
        client
            .getAuthorizedGrantTypes()
            .containsAll(Arrays.asList(AppTokenClaims.AUTHORIZED_GRANTS)));
    assertThat(client.getScope().containsAll(Arrays.asList(AppTokenClaims.SCOPES)));
    assertThat(client.getRegisteredRedirectUri()).isEqualTo(application.getURISet());
    assertThat(client.getAuthorities())
        .containsExactly(new SimpleGrantedAuthority(AppTokenClaims.ROLE));
  }

  @Test
  public void testLoadClientByClientIdNotFound() {
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("Client ID not found.");
  }

  @Test
  public void testLoadClientByClientIdEmptyString() {
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId(""))
        .withMessage("Client ID not found.");
  }

  @Test
  public void testLoadClientByClientIdNotApproved() {
    val application = entityGenerator.setupApplication("123456");
    application.setStatus("Pending");
    applicationService.update(application);
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("Client Access is not approved.");
    application.setStatus("Rejected");
    applicationService.update(application);
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("Client Access is not approved.");
    application.setStatus("Disabled");
    applicationService.update(application);
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("Client Access is not approved.");
  }
}
