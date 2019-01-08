package bio.overture.ego.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Builder
@Value
public class ServiceTestConfig<T, ID, S extends BaseService<T, ID>>{

  @NonNull private final Class<T> entityType;
  @NonNull private final S service;
  @NonNull private final ID nonExistingId;
  @NonNull private final String nonExistingName;
  @NonNull private final Set<String> existingNames;
  @NonNull private final Set<T> existingEntities;

}
