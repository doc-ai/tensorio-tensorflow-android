#!/bin/bash

# See https://github.com/broadinstitute/gatk/pull/5056/files

# This script's purpose is for use with jitpack.io - a repository to publish snapshot automatically
# This script downloads git-lfs and pull needed sources to build GATK in the jitpack environment

GIT_LFS_VERSION="2.12.0"
GIT_LFS_LINK=https://github.com/git-lfs/git-lfs/releases/download/v${GIT_LFS_VERSION}/git-lfs-linux-amd64-v${GIT_LFS_VERSION}.tar.gz
GIT_LFS="./git-lfs"

NDK_VERSION=19.2.5345600

echo "Downloading and untarring git-lfs binary"
wget -qO- $GIT_LFS_LINK | tar xvz git-lfs

echo "Fetching LFS files."
$GIT_LFS install
$GIT_LFS pull --include distribution/lib

# echo "Installing NDK ${NDK_VERSION}"
# touch /opt/android-sdk-linux/.android/repositories.cfg
# sdkmanager --install "ndk;${NDK_VERSION}"
$ANDROID_HOME/tools/bin/sdkmanager --install "ndk;${NDK_VERSION}"

# ANDROID_NDK_HOME
# NDK_HOME
