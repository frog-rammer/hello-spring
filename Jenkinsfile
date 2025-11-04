pipeline {
  agent { label 'maven-kaniko' }

  environment {
    IMAGE = "index.docker.io/frogrammer123/hello-spring:1.0.${BUILD_NUMBER}"
  }

  stages {

    stage('Build JAR') {
      steps {
        container('maven') {
          // 중앙 저장소 일시 오류 대비 재시도
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

                # (읽기전용) /kaniko/.docker/.dockerconfigjson 내용만 복사해 사용
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
      // 스테이지 전체 타임아웃: 예상치 못한 무한 대기 차단
      timeout(time: 5, unit: 'MINUTES') {
        steps {
          container('kubectl') {
            // Groovy 변수 확장이 필요하므로 """ 사용
            sh """
              set -euo pipefail

              echo "[INFO] Target image: ${env.IMAGE}"
              echo "[CHECK] kubectl connectivity"
              kubectl version --short || true
              kubectl get ns || true

              echo "[APPLY] Set image on Deployment"
              # 컨테이너 이름 'app'이 Deployment 내 컨테이너 이름과 반드시 일치해야 함
              kubectl -n app-spring set image deploy/hello-spring app=${env.IMAGE}

              echo "[WAIT] Rollout status (with timeout)"
              # 무한 대기 방지를 위해 rollout에 타임아웃 부여
              if ! kubectl -n app-spring rollout status deploy/hello-spring --timeout=120s; then
                echo "[ERROR] Rollout timed out. Dumping diagnostics..."
                kubectl -n app-spring get pods -o wide || true
                kubectl -n app-spring describe deploy/hello-spring || true
                kubectl -n app-spring get events --sort-by=.metadata.creationTimestamp | tail -n 100 || true
                exit 1
              fi

              echo "[OK] Rollout succeeded."
              kubectl -n app-spring get pods -o wide
            """
          }
        }
      }
    }
  }
}
