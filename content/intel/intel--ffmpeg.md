· FFmpeg-vaapi is an FFmpeg plugin, which supplies hardware acceleration based on the low-level VAAPI interface that takes advantage of the industry standard VA API to execute high-performance video codec, video processing, and transcoding capability on Intel GPU.
· FFmpeg-qsv is an FFmpeg plugin, which supplies hardware acceleration based on Intel GPU. It provides high-performance video codec, video processing, and transcoding capability based on Intel Media SDK library.
· FFmpeg-ocl is an FFmpeg plugin, which supplies hardware acceleration based on industrial standard OpenCL on CPU/GPU. It is mainly used to accelerate video processing filters.

· FFmpeg-vaapi是一个FFmpeg插件，它提供基于低级VAAPI接口的硬件加速，利用行业标准VAAPI在英特尔GPU上执行高性能视频编解码器，视频处理和转码功能。
· FFmpeg-qsv是一个FFmpeg插件，提供基于Intel GPU的硬件加速。 它提供基于Intel Media SDK库的高性能视频编解码器，视频处理和转码功能。
· FFmpeg-ocl是一个FFmpeg插件，它在CPU/GPU上提供基于工业标准OpenCL的硬件加速。 它主要用于加速视频处理过滤器。

QSV插件到底层需要经过MSDK、LibVA、UMD和LibDRM

· MSDK

MSDK的全称是Media Software Development Kit，是Intel的媒体开发库，它支持多种图形平台（graphics platforms），实现了通用功能，能对数字视频进行预处理、编解码、以及不同编码格式的转换。该工具的源码地址在Intel® Media SDK，可以在Linux平台上编译使用。
· VA-API

VA-API全称Video Acceleration API，在用户态暴露可以操作核显的API，这是一套类unix平台提供视频硬件加速的开源库和标准，并不是Intel独有，NVIDIA和AMD都有对应的API工具。Intel的源码地址在Intel-vaapi-driver Project，可以在Linux平台上使用，知乎的问题FFmpeg为什么迟迟不启用vaapi解码/编码？的高赞回答可以帮助快速了解VA-API。
· UMD

首先吐槽下Intel的官方的英文缩写实在太多了，而且好多缩写在Google上很难搜到相关的内容，UMD就是个例子。好在官网上解释了：UMD是User Mode Driver的缩写，在这里指的其实是VA-API Driver。而Intel提供了2个工具： intel-vaapi-driver 和 intel-media-driver，前者是历史遗留的，后者是新提供的。推荐使用后者。
· LibDRM

DRM全称是Direct Rendering Manager，即直接渲染管理器，它是为了解决多个程序对 Video Card资源的协同使用问题而产生的。它向用户空间提供了一组 API，用以访问操纵 GPU。该段话引用自Linux 下的 DRM，从Intel官网中还可以知道，DRM还是一套跨多驱动管理的中间件，承接用户态的VA-API和内核态的各类driver。同VA-API，DRM也是一套Linux/Unix平台上通用的解决方案。

