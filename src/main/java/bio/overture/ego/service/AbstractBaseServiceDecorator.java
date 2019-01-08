package bio.overture.ego.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class that helps reduces delegate method declaration in all services implementing the BaseService interface.
 * This is a bonus because @NonNull can be defined once here
 */
@RequiredArgsConstructor
public abstract class AbstractBaseServiceDecorator<T, ID> implements BaseService<T, ID> {

  @NonNull private final BaseService<T, ID> internalService;

  @Override public String getEntityTypeName() {
    return internalService.getEntityTypeName();
  }

  @Override public Optional<T> findById(@NonNull ID id) {
    return internalService.findById(id);
  }

  @Override public boolean isExist(@NonNull ID id) {
    return internalService.isExist(id);
  }

  @Override public void delete(@NonNull ID id) {
    internalService.delete(id);
  }

  @Override public Set<T> getMany(@NonNull List<ID> ids) {
    return internalService.getMany(ids);
  }
}
