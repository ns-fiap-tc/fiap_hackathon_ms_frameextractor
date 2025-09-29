# # Remote backend instance, to save tfstate
terraform {
  backend "s3" {
    bucket         = "hacka-tfstate-bucket"
    key            = "ms-frameextractor/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-lock"
    encrypt        = true
  }
}
