
add_executable(webrtc-recvonly-h264 sendonly/webrtc-recvonly-h264.c)
add_executable(webrtc-unidirectional-h264 sendonly/webrtc-unidirectional-h264.c)
add_executable(multiparty-sendrecv multiparty-sendrecv/gst/mp-webrtc-sendrecv.c)
add_executable(sendrecv sendrecv/gst/webrtc-sendrecv.c)