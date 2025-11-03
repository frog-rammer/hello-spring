pipeline {
  agent { label 'maven-kaniko' }

  stages {
    stage('Build JAR') {
      steps {
        container('maven') {
          sh 'mvn -Dmaven.test.skip=true clean package'
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
