FROM gitpod/workspace-full-vnc

USER gitpod

#RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
#             && sdk install java 12.0.2.j9-adpt \
#             && sdk default java 12.0.2.j9-adpt"
RUN wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.2.0/graalvm-ce-java11-linux-amd64-20.2.0.tar.gz && \
    tar -xf graalvm-ce-java11-linux-amd64-20.2.0.tar.gz && \
    rm graalvm-ce-java11-linux-amd64-20.2.0.tar.gz
ENV GRAALVM_HOME=$HOME/graalvm-ce-java11-20.2.0
RUN $GRAALVM_HOME/bin/gu install native-image
RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
             && sdk install java graal-20+2 $GRAALVM_HOME \
             && sdk default java graal-20+2"
