resource "kubernetes_secret" "secrets-ms-frameextractor" {
  metadata {
    name = "secrets-ms-frameextractor"
  }

  type = "Opaque"

  data = {
    /*DB_HOST             = data.kubernetes_service.mongodb-service.metadata[0].name
    DB_PORT             = var.db_hacka_port
    DB_NAME             = var.db_hacka_name
    DB_USER             = var.db_hacka_username
    DB_PASS             = var.db_hacka_password */

    MESSAGE_QUEUE_HOST   = data.kubernetes_service.messagequeue_service.metadata[0].name
    NOTIFICACAO_SERVICE_CLIENT = data.kubernetes_service.service-ms-notificacao.metadata[0].name

    AWS_REGION=var.aws_region
    AWS_S3_BUCKET_NAME=var.aws_s3_bucket_name
    AWS_ACCESS_KEY_ID=var.aws_s3_access_key_id
    AWS_SECRET_ACCESS_KEY=var.aws_s3_secret_access_key
  }

  lifecycle {
    prevent_destroy = false
  }
}

# MS FRAMEEXTRACTOR 
resource "kubernetes_deployment" "deployment-ms-frameextractor" {
  metadata {
    name      = "deployment-ms-frameextractor"
    namespace = "default"
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "deployment-ms-frameextractor"
      }
    }

    template {
      metadata {
        labels = {
          app = "deployment-ms-frameextractor"
        }
      }

      spec {
        toleration {
          key      = "key"
          operator = "Equal"
          value    = "value"
          effect   = "NoSchedule"
        }

        container {
          name  = "deployment-ms-frameextractor-container"
          image = "${var.dockerhub_username}/fiap_hackathon_ms_frameextractor:latest"

          resources {
            requests = {
              memory : "512Mi"
              cpu : "500m"
            }
            limits = {
              memory = "1Gi"
              cpu    = "1"
            }
          }

          env_from {
            secret_ref {
              name = kubernetes_secret.secrets-ms-frameextractor.metadata[0].name
            }
          }

          env {
            name  = "MANAGEMENT_OTLP_METRICS_EXPORT_URL"
            value = "http://otel-collector.observability.svc.cluster.local:4318/v1/metrics"
          }
          env {
            name  = "MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED"
            value = "true"
          }
          env {
            name  = "MANAGEMENT_METRICS_TAGS_APPLICATION"
            value = "frameextractor-service"
          }
          env {
            name  = "MANAGEMENT_METRICS_TAGS_ENVIRONMENT"
            value = "production"
          }

          port {
            container_port = "8080"
          }
          # Liveness Probe para verificar se a aplicação está "viva"
          liveness_probe {
            http_get {
              path = "/actuator/health" 
              port = 8080
            }
            initial_delay_seconds = 60 # Espera 60s antes da primeira verificação
            period_seconds        = 10  # Verifica a cada 10s
            timeout_seconds       = 5   # Considera falha se não responder em 5s
            failure_threshold     = 3   # Tenta 3 vezes antes de reiniciar o container
          }

          # Readiness Probe para verificar se a aplicação está pronta para receber tráfego
          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 60 # Espera 60s antes de marcar como "pronto"
            period_seconds        = 10
            timeout_seconds       = 5
            failure_threshold     = 3
          }
        }
      }
    }
  }
  #depends_on = [kubernetes_deployment.messagequeue_deployment]
}

resource "kubernetes_service" "service-ms-frameextractor" {
  metadata {
    name      = "service-ms-frameextractor"
    namespace = "default"
    annotations = {
      "service.beta.kubernetes.io/aws-load-balancer-type" : "nlb",
      "service.beta.kubernetes.io/aws-load-balancer-scheme" : "internal",
      "service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled" : "true",
      "prometheus.io/scrape" = "true",
      "prometheus.io/port" = "8080",
      "prometheus.io/path" = "/actuator/prometheus"
    }
  }
  spec {
    selector = {
      app = "deployment-ms-frameextractor"
    }
    port {
      port = "80"
      target_port = "8080"
    }
    type = "LoadBalancer"
  }
}

# Horizontal Pod Autoscaler (HPA)
resource "kubernetes_horizontal_pod_autoscaler_v2" "hpa-ms-frameextractor" {
  metadata {
    name      = "hpa-ms-frameextractor"
    namespace = "default"
  }

  spec {
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment.deployment-ms-frameextractor.metadata[0].name
    }

    min_replicas = 1
    max_replicas = 5

    metric {
      type = "Resource"
      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = 70 # Escala se o uso médio de CPU passar de 70% do "request"
        }
      }
    }
  }
}