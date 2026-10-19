pipeline {
    agent any
    parameters {
        string(name: 'GREETING', defaultValue: 'Hello', description: 'Greeting text')
    }
    stages {
        stage('Run') {
            steps {
                echo "${params.GREETING}, Jenkins!"
            }
        }
    }
}