cmake_minimum_required(VERSION 3.0)
project(gst-example)

find_package(PkgConfig REQUIRED)
pkg_search_module(GST1 REQUIRED gstreamer-1.0)

include_directories(${GST1_INCLUDE_DIRS})
#add_executable(mytest 1.c)
#
#target_include_directories(mytest PRIVATE ${GST1_INCLUDE_DIRS})
#target_link_libraries(mytest ${GST1_LIBRARIES})

message("GST1_LIBRARIES ${GST1_LIBRARIES}")

