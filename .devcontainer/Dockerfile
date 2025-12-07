FROM mcr.microsoft.com/devcontainers/typescript-node:1-22-bookworm

USER root

ENV JMETER_VERSION=5.6.3
ENV JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ENV ACT_VERSION=0.2.74
ENV PATH=${JMETER_HOME}/bin:${PATH}

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven \
    curl \
    docker.io \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && usermod -aG docker node

RUN curl -fsSL https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz -o /tmp/jmeter.tgz \
    && tar -xzf /tmp/jmeter.tgz -C /opt \
    && rm /tmp/jmeter.tgz \
    && chmod +x ${JMETER_HOME}/bin/jmeter

RUN curl -fsSL https://github.com/nektos/act/releases/download/v${ACT_VERSION}/act_Linux_x86_64.tar.gz -o /tmp/act.tar.gz \
    && tar -xzf /tmp/act.tar.gz -C /usr/local/bin act \
    && rm /tmp/act.tar.gz \
    && chmod +x /usr/local/bin/act

WORKDIR /home/node/
USER node

RUN npm install -g \
    npm@11.5.2 \
    @anthropic-ai/claude-code

RUN echo "=== Installed Versions ===" \
    && node --version \
    && npm --version \
    && java --version \
    && mvn --version \
    && jmeter --version \
    && act --version \
    && echo "========================="

CMD ["sleep", "infinity"]
