FROM gitpod/workspace-full-vnc

#RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
#             && sdk install java 12.0.2.j9-adpt \
#             && sdk default java 12.0.2.j9-adpt"
RUN wget https://download2.gluonhq.com/substrate/graalvm/graalvm-svm-linux-20.1.0-ea+26.zip
RUN unzip graalvm-svm-linux-20.1.0-ea+26.zip
ENV GRAALVM_HOME=/workspace/timersfx/graalvm-svm-linux-20.1.0-ea+26
ENV JAVA_HOME=/workspace/timersfx/graalvm-svm-linux-20.1.0-ea+26
