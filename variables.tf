variable "service_name" {
  default = "default"
}
variable "environment" {
}
variable "application" {
  default = "egoserverservice-api"
}
variable "organization" {
  default="kf"
}
variable "region" {
  default="us-east-1"
}
variable "chop_cidr" {}
variable "bucket" {}
variable "owner" {}
variable "organzation" {}
variable "image" {
  default="538745987955.dkr.ecr.us-east-1.amazonaws.com/kf-api-egoserverservice"
}
variable "task_role_arn" {
}
variable "vault_url" {
}
variable "pg_host" {}
variable "db_secret_path" {
  default=""
}
variable "pg_db_name" {
  default = "ego"
}
variable "vault_role" {}
