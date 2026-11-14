#!/usr/bin/env groovy


def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """
        
        sh """
            # Update main application deployment - note the correct image name is trainwithshubham/easyshop-app
            sed -i "s|image: trainwithshubham/easyshop-app:.*|image: trainwithshubham/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: trainwithshubham/easyshop-migration:.*|image: trainwithshubham/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
            
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/LondheShubham153/tws-e-commerce-app.git
                git push origin HEAD:\${GIT_BRANCH}
            fi
        """
    }
}
