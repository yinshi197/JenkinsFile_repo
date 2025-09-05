pipeline {
    agent any
    stages {
        stage('Parallel Stages') {
            parallel {
                stage('Build') { steps { sh 'scripts/build.sh' } }
                stage('Test')  { steps { sh 'scripts/test.sh' } }
            }
        }
    }
}