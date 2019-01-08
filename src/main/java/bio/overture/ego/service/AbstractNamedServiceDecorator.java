package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import lombok.NonNull;

import java.util.Optional;

/**
 * Helper class that helps reduces delegate method declaration in all services implementing the NamedService interface.
 * This is a bonus because @NonNull can be defined once here
 */
public abstract class AbstractNamedServiceDecorator<T extends Identifiable<ID>, ID>
    extends AbstractBaseServiceDecorator<T, ID>
    implements NamedService<T, ID>{

  private final NamedService<T, ID> internalNamedService;

  public AbstractNamedServiceDecorator(@NonNull NamedService<T, ID> internalNamedService) {
    super(internalNamedService);
    this.internalNamedService = internalNamedService;
  }

  @Override public Optional<T> findByName(@NonNull String name) {
    return internalNamedService.findByName(name);
  }

  @Override public T getByName(@NonNull String name) {
    return internalNamedService.getByName(name);
  }

}
