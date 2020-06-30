pipeline {
  agent {
    docker {
      image 'maven:3.6.3-jdk-11'
      args  '-e DOCKER_HOST=${DOCKER_HOST} -u root --tmpfs /tmp:rw,exec,size=1g --tmpfs /root/.m2:rw,exec,size=1g --tmpfs ${WORKSPACE},size=4g'
    }
  }

  stages {
    stage('Check compilation') {
      steps {
        sh 'mvn clean package -DskipTests -T1C'
      }
    }
  }
}
