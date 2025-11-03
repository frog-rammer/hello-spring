pipeline {
  agent { label 'maven-kaniko' }

  stages {
    stage('Build JAR') {
      steps {
        container('maven') {
          retry(2) { // Maven Central 500/네트워크 일시 오류 방지 재시도
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
          sh '''
            /kaniko/executor \
              --context=/workspace \
              --dockerfile=/workspace/Dockerfile \
              --destination=index.docker.io/frog-rammer/hello-spring:1.0.${BUILD_NUMBER} \
              --insecure --skip-tls-verify
          '''
        }
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        container('kubectl') {
          sh '''
            kubectl -n app-spring set image deploy/hello-spring app=index.docker.io/frog-rammer/hello-spring:1.0.${BUILD_NUMBER}
            kubectl -n app-spring rollout status deploy/hello-spring
          '''
        }
      }
    }
  }
}
