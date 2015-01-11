# EhViewer

这是一个 Android 平台上的 E-Hentai 阅读应用。访问 [EhViewer](http://www.ehviewer.com/) 网站以获取更多信息。<br>
EhViewer is an E-Hentai Application for Android. Visit [EhViewer](http://www.ehviewer.com/)(Chinese Only) website for more information.

# Build

本项目使用 Eclipse + ADT 作为开发环境，如果你不清楚如何配置，请自行搜索。clone 之后直接导入 Eclipse 即可编译。我没试过将其迁至 Android Studio，如果你喜欢使用 Android Studio，你需要自己研究下，项目中有 NDK 部分，迁移时要注意。<br>
目前 Android NDK (r10d) 只对 x86 使用 yasm，而 x86\_64 没有启用 yasm，但是 libjpeg-turbo 使用了汇编代码。如果要编译 x86_64 的 jni 部分需要对 Android NDK 中的脚本进行修改。<br>
Just import into Eclipse and build. Do it youself if you prefer Android Studio.

##针对 x86\_64 yasm 所做的修改
build/core/build-binary.mk<br>
将（注意第三行）<br>
```markdown
# all_source_patterns contains the list of filename patterns that correspond
# to source files recognized by our build system
ifeq ($(TARGET_ARCH_ABI),x86)
all_source_extensions := .c .s .S .asm $(LOCAL_CPP_EXTENSION) $(LOCAL_RS_EXTENSION)
else
all_source_extensions := .c .s .S $(LOCAL_CPP_EXTENSION) $(LOCAL_RS_EXTENSION)
endif
all_source_patterns   := $(foreach _ext,$(all_source_extensions),%$(_ext))
all_cpp_patterns      := $(foreach _ext,$(LOCAL_CPP_EXTENSION),%$(_ext))
all_rs_patterns       := $(foreach _ext,$(LOCAL_RS_EXTENSION),%$(_ext))
```
改为<br>
```markdown
# all_source_patterns contains the list of filename patterns that correspond
# to source files recognized by our build system
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), x86 x86_64))
all_source_extensions := .c .s .S .asm $(LOCAL_CPP_EXTENSION) $(LOCAL_RS_EXTENSION)
else
all_source_extensions := .c .s .S $(LOCAL_CPP_EXTENSION) $(LOCAL_RS_EXTENSION)
endif
all_source_patterns   := $(foreach _ext,$(all_source_extensions),%$(_ext))
all_cpp_patterns      := $(foreach _ext,$(LOCAL_CPP_EXTENSION),%$(_ext))
all_rs_patterns       := $(foreach _ext,$(LOCAL_RS_EXTENSION),%$(_ext))
```
然后在<br>
```markdown
ifeq ($(TARGET_ARCH_ABI),x86)
$(foreach src,$(filter %.asm,$(LOCAL_SRC_FILES)), $(call compile-asm-source,$(src),$(call get-object-name,$(src))))
endif
```
的下方添加<br>
```markdown
ifeq ($(TARGET_ARCH_ABI),x86_64)
$(foreach src,$(filter %.asm,$(LOCAL_SRC_FILES)), $(call compile-asm-64-source,$(src),$(call get-object-name,$(src))))
endif
```
build/core/definitions.mk<br>
添加
```markdown
# -----------------------------------------------------------------------------
# Template  : ev-compile-asm-64-source
# Arguments : 1: single ASM source file name (relative to LOCAL_PATH)
#             2: target object file (without path)
# Returns   : None
# Usage     : $(eval $(call ev-compile-asm-64-source,<srcfile>,<objfile>)
# Rationale : Internal template evaluated by compile-asm-64-source
# -----------------------------------------------------------------------------
define  ev-compile-asm-64-source
_SRC:=$$(call local-source-file-path,$(1))
_OBJ:=$$(LOCAL_OBJS_DIR:%/=%)/$(2)

_FLAGS := $$(call host-c-includes,$$(LOCAL_C_INCLUDES) $$(LOCAL_PATH)) \
          $$(LOCAL_ASMFLAGS) \
          $$(NDK_APP_ASMFLAGS) \
          $$(call host-c-includes,$$($(my)C_INCLUDES)) \
          -f elf64 -m amd64 -DELF -D__x86_64__ -DPIC

_TEXT := Assemble $$(call get-src-file-text,$1)
_CC   := $$(NDK_CCACHE) $$(TARGET_ASM)

$$(_OBJ): PRIVATE_ABI      := $$(TARGET_ARCH_ABI)
$$(_OBJ): PRIVATE_SRC      := $$(_SRC)
$$(_OBJ): PRIVATE_OBJ      := $$(_OBJ)
$$(_OBJ): PRIVATE_MODULE   := $$(LOCAL_MODULE)
$$(_OBJ): PRIVATE_TEXT     := $$(_TEXT)
$$(_OBJ): PRIVATE_CC       := $$(_CC)
$$(_OBJ): PRIVATE_CFLAGS   := $$(_FLAGS)

ifeq ($$(LOCAL_SHORT_COMMANDS),true)
_OPTIONS_LISTFILE := $$(_OBJ).cflags
$$(_OBJ): $$(call generate-list-file,$$(_FLAGS),$$(_OPTIONS_LISTFILE))
$$(_OBJ): PRIVATE_CFLAGS := @$$(call host-path,$$(_OPTIONS_LISTFILE))
$$(_OBJ): $$(_OPTIONS_LISTFILE)
endif

$$(call generate-file-dir,$$(_OBJ))
$$(_OBJ): $$(_SRC) $$(LOCAL_MAKEFILE) $$(NDK_APP_APPLICATION_MK) $$(NDK_DEPENDENCIES_CONVERTER) $(LOCAL_RS_OBJECTS)
    $$(call host-echo-build-step,$$(PRIVATE_ABI),$$(PRIVATE_TEXT)) "$$(PRIVATE_MODULE) <= $$(notdir $$(PRIVATE_SRC))"
    $$(hide) $$(PRIVATE_CC) $$(PRIVATE_CFLAGS) $$(call host-path,$$(PRIVATE_SRC)) -o $$(call host-path,$$(PRIVATE_OBJ))
endef

# -----------------------------------------------------------------------------
# Function  : compile-asm-64-source
# Arguments : 1: single Assembly source file name (relative to LOCAL_PATH)
#             2: object file
# Returns   : None
# Usage     : $(call compile-asm-64-source,<srcfile>,<objfile>)
# Rationale : Setup everything required to build a single Assembly source file
# -----------------------------------------------------------------------------
compile-asm-64-source = $(eval $(call ev-compile-asm-64-source,$1,$2))
```

# Thanks

- [android-gif-drawable](https://github.com/koral--/android-gif-drawable)
    - 参考 gif 解码
- [ActionBar-PullToRefresh](https://github.com/chrisbanes/ActionBar-PullToRefresh)
    - 曾作为刷新控件
- [ActionBarSherlocke](https://github.com/JakeWharton/ActionBarSherlock)
    - 曾用来对 4.0 一些系统兼容
- [Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh)
    - 曾作为刷新控件
- [AndroidStaggeredGrid](https://github.com/etsy/AndroidStaggeredGrid)
    - 列表控件
- [Apollo-CM](https://github.com/adneal/Apollo-CM)
    - 缓存机制
- [DiskLruCache](https://github.com/JakeWharton/DiskLruCache)
    - 缓存机制
- [FloatingActionButton](https://github.com/FaizMalkani/FloatingActionButton)
    - 按钮控件
- [GIFLIB](http://giflib.sourceforge.net)
    - gif 解码
- [Gloria Hallelujah](https://www.google.com/fonts/specimen/Gloria+Hallelujah)
    - 启动界面字体
- [ImageLoadingPattern](https://github.com/rnrneverdies/ImageLoadingPattern)
    - 图片过渡效果
- [libjpeg-turbo](http://www.libjpeg-turbo.org)
    - jpge 解码
- [libpng](http://www.libpng.org/pub/png/libpng.html)
    - png 解码
- [material-menu](https://github.com/balysv/material-menu)
    - 抽屉指示器
- [OpenCC](https://github.com/BYVoid/OpenCC)
    - 繁化工作
- [Slabo](https://github.com/TiroTypeworks/Slabo)
    - 图标字体
- [SlidingMenu](https://github.com/jfeinstein10/SlidingMenu)
    - 曾作为抽屉控件
- [source-code-pro](https://github.com/adobe/source-code-pro)
    - 刷新控件字体
- [svg-android](https://code.google.com/p/svg-android/)
    - svg 解析
- [SystemBarTint](https://github.com/jgilfelt/SystemBarTint)
    - 状态栏背景设置
- [twoway-view](https://github.com/lucasr/twoway-view)
    - RecyclerView 使用方法
- [zlib](http://www.zlib.net)
    - 辅助 png 解码
- [Android Open Source Project](http://source.android.com)
    - 很多很多
- [commons-lang](http://commons.apache.org/proper/commons-lang)
    - 很多很多

# License

本项目内容采用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 授权，
部分文件采用 [Creative Commons Attribution 3.0 Unported License](http://creativecommons.org/licenses/by/3.0/) 授权（[列表](rc/list1)），
部分文件采用 [SIL Open Font License, 1.1](http://scripts.sil.org/OFL) 授权（[列表](rc/list2)）。
<br>
The content of this project itself is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0),
some files are licensed under the [Creative Commons Attribution 3.0 Unported License](http://creativecommons.org/licenses/by/3.0/) ([list](rc/list1)),
some files are licensed under the [SIL Open Font License, 1.1](http://scripts.sil.org/OFL) ([list](rc/list2)).
