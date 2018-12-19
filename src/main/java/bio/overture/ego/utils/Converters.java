package bio.overture.ego.utils;

import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.isNull;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Converters {

  public static List<UUID> convertToUUIDList(Collection<String> uuids){
    return uuids.stream()
        .map(UUID::fromString)
        .collect(toImmutableList());
  }

  public static Set<UUID> convertToUUIDSet(Collection<String> uuids){
    return uuids.stream()
        .map(UUID::fromString)
        .collect(toImmutableSet());
  }

  public static <T> List<T> nullToEmptyList(List<T> list){
    if (isNull(list)){
      return newArrayList();
    } else {
      return list;
    }
  }

  public static <T> Set<T> nullToEmptySet(Set<T> set){
    if (isNull(set)){
      return newHashSet();
    } else {
      return set;
    }
  }

  public static <T> Collection<T> nullToEmptyCollection(Collection<T> collection){
    if (isNull(collection)){
      return newHashSet();
    } else {
      return collection;
    }
  }


}
