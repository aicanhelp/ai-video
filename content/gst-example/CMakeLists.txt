cmake_minimum_required(VERSION 3.23)
project(gst-example)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

find_package(PkgConfig REQUIRED)

pkg_search_module(GDK REQUIRED gdk-3.0)
pkg_search_module(GTK REQUIRED gtk+-3.0)
pkg_search_module(GST1 REQUIRED gstreamer-play-1.0)
pkg_search_module(GST1_AUDIO REQUIRED gstreamer-audio-1.0 gstreamer-pbutils-1.0)
pkg_search_module(GST1_PBUTILS REQUIRED gstreamer-pbutils-1.0)
pkg_search_module(JSON_GLIB REQUIRED json-glib-1.0)
pkg_search_module(LIB_SOUP REQUIRED libsoup-2.4)

find_library(M_LIB m)
find_library(X11_LIB X11)
find_library(GSTTAG_LIB gsttag-1.0)

include_directories(
        ${GTK_INCLUDE_DIRS}
        ${GDK_INCLUDE_DIRS}
        ${JSON_GLIB_INCLUDE_DIRS}
        ${LIB_SOUP_INCLUDE_DIRS}
        ${GST1_INCLUDE_DIRS}
        ${GST1_AUDIO_INCLUDE_DIRS}
        ${GST1_PBUTILS_INCLUDE_DIRS}
)

link_libraries(
        ${GTK_LIBRARIES}
        ${GDK_LIBRARIES}
        ${JSON_GLIB_LIBRARIES}
        ${LIB_SOUP_LIBRARIES}
        ${GST1_LIBRARIES}
        ${GST1_AUDIO_LIBRARIES}
        ${GST1_PBUTILS_LIBRARIES}
        ${M_LIB}
        ${X11_LIB}
        ${GSTTAG_LIB}
)

message("include: ${GST1_INCLUDE_DIRS} ${LIB_SOUP_INCLUDE_DIRS} ${JSON_GLIB_INCLUDE_DIRS}")
message("GST1_LIBRARIES ${JSON_GLIB_LIBRARIES};${GST1_LIBRARIES};${GST1_AUDIO_LIBRARIES};${LIB_SOUP_LIBRARIES};${M_LIB}")

set(ENV{LIBVA_DRIVER_NAME} "nvidia")
set(ENV{LIBVA_DRIVERS_PATH} "/snap/vlc/3078/usr/lib/x86_64-linux-gnu/dri")

add_subdirectory(examples)
add_subdirectory(network)
add_subdirectory(webrtc)
add_subdirectory(playback)