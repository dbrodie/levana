# ============================================================
# Stage 1: claude-base
# Mirrors Anthropic's reference Dockerfile (node:20 base).
# Upgrade path: when Anthropic updates their Dockerfile, update
# only this stage. The android-dev stage below is untouched.
# Source: https://github.com/anthropics/claude-code/blob/main/.devcontainer/Dockerfile
# ============================================================
FROM --platform=linux/amd64 node:20 AS claude-base

ARG TZ
ENV TZ="$TZ"

ARG CLAUDE_CODE_VERSION=latest

# Install basic development tools and iptables/ipset
RUN apt-get update && apt-get install -y --no-install-recommends \
  less \
  git \
  procps \
  sudo \
  fzf \
  zsh \
  man-db \
  unzip \
  gnupg2 \
  gh \
  iptables \
  ipset \
  iproute2 \
  dnsutils \
  aggregate \
  jq \
  nano \
  vim \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Ensure default node user has access to /usr/local/share
RUN mkdir -p /usr/local/share/npm-global && \
  chown -R node:node /usr/local/share

ARG USERNAME=node

# Persist bash history.
RUN SNIPPET="export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
  && mkdir /commandhistory \
  && touch /commandhistory/.bash_history \
  && chown -R $USERNAME /commandhistory

# Set `DEVCONTAINER` environment variable to help with orientation
ENV DEVCONTAINER=true

# Create workspace and config directories and set permissions
RUN mkdir -p /workspace /home/node/.claude && \
  chown -R node:node /workspace /home/node/.claude

WORKDIR /workspace

ARG GIT_DELTA_VERSION=0.18.2
RUN ARCH=$(dpkg --print-architecture) && \
  wget "https://github.com/dandavison/delta/releases/download/${GIT_DELTA_VERSION}/git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  sudo dpkg -i "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  rm "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb"

# Set up non-root user
USER node

# Install global packages
ENV NPM_CONFIG_PREFIX=/usr/local/share/npm-global
ENV PATH=$PATH:/usr/local/share/npm-global/bin

# Set the default shell to zsh rather than sh
ENV SHELL=/bin/zsh

# Set the default editor and visual
ENV EDITOR=nano
ENV VISUAL=nano

# Default powerline10k theme
ARG ZSH_IN_DOCKER_VERSION=1.2.0
RUN sh -c "$(wget -O- https://github.com/deluan/zsh-in-docker/releases/download/v${ZSH_IN_DOCKER_VERSION}/zsh-in-docker.sh)" -- \
  -p git \
  -p fzf \
  -a "source /usr/share/doc/fzf/examples/key-bindings.zsh" \
  -a "source /usr/share/doc/fzf/examples/completion.zsh" \
  -a "export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
  -x

# Install Claude
RUN npm install -g @anthropic-ai/claude-code@${CLAUDE_CODE_VERSION}


# ============================================================
# Stage 2: android-dev
# Adds Android SDK + JDK on top of claude-base.
# This stage is independent of Anthropic's Dockerfile.
# ============================================================
FROM claude-base AS android-dev

USER root

# JDK 17 + native GL libs for Robolectric NATIVE graphics mode
RUN apt-get update && apt-get install -y --no-install-recommends \
  openjdk-17-jdk \
  libgl1 \
  libegl1 \
  libgles2 \
  wget \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Android SDK — cmdline-tools 11.0 (build 11076708)
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN mkdir -p /opt/android-sdk/cmdline-tools && \
  wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
    -O /tmp/cmdline-tools.zip && \
  unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract && \
  mv /tmp/cmdline-tools-extract/cmdline-tools /opt/android-sdk/cmdline-tools/latest && \
  rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools-extract && \
  chown -R node:node /opt/android-sdk

USER node

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses > /dev/null 2>&1 && \
  sdkmanager \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"
