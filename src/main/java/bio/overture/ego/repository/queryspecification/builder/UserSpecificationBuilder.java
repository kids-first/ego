package bio.overture.ego.repository.queryspecification.builder;

import bio.overture.ego.model.entity.User;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.criteria.Root;
import java.util.UUID;

import static bio.overture.ego.model.enums.JavaFields.APPLICATION;
import static bio.overture.ego.model.enums.JavaFields.GROUP;
import static bio.overture.ego.model.enums.JavaFields.USERAPPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.USERGROUPS;
import static bio.overture.ego.model.enums.JavaFields.USERPERMISSIONS;
import static javax.persistence.criteria.JoinType.LEFT;

@Setter
@Accessors(fluent = true, chain = true)
public class UserSpecificationBuilder extends AbstractSpecificationBuilder<User, UUID> {

  private boolean fetchUserPermissions;
  private boolean fetchUserGroups;
  private boolean fetchApplications;

  @Override
  protected Root<User> setupFetchStrategy(Root<User> root) {
    if (fetchApplications) {
      root.fetch(USERAPPLICATIONS, LEFT)
          .fetch(APPLICATION, LEFT);
    }
    if (fetchUserGroups) {
      root.fetch(USERGROUPS, LEFT)
          .fetch(GROUP, LEFT);
    }
    if (fetchUserPermissions) {
      root.fetch(USERPERMISSIONS, LEFT);
    }
    return root;
  }
}
