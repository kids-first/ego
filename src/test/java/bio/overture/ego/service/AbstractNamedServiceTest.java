package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.exceptions.NotFoundException;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Ignore
public abstract class AbstractNamedServiceTest<T extends Identifiable<ID>, ID, S extends NamedService<T, ID>> extends AbstractBaseServiceTest<T, ID, S> {

  @Test
  public void getEntityByName_WhenExisting_Success() {
    val existingName = getConfig().getExistingNames().stream().findAny().get();
    val actualEntity = getService().getByName(existingName);
    assertThat(getConfig().getExistingEntities()).containsAnyOf(actualEntity);
  }

  @Test
  public void getEntityByName_WhenNotExisting_ThrowsNotFoundError() {
    val throwable = catchThrowable(() -> getService().getByName(getConfig().getNonExistingName()));
    assertThat(throwable).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void findEntityByName_WhenExisting_PresentAndMatching() {
    val existingName = getConfig().getExistingNames().stream().findAny().get();
    val result = getService().findByName(existingName);
    assertThat(result).isNotEmpty();
    val actualEntity = result.get();
    assertThat(getConfig().getExistingEntities()).containsAnyOf(actualEntity);
  }

  @Test
  public void findEntityByName_WhenNotExisting_NotPresent() {
    val result = getService().findByName(getConfig().getNonExistingName());
    assertThat(result).isEmpty();
  }

}
