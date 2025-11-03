pipeline {
  agent { label 'maven-kaniko' }

  environment {
    IMAGE = "index.docker.io/frog-rammer/hello-spring:1.0.${BUILD_NUMBER}"
  }

  stages {
    stage('Build JAR') {
      steps {
        container('maven') {
          // 네트워크/중앙 저장소 일시 오류 대비 재시도 + 타임아웃 설정
          retry(2) {
            sh '''
              echo "[DEBUG] PWD=$(pwd)"
              echo "[DEBUG] LS (repo root):"
              ls -la
              mvn -U \
                  -Dmaven.wagon.http.retryHandler.count=5 \
                  -Dmaven.wagon.rto=60000 \
                  -Dmaven.wagon.connectTimeout=60000 \
                  -Dmaven.test.skip=true clean package
            '''
          }
        }
      }
    }

    stage('Build & Push Docker Image') {
      steps {
        container('kaniko') {
          // Kaniko가 정확한 경로를 보도록 $WORKSPACE를 명시
          retry(2) {
            sh '''
              echo "[DEBUG] Kaniko context check:"
              echo "WORKSPACE=$WORKSPACE"
              ls -la "$WORKSPACE"
              test -f "$WORKSPACE/Dockerfile" || { echo "Dockerfile not found at $WORKSPACE/Dockerfile"; exit 1; }

              /kaniko/executor \
                --context="$WORKSPACE" \
                --dockerfile="$WORKSPACE/Dockerfile" \
                --destination="$IMAGE" \
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
            kubectl -n app-spring set image deploy/hello-spring app='$IMAGE'
            kubectl -n app-spring rollout status deploy/hello-spring
          '''
        }
      }
    }
  }
}
