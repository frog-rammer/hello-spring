pipeline {
  agent { label 'maven-kaniko' }

  environment {
    IMAGE = "index.docker.io/frog-rammer/hello-spring:1.0.${BUILD_NUMBER}"
  }

  stages {

    stage('Build JAR') {
      steps {
        container('maven') {
          // 중앙 저장소 일시 오류 대비 재시도 + 타임아웃
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
          withEnv(['DOCKER_CONFIG=/kaniko/.docker']) {   // Kaniko 인증 경로
            retry(2) {
              sh '''
                set -e
                echo "[DEBUG] Kaniko context check:"
                echo "WORKSPACE=$WORKSPACE"
                ls -la "$WORKSPACE"
                test -f "$WORKSPACE/Dockerfile" || { echo "Dockerfile not found at $WORKSPACE/Dockerfile"; exit 1; }

                # docker-registry 시크릿(.dockerconfigjson) → Kaniko 표준(config.json)
                if [ -f /kaniko/.docker/.dockerconfigjson ]; then
                  cp /kaniko/.docker/.dockerconfigjson /kaniko/.docker/config.json
                fi

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
    }

    stage('Deploy to Kubernetes') {
      steps {
        container('kubectl') {
          // 작은따옴표 → 큰따옴표(쉘 변수 확장) 또는 Groovy 치환 사용
          sh """
            kubectl -n app-spring set image deploy/hello-spring app=${env.IMAGE}
            kubectl -n app-spring rollout status deploy/hello-spring
          """
        }
      }
    }
  }
}
