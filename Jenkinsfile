pipeline {
    agent { label 'Windows-X64-Test' }

    stages {
        stage('Checkout') {
            steps {
                echo '拉取代码...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'scripts/build.sh'
            }
        }

        stage('Test') {
            steps {
                sh 'scripts/test.sh'
            }
        }
    }

    post {
        always {
            echo '流水线结束'
        }
    }
}