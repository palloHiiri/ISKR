FROM jenkins/jenkins:alpine
USER root
RUN apk add docker
RUN apk add python3
RUN apk add pipx
RUN apk add npm
RUN apk add rsync
RUN apk add gradle
RUN apk add maven
RUN apk add sshpass
RUN pipx install --include-deps ansible
RUN pipx ensurepath
RUN mkdir /home/autodeploy