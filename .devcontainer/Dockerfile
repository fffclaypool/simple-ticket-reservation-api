FROM mcr.microsoft.com/devcontainers/typescript-node:1-22-bookworm

USER root

ENV JMETER_VERSION=5.6.3
ENV JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ENV PATH=${JMETER_HOME}/bin:${PATH}

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz -o /tmp/jmeter.tgz \
    && tar -xzf /tmp/jmeter.tgz -C /opt \
    && rm /tmp/jmeter.tgz \
    && chmod +x ${JMETER_HOME}/bin/jmeter

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
    && echo "========================="

CMD ["sleep", "infinity"]
