pipeline {
  agent { label 'maven-kaniko' }
  options { timestamps() }

  environment {
    REGISTRY   = 'index.docker.io'
    IMAGE      = 'frog-rammer/hello-spring'
    NAMESPACE  = 'app-spring'
    DOCKER_DEST = "${REGISTRY}/${IMAGE}:1.0.${BUILD_NUMBER}"
  }

  stages {
    stage('Build JAR') {
      steps {
        container('maven') {
          retry(2) { // Maven Central 500/네트워크 일시 오류 흡수
            sh '''
              mvn -U \
                  -Dmaven.wagon.http.retryHandler.count=5 \
                  -Dmaven.wagon.httpconnectionManager.ttl=300 \
                  -Dmaven.wagon.http.pool=true \
                  -Dmaven.wagon.rto=60000 \
                  -Dmaven.wagon.connectTimeout=60000 \
                  -Dmaven.test.skip=true \
                  clean package
            '''
          }
        }
      }
    }

    stage('Build & Push Docker Image') {
      steps {
        container('kaniko') {
          retry(2) { // 레지스트리 푸시 일시 실패 대비
            sh '''
              /kaniko/executor \
                --context=/workspace \
                --dockerfile=/workspace/Dockerfile \
                --destination="$DOCKER_DEST" \
                --insecure --skip-tls-verify
            '''
          }
        }
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        container('kubectl') {
          sh '''
            set -euo pipefail
            kubectl -n "$NAMESPACE" set image deploy/hello-spring app="$DOCKER_DEST"
            kubectl -n "$NAMESPACE" rollout status deploy/hello-spring --timeout=180s
          '''
        }
      }
    }
  }

  post {
    always {
      echo "Build URL: ${env.BUILD_URL}"
    }
  }
}
