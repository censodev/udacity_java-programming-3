module image {
    requires org.slf4j;
    requires software.amazon.awssdk.services.rekognition;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    requires java.desktop;
    requires software.amazon.awssdk.core;

    exports com.udacity.catpoint.image.service;
}