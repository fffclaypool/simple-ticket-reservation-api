# .devcontainer/Dockerfile
FROM mcr.microsoft.com/devcontainers/typescript-node:1-22-bookworm

# Java 17とMavenをインストール
USER root
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

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
    && echo "========================="

# デフォルトコマンド（devContainerではsleep infinityで上書きされる）
CMD ["sleep", "infinity"]
