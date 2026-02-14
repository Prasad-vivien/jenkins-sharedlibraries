def call (String dockerRegistry,
          String dockerImageTag,
          String kubernetesDeployment,
          String kubernetesContainer,
          String awsCredID,
          String awsRegion,
          String eksClusterName,
          String kubernetesNamespace = 'default') {

    sh """
        if ! command -v aws > /dev/null; then
            echo "Installing AWS CLI..."
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
            sudo apt update -y
            sudo apt install unzip -y
            unzip awscliv2.zip
            sudo ./aws/install
            rm -rf awscliv2.zip aws
        fi

        if ! command -v kubectl > /dev/null; then
            curl -LO "https://dl.k8s.io/release/\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
            sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
        fi
    """

    withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        credentialsId: awsCredID
    ]]) {

        sh """
            export AWS_DEFAULT_REGION=${awsRegion}

            aws eks update-kubeconfig --region ${awsRegion} --name ${eksClusterName}

            if kubectl get deployment ${kubernetesDeployment} -n ${kubernetesNamespace} > /dev/null 2>&1; then
                echo "Deployment exists. Updating image..."
                kubectl set image deployment/${kubernetesDeployment} \
                ${kubernetesContainer}=${dockerRegistry}:${dockerImageTag} \
                -n ${kubernetesNamespace}
            else
                echo "Deployment not found. Creating from manifest..."
                kubectl apply -n ${kubernetesNamespace} -f manifest.yml
                kubectl set image deployment/${kubernetesDeployment} \
                ${kubernetesContainer}=${dockerRegistry}:${dockerImageTag} \
                -n ${kubernetesNamespace}
            fi
        """
    }
}
