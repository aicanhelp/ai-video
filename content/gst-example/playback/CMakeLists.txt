cmake_minimum_required(VERSION 3.23)
message("DIR: ${CMAKE_CURRENT_SOURCE_DIR}")

file(GLOB_RECURSE RESOURCE_UI LIST_DIRECTORIES false RELATIVE ${CMAKE_CURRENT_SOURCE_DIR}/player/gtk/resources "*.ui")
add_executable(gst-player player/gst-play/gst-play-kb.c player/gst-play/gst-play-kb.h player/gst-play/gst-play.c)
add_executable(gtk-player player/gtk/gtk-play.c player/gtk/gtk-video-renderer.c player/gtk/gtk-video-renderer.h)
message("UI: ${RESOURCE_UI}")
set_target_properties(gtk-player PROPERTIES RESOURCE "${RESOURCE_UI}")
#target_include_directories(gst-player player/gst-play)