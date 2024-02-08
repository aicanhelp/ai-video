#include <gst/gst.h>



int main(int argc, char *argv[]) {
    GstElement *pipeline;
    GstBus *bus;
    GstMessage *msg;

    /* Initialize GStreamer */
    gst_init(&argc, &argv);

    gchar *file1="playbin uri=https://www.freedesktop.org/software/gstreamer-sdk/data/media/sintel_trailer-480p.webm";
    gchar *file2="playbin uri=file:////home/modongsong/data/test-data/sintel_trailer-480p.webm";

    /* Build the pipeline */
    pipeline =gst_parse_launch(file2,NULL);

    /* Start playing */
    gst_element_set_state(pipeline, GST_STATE_PLAYING);

    /* Wait until error or EOS */
    bus = gst_element_get_bus(pipeline);
    msg = gst_bus_timed_pop_filtered(bus, GST_CLOCK_TIME_NONE,
                                     GST_MESSAGE_ERROR | GST_MESSAGE_EOS);

    /* See next tutorial for proper error message handling/parsing */
    if (GST_MESSAGE_TYPE(msg) == GST_MESSAGE_ERROR) {
        g_error("An error occurred! Re-run with the GST_DEBUG=*:WARN environment "
                "variable set for more details.");
    }

    /* Free resources */
    gst_message_unref(msg);
    gst_object_unref(bus);
    gst_element_set_state(pipeline, GST_STATE_NULL);
    gst_object_unref(pipeline);

    return 0;
}
