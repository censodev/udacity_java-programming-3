module security {
    requires image;
    requires java.desktop;
    requires miglayout;
    requires com.google.common;
    requires java.prefs;
    requires com.google.gson;

    opens com.udacity.catpoint.security.data to com.google.gson;
}