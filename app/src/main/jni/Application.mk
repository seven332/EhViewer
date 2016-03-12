# Copyright 2015 Hippo Seven
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

APP_ABI := all
APP_PLATFORM := android-16
APP_OPTIM := release

APP_CFLAGS += -ffunction-sections -fdata-sections -fPIC
APP_CPPFLAGS += -ffunction-sections -fdata-sections -fvisibility=hidden -fPIC
APP_LDFLAGS += -Wl,--gc-sections -fPIC

NDK_TOOLCHAIN_VERSION := clang
