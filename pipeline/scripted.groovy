node {
    stage('Build') {
        sh 'scripts/build.sh'
    }
    stage('Test') {
        sh 'scripts/test.sh'
    }
}