# .devcontainer/Dockerfile
FROM mcr.microsoft.com/devcontainers/typescript-node:1-22-bookworm

# Java 17、Maven、JMeterをインストール
USER root

# JMeterのバージョン
ENV JMETER_VERSION=5.6.3
ENV JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ENV PATH=${JMETER_HOME}/bin:${PATH}

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# JMeterのダウンロードとインストール
RUN curl -fsSL https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz -o /tmp/jmeter.tgz \
    && tar -xzf /tmp/jmeter.tgz -C /opt \
    && rm /tmp/jmeter.tgz \
    && chmod +x ${JMETER_HOME}/bin/jmeter

WORKDIR /home/node/
USER node

# Claude Codeとnpmを最新版にアップデート
RUN npm install -g \
    npm@11.5.2 \
    @anthropic-ai/claude-code

# バージョン確認用コマンド（デバッグ用）
RUN echo "=== Installed Versions ===" \
    && node --version \
    && npm --version \
    && java --version \
    && mvn --version \
    && jmeter --version \
    && echo "========================="

# デフォルトコマンド（devContainerではsleep infinityで上書きされる）
CMD ["sleep", "infinity"]
