@Library(value="kids-first/aws-infra-jenkins-shared-libraries", changelog=false) _
ecs_service_type_1_standard {
    projectName = "kf-portal-ego"
    organization = "kf-strides"
    environments = "dev,qa,prd"
    docker_image_type = "alpine"
    entrypoint_command = "/srv/ego/exec/run.sh" 
    deploy_scripts_version = "master"
    quick_deploy = "true"
    external_config_repo = "false"
    container_port = "8081"
    vcpu_container             = "2048"
    memory_container           = "4096"
    vcpu_task                  = "2048"
    memory_task                = "4096"
    health_check_path = "/swagger-ui.html#"
    dependencies = "postgres_rds"
}
