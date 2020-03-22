FROM gitpod/workspace-full-vnc

USER root

#RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
#             && sdk install java 12.0.2.j9-adpt \
#             && sdk default java 12.0.2.j9-adpt"
RUN mkdir /usr/local/graalvm && \
    cd /usr/local/graalvm && \
    wget https://download2.gluonhq.com/substrate/graalvm/graalvm-svm-linux-20.1.0-ea+26.zip && \
    unzip graalvm-svm-linux-20.1.0-ea+26.zip && \
    rm graalvm-svm-linux-20.1.0-ea+26.zip
ENV GRAALVM_HOME=/usr/local/graalvm/graalvm-svm-linux-20.1.0-ea+26
RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
             && sdk install java graal-ea+26 /usr/local/graalvm/graalvm-svm-linux-20.1.0-ea+26 \
             && sdk default java graal-ea+26"
