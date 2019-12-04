FROM jenkins/jenkins:2.207

USER jenkins

# Install plugins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

# Configure Jenkins
COPY jenkins.yaml /usr/share/jenkins/ref/jenkins.yaml
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/

# Disable first startup wizard
COPY setupWizard.groovy /usr/share/jenkins/ref/init.groovy.d/setupWizard.groovy