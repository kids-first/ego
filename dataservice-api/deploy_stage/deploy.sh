#!/bin/bash

if [ $1 = "dev" ]; then
  rm -rf aws-ecs-service-*
  git clone git@github.com:kids-first/aws-ecs-service-type-1.git
  cd aws-ecs-service-type-1/
  echo "Setting up backend"
  echo 'key        = "dev/kf-dev-pi-egoserverservice-us-east-1-RSF"' >> dev.conf
  terraform init -backend=true -backend-config=dev.conf
  terraform validate -var 'image=538745987955.dkr.ecr.us-east-1.amazonaws.com/kf-api-egoserverservice:latest' \
   -var 'task_role_arn="arn:aws:iam::538745987955:role/kfArrangerserviceApiRole"' -var 'application=egoserverservice-api' \
   -var 'service_name="kf-api-egoserverservice"' -var 'owner="jenkins"' -var-file=dev.tfvar \
   -var 'vault_role="kf_egoserverservice_api_role"'
  terraform apply --auto-approve -var 'image=538745987955.dkr.ecr.us-east-1.amazonaws.com/kf-api-egoserverservice:latest' \
   -var 'task_role_arn="arn:aws:iam::538745987955:role/kfArrangerserviceApiRole"' -var 'application=egoserverservice-api' \
   -var 'service_name="kf-api-egoserverservice"' -var 'owner="jenkins"' -var-file=dev.tfvar \
   -var 'vault_role="kf_egoserverservice_api_role"'
fi

if [ $1 = "qa" ]; then
  rm -rf aws-ecs-service-*
  git clone git@github.com:kids-first/aws-ecs-service-type-1.git
  cd aws-ecs-service-type-1/
  echo "Setting up backend"
  echo 'key        = "qa/kf-qa-api-egoserverservice-us-east-1-RSF"' >> qa.conf
  terraform init -backend=true -backend-config=qa.conf
  terraform validate -var 'image=538745987955.dkr.ecr.us-east-1.amazonaws.com/kf-api-egoserverservice:latest' \
   -var 'task_role_arn="arn:aws:iam::538745987955:role/kfArrangerserviceApiRole"' -var 'application=egoserverservice-api' \
   -var 'service_name="kf-api-egoserverservice"' -var 'owner="jenkins"' -var-file=qa.tfvar \
   -var 'vault_role="kf_egoserverservice_api_role"'
  terraform apply --auto-approve -var 'image=538745987955.dkr.ecr.us-east-1.amazonaws.com/kf-api-egoserverservice:latest' \
   -var 'task_role_arn="arn:aws:iam::538745987955:role/kfArrangerserviceApiRole"' -var 'application=egoserverservice-api' \
   -var 'service_name="kf-api-egoserverservice"' -var 'owner="jenkins"' -var-file=qa.tfvar \
   -var 'vault_role="kf_egoserverservice_api_role"'
fi