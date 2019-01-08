package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.NamedRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;

@RequiredArgsConstructor
public final class NamedServiceImpl<T extends Identifiable<ID>, ID> implements NamedService<T, ID> {

  private final BaseService<T, ID> baseService;
  private final NamedRepository<T, ID> namedRepository;

  /**
   * Implementation
   */
  @Override
  public Optional<T> findByName(String name) {
    return namedRepository.findByName(name);
  }

  @Override
  public T getByName(String name) {
    val result = findByName(name);
    checkExists(
        result.isPresent(),
        "The '%s' entity with name '%s' was not found",
        getEntityTypeName(),
        name);
    return result.get();
  }

  /**
   * Delegates
   */
  @Override public String getEntityTypeName() {
    return baseService.getEntityTypeName();
  }

  @Override public Optional<T> findById(ID id) {
    return baseService.findById(id);
  }

  @Override public boolean isExist(ID id) {
    return baseService.isExist(id);
  }

  @Override public void delete(ID id) {
    baseService.delete(id);
  }

  @Override public Set<T> getMany(List<ID> ids) {
    return baseService.getMany(ids);
  }

}
