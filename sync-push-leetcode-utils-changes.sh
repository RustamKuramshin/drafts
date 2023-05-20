#!/usr/bin/env bash

COPIED_PACKAGE_NAME="package com.rustam.dev.leetcode"
NEW_PACKAGE_NAME="package leetcode.utils"

pwd && ls -la && \
cat ./src/main/java/com/rustam/dev/leetcode/LeetCodeUtils.java > ../leetcode-utils/src/main/java/leetcode/utils/LeetCodeUtils.java && \
cd .. && ls -la && cd leetcode-utils && pwd && ls -la && cd ./src/main/java/leetcode/utils && ls -ls && \
gsed -i "s/${COPIED_PACKAGE_NAME}/${NEW_PACKAGE_NAME}/g" LeetCodeUtils.java
git status && git add -A && git commit -m "init commit" && git push -u origin main