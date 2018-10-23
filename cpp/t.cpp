void upload();
bool a;
void setup () {
TL_WiFi . init () ;
TL_WiFi . join ( " SSID " , "PASSWORD" ) ;
TL_Light . setMeasuringRange (1 ,30000 , "LUX" ) ; TL_Light . setADCResolution (10) ;
}
void loop() {
TL_Light . read () ; TL_Soil_Moisture . read () ; upload () ;
TL_Time. delayMillis (60000) ;
}
void upload () {
double light = TL_Light.data();
double sm = TL_Soil_Moisture . data () ; String url = "http://hostname/ul.php?"; url += String ( " light=" ) + String ( light ) ; url += String ( "&sm=" ) + String (sm) ; TL_WiFi . get ( url ) ;
}