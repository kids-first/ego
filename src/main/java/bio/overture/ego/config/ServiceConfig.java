package bio.overture.ego.config;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.BaseRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.NamedRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.service.BaseService;
import bio.overture.ego.service.BaseServiceImpl;
import bio.overture.ego.service.NamedService;
import bio.overture.ego.service.NamedServiceImpl;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class ServiceConfig {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Bean
  public NamedService<User, UUID> userNamedService(){
    return createNamedService(User.class, userRepository);
  }

  @Bean
  public NamedService<Group, UUID> groupNamedService(){
    return createNamedService(Group.class, groupRepository);
  }

  @Bean
  public NamedService<Application, UUID> applicationNamedService(){
    return createNamedService(Application.class, applicationRepository);
  }

  public static <T extends Identifiable<ID> ,ID> NamedService<T, ID>
  createNamedService(Class<T> tClass, NamedRepository<T, ID> namedRepository){
    val baseService = createBaseService(tClass, namedRepository);
    return new NamedServiceImpl<>(baseService, namedRepository);
  }

  public static <T extends Identifiable<ID> ,ID> BaseService<T, ID>
  createBaseService(Class<T> tClass, BaseRepository<T, ID> baseRepository){
    return new BaseServiceImpl<T, ID>(tClass, baseRepository);
  }

}
