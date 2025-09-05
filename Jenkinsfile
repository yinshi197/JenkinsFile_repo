pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo '拉取代码...'
                checkout scm
            }
        }

        stage('Prepare') {
            steps {
                echo '设置脚本执行权限...'
                sh 'chmod +x scripts/*.sh'
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