# AWS provider configuration
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "aws_s3_bucket_name" {
  description = "AWS S3 Bucket Name"
  type        = string
  default     = "bucket-hacka"
}

variable "aws_s3_access_key_id" {
  description = "The aws access key id"
  type        = string
  sensitive   = true
}

variable "aws_s3_secret_access_key" {
  description = "The aws secret access key"
  type        = string
  sensitive   = true
}

# Database hacka configuration
variable "db_hacka_username" {
  description = "The username for the Mongo hacka instance"
  type        = string
  sensitive   = true
}

variable "db_hacka_password" {
  description = "The password for the Mongo hacka instance"
  type        = string
  sensitive   = true
}

variable "db_hacka_name" {
  description = "Database hacka name"
  type        = string
  default     = "lanch_cat_db"
}

variable "db_hacka_port" {
  description = "Database hacka port"
  type        = string
  default     = "27017"
}

# variable "db_hacka_identifier" {
#   description = "The identifier for the RDS hacka instance"
#   type        = string
#   default     = "hacka-db"
# }

#Variaveis DockerHUB

variable "dockerhub_username" {
  description = "The username of the dockerhub image to deploy"
  type        = string
}

/*variable "dockerhub_token" {
  description = "The access token of the dockerhub image to deploy"
  type        = string
}*/