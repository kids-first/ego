package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.exceptions.NotFoundException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Ignore
public abstract class AbstractBaseServiceTest<T extends Identifiable<ID>, ID, S extends BaseService<T, ID>> {

  @Setter @Getter private ServiceTestConfig<T, ID, S> config;

  public S getService(){
    return config.getService();
  }

  private ID getAnyExistingId(){
    return config.getExistingEntities().stream().findAny().get().getId();
  }

  @Test
  public void entityDeletion_WhenExisting_SuccessfullyDeleted(){
    val existingId = getAnyExistingId();
    assertThat(getService().isExist(existingId)).isTrue();
    getService().delete(existingId);
    assertThat(getService().isExist(existingId)).isFalse();
    config.getExistingEntities().stream()
        .filter(x -> !x.getId().equals(existingId))
        .map(Identifiable::getId)
        .forEach(x -> assertThat(getService().isExist(x)).isTrue());
  }

  @Test
  public void entityDeletion_WhenNotExisting_ThrowsNotFoundError(){
    val idToDelete = config.getNonExistingId();
    assertThat(getService().isExist(idToDelete)).isFalse();
    val throwable = catchThrowable(() -> getService().delete(idToDelete));
    assertThat(throwable).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void entityExistence_WhenExisting_True(){
    val existingId = getAnyExistingId();
    assertThat(getService().isExist(existingId)).isTrue();
  }

  @Test
  public void entityExistence_WhenNotExisting_False(){
    val nonExistingId = config.getNonExistingId();
    assertThat(getService().isExist(nonExistingId)).isFalse();
  }

  @Test
  public void findEntityById_WhenExisting_Present(){
    val existingId = getAnyExistingId();
    assertThat(getService().isExist(existingId)).isTrue();
    assertThat(getService().findById(existingId)).isNotEmpty();
  }

  @Test
  public void findEntityById_WhenNotExisting_NotPresent(){
    val nonExistingId = config.getNonExistingId();
    assertThat(getService().isExist(nonExistingId)).isFalse();
    assertThat(getService().findById(nonExistingId)).isEmpty();
  }

  @Test
  public void getEntityById_WhenExisting_Success(){
    val existingId = getAnyExistingId();
    assertThat(getService().isExist(existingId)).isTrue();
    assertThat(getService().getById(existingId).getId()).isEqualTo(existingId);
  }

  @Test
  public void getEntityById_WhenNotExisting_ThrowsNotFoundError(){
    val nonExistingId = config.getNonExistingId();
    assertThat(getService().isExist(nonExistingId)).isFalse();
    val throwable = catchThrowable(() -> getService().getById(nonExistingId));
    assertThat(throwable).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void getEntityName_OfCurrentEntityClass_Matching(){
    assertThat(getService().getEntityTypeName()).isEqualTo(config.getEntityType().getSimpleName());
  }

  @Test
  public void checkEntityExistence_WhenExisting_Nothing(){
    val existingId = getAnyExistingId();
    assertThat(getService().isExist(existingId)).isTrue();
    getService().checkExistence(existingId);
  }

  @Test
  public void checkEntityExistence_WhenNotExisting_ThrowsNotFoundError(){
    val nonExistingId = config.getNonExistingId();
    assertThat(getService().isExist(nonExistingId)).isFalse();
    val throwable = catchThrowable(() -> getService().checkExistence(nonExistingId));
    assertThat(throwable).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void getManyEntities_WhenAllExisting_AllMatching(){
    val existingIds = config.getExistingEntities().stream()
        .map(Identifiable::getId)
        .collect(toImmutableList());
    existingIds.forEach(x -> assertThat(getService().isExist(x)).isTrue());
    val actualIds = getService().getMany(existingIds)
        .stream()
        .map(Identifiable::getId)
        .collect(toImmutableSet());
    assertThat(actualIds).containsExactlyInAnyOrderElementsOf(existingIds);
  }

  @Test
  public void getManyEntities_WhenSomeExisting_AllExistingAreMatching(){
    val existingIds = config.getExistingEntities().stream()
        .map(Identifiable::getId)
        .collect(toImmutableList());
    existingIds.forEach(x -> assertThat(getService().isExist(x)).isTrue());
    val idsToSearch = Lists.<ID>newArrayList();
    idsToSearch.addAll(existingIds);
    idsToSearch.add(config.getNonExistingId());
    val actualIds = getService().getMany(idsToSearch)
        .stream()
        .map(Identifiable::getId)
        .collect(toImmutableSet());
    assertThat(actualIds).containsExactlyInAnyOrderElementsOf(existingIds);
  }

  @Test
  public void getManyEntities_WhenNoneExisting_EmptyCollection(){
    assertThat(getService().isExist(config.getNonExistingId())).isFalse();
    val actualIds = getService().getMany(ImmutableList.of(config.getNonExistingId()))
        .stream()
        .map(Identifiable::getId)
        .collect(toImmutableSet());
    assertThat(actualIds).isEmpty();
  }

}
