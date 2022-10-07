cmake_minimum_required(VERSION 3.0)
project(gst-example)

find_package(PkgConfig REQUIRED)

pkg_search_module(GDK REQUIRED gdk-3.0)
pkg_search_module(GTK REQUIRED gtk+-3.0)
pkg_search_module(GST1 REQUIRED gstreamer-play-1.0)
pkg_search_module(GST1_AUDIO REQUIRED gstreamer-audio-1.0 gstreamer-pbutils-1.0)
pkg_search_module(GST1_PBUTILS REQUIRED gstreamer-pbutils-1.0)
pkg_search_module(JSON_GLIB REQUIRED json-glib-1.0)
pkg_search_module(LIB_SOUP REQUIRED libsoup-2.4)

find_library(M_LIB m)

include_directories(${GTK_INCLUDE_DIRS})
include_directories(${GDK_INCLUDE_DIRS})
include_directories(${JSON_GLIB_INCLUDE_DIRS})
include_directories(${LIB_SOUP_INCLUDE_DIRS})
include_directories(${GST1_INCLUDE_DIRS})
include_directories(${GST1_AUDIO_INCLUDE_DIRS})
include_directories(${GST1_PBUTILS_INCLUDE_DIRS})

link_libraries(${GTK_LIBRARIES})
link_libraries(${GDK_LIBRARIES})
link_libraries(${JSON_GLIB_LIBRARIES})
link_libraries(${LIB_SOUP_LIBRARIES})
link_libraries(${GST1_LIBRARIES})
link_libraries(${GST1_AUDIO_LIBRARIES})
link_libraries(${GST1_PBUTILS_LIBRARIES})
link_libraries(${M_LIB})

message("include: ${GST1_INCLUDE_DIRS} ${LIB_SOUP_INCLUDE_DIRS} ${JSON_GLIB_INCLUDE_DIRS}")
message("GST1_LIBRARIES ${JSON_GLIB_LIBRARIES};${GST1_LIBRARIES};${GST1_AUDIO_LIBRARIES};${LIB_SOUP_LIBRARIES};${M_LIB}")

add_subdirectory(examples)
add_subdirectory(network)
add_subdirectory(webrtc)
add_subdirectory(playback)