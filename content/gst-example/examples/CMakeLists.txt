#target_include_directories(mytest PRIVATE ${GST1_INCLUDE_DIRS})
#target_link_libraries(mytest ${GST1_LIBRARIES})

FILE(GLOB TUTORIALS tutorials/*.c)

foreach (TUTORIAL ${TUTORIALS})
    get_filename_component(TUTORIAL_FILE ${TUTORIAL} NAME)
    get_filename_component(TUTORIAL_NAME ${TUTORIAL} NAME_WLE)
#    message("Tutorial: ${TUTORIAL_NAME}")
    add_executable(${TUTORIAL_NAME} tutorials/${TUTORIAL_FILE})
endforeach (TUTORIAL)
