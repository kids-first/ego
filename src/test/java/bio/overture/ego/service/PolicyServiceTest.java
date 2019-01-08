package bio.overture.ego.service;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.search.SearchFilter;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityGenerator.generateRandomUUIDNotIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class PolicyServiceTest extends AbstractNamedServiceTest<Policy, UUID, PolicyService> {

  private static final String POLICY1 = "Base,PolicyOne";
  private static final String POLICY2 = "Base,PolicyTwo";
  private static final String POLICY3 = "Base,PolicyThree";
  private static final Set<String> POLICIES = ImmutableSet.of(POLICY1, POLICY2, POLICY3);

  @Autowired private PolicyService policyService;
  @Autowired private EntityGenerator entityGenerator;


  private List<Group> groups;

  @Before
  public void setUp() {
    groups = entityGenerator.setupGroups("Group One", "GroupTwo", "Group Three");
    val firstGroup = groups.get(0);
    val existingPolicies = POLICIES.stream().map(x -> entityGenerator.setupPolicy(x, firstGroup.getName())).collect(toImmutableSet());
    val existingIds = existingPolicies.stream().map(Policy::getId).collect(toImmutableSet());
    val existingNames = existingPolicies.stream().map(Policy::getName).collect(toImmutableSet());
    val nonExistingId = generateRandomUUIDNotIn(existingIds);
    val nonExistingName = EntityGenerator.generateRandomNameNotIn(existingNames);
    val config = ServiceTestConfig.<Policy,UUID, PolicyService>builder()
        .entityType(Policy.class)
        .existingEntities(existingPolicies)
        .existingNames(existingNames)
        .nonExistingName(nonExistingName)
        .nonExistingId(nonExistingId)
        .service(policyService)
        .build();
    this.setConfig(config);
  }

  // Create
  @Test
  public void testCreate() {
    val policy = entityGenerator.setupPolicy("Study001,Group One");
    assertThat(policy.getName()).isEqualTo("Study001");
  }

  @Test
  @Ignore
  public void testCreateUniqueName() {
    //    policyService.create(entityGenerator.createPolicy(Pair.of("Study001",
    // groups.get(0).getId())));
    //    policyService.create(entityGenerator.createPolicy(Pair.of("Study002",
    // groups.get(0).getId())));
    //    assertThatExceptionOfType(DataIntegrityViolationException.class)
    //        .isThrownBy(() ->
    // policyService.create(entityGenerator.createPolicy(Pair.of("Study001",
    // groups.get(0).getId()))));
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Read
  @Test
  public void testGet() {
    val policy = entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    val savedPolicy = policyService.get(policy.getId().toString());
    assertThat(savedPolicy.getName()).isEqualTo("Study001");
  }

  @Test
  public void testGetNotFoundException() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> policyService.get(UUID.randomUUID().toString()));
  }

  @Test
  public void testGetByName() {
    entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    val savedUser = policyService.getByName("Study001");
    assertThat(savedUser.getName()).isEqualTo("Study001");
  }

  @Test
  public void testGetByNameAllCaps() {
    entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    val savedUser = policyService.getByName("STUDY001");
    assertThat(savedUser.getName()).isEqualTo("Study001");
  }

  @Test
  @Ignore
  public void testGetByNameNotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> policyService.getByName("Study000"));
  }

  @Test
  public void testListUsersNoFilters() {
    entityGenerator.setupTestPolicies();
    val aclEntities =
        policyService.listPolicies(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListUsersNoFiltersEmptyResult() {
    val aclEntities =
        policyService.listPolicies(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListUsersFiltered() {
    entityGenerator.setupTestPolicies();
    val userFilter = new SearchFilter("name", "Study001");
    val aclEntities =
        policyService.listPolicies(Arrays.asList(userFilter), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(1L);
  }

  @Test
  public void testListUsersFilteredEmptyResult() {
    entityGenerator.setupTestPolicies();
    val userFilter = new SearchFilter("name", "Study004");
    val aclEntities =
        policyService.listPolicies(Arrays.asList(userFilter), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(0L);
  }

  // Update
  @Test
  public void testUpdate() {
    val policy = entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    policy.setName("StudyOne");
    val updated = policyService.update(policy);
    assertThat(updated.getName()).isEqualTo("StudyOne");
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupTestPolicies();
    val policy = policyService.getByName("Study001");
    policyService.delete(policy.getId().toString());

    val remainingAclEntities =
        policyService.listPolicies(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(remainingAclEntities.getTotalElements()).isEqualTo(2L);
    assertThat(remainingAclEntities.getContent()).doesNotContain(policy);
  }
}
