FROM gitpod/workspace-full-vnc

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
             && sdk install java 12.0.2.j9-adpt \
             && sdk default java 12.0.2.j9-adpt"
