pipeline {
  agent { label 'maven-kaniko' }

  environment {
    IMAGE = "index.docker.io/frogrammer123/hello-spring:1.0.${BUILD_NUMBER}"
  }

  stages {

    stage('Build JAR') {
      steps {
        container('maven') {
          // 중앙 저장소 일시 오류 대비 재시도 + 타임아웃
          retry(2) {
            sh '''
              set -e
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
          // Kaniko가 참고할 인증 경로를 '쓰기 가능한' 워크스페이스로 설정
          withEnv(['DOCKER_CONFIG=/workspace/.docker']) {
            retry(2) {
              sh '''
                set -euo pipefail

                echo "[DEBUG] Kaniko context check:"
                echo "WORKSPACE=${WORKSPACE}"
                ls -la "${WORKSPACE}"
                test -f "${WORKSPACE}/Dockerfile" || { echo "[ERROR] Dockerfile not found at ${WORKSPACE}/Dockerfile"; exit 1; }

                # 쓰기 가능한 경로 준비
                mkdir -p "${DOCKER_CONFIG}"

                # (읽기전용) Secret 마운트가 있을 경우 그 '내용'만 복사해서 사용
                if [ -f /kaniko/.docker/.dockerconfigjson ]; then
                  echo "[DEBUG] Found /kaniko/.docker/.dockerconfigjson -> copying to ${DOCKER_CONFIG}/config.json"
                  cp /kaniko/.docker/.dockerconfigjson "${DOCKER_CONFIG}/config.json"
                else
                  echo "[WARN] /kaniko/.docker/.dockerconfigjson not found. If pushing to a private registry, auth may fail."
                fi

                # 이미지 빌드 & 푸시
                /kaniko/executor \
                  --context="${WORKSPACE}" \
                  --dockerfile="${WORKSPACE}/Dockerfile" \
                  --destination="${IMAGE}" \
                  --skip-tls-verify
              '''
            }
          }
        }
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        container('kubectl') {
          // 작은따옴표 대신 큰따옴표 사용하여 Groovy/쉘 변수 확장
          sh """
            set -e
            echo "[INFO] Rolling out image: ${env.IMAGE}"
            kubectl -n app-spring set image deploy/hello-spring app=${env.IMAGE}
            kubectl -n app-spring rollout status deploy/hello-spring
          """
        }
      }
    }
  }
}
