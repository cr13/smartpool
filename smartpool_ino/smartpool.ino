// SmartPool //

//Librerias String
#include <stdlib.h>
#include <string>

//Librerias eeprom y tiempo
#include "EEPROM.h"
#include "time.h"

//Librerias para SD card
#include "FS.h"
#include "SD.h"
#include "SPI.h"

//Librerias para Temperatura
#include <OneWire.h>
#include <DallasTemperature.h>

//Libreria Bluetooth
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>

//Libreria wifi
#include <WiFi.h>
#include <HTTPClient.h>

//Librerias reloj DS3231 RTC
#include <Wire.h>//conecta el bus i2c 
//#include "DS3231.h"
#include "RTClib.h"

using namespace std;

/*
 * Connect the SD card to the following pins:
 *
 * SD Card | ESP32
 *    CS       D5
 *    SCK      D18
 *    MOSI     D23
 *    MISO     D19
 *    VDD      3.3V
 *    GND      GND
 */

/*
 * Conectar DS3231 RTC
 * 
 * GND  GND
 * 3v3  VCC
 * 21   SDA
 * 22   SCL
*/
/********************************************************************/
// Direcciones de EEPROM 
#define EEPROM_SIZE 64
int DireccionHora_fut = 1;
int DireccionNombre = 10;
int DireccionSSID = 27;
int DireccionPass = 45;

/********************************************************************/
//Configuración de conexion wifi y variables para ajustar la fecha y hora
std::string ssid;
std::string password;

/********************************************************************/

// Definicion de variables
RTC_DS3231 RTC;

// Configuración de acceso a la BD
#define API_HOST "https://smartpoolapi.herokuapp.com/XXXXXXX"
#define API_HEADER "Content-Type", "application/x-www-form-urlencoded"

// Data wire is plugged into pin 4 on the ESP32 
#define ONE_WIRE_BUS 4

//Analog Input
#define ANALOG_PIN_0 36 //pin Turbity
int sensorTurbValue = 0;

#define ANALOG_PIN_3 39 //pin Ph  
unsigned long int avgValue; 
int buf[10],aux; //buffer de 10 muestras

// String //
String  inputString_BT            = "" ;
String temp;
String turbi;
String ph;
String fecha;
String datos;
String MAC_BLE;

// Booleanos //
bool BTstring_Complete         = false ; 
bool deviceConnected           = false ;

// Unsigned long //
unsigned long fecha_hora;
unsigned long hora_futura;
DateTime future;
unsigned long Tmax = 216000*2; //2 minuto
unsigned long Texceso = 0;

//Creamos el servidor BLE con los servicios y sus características
BLEServer *pServer = NULL;
BLECharacteristic *pCharacteristic_TX;
BLECharacteristic *pCharacteristic_RX_temp;
BLECharacteristic *pCharacteristic_TX_temp;
BLECharacteristic *pCharacteristic_RX_turb;
BLECharacteristic *pCharacteristic_TX_turb;
BLECharacteristic *pCharacteristic_RX_ph;
BLECharacteristic *pCharacteristic_TX_ph;
BLECharacteristic *pCharacteristic_RX_date;
BLECharacteristic *pCharacteristic_TX_date;
BLECharacteristic *pCharacteristic_RX_name;
BLECharacteristic *pCharacteristic_TX_name;

#define SERVICE_UUID            "0000ffd0-0000-1000-8000-73a310a5bbfd"
#define SERVICE_UUID2           "0000ffa0-0000-1000-8000-73a310a5bbfd"  
#define CHARACTERISTIC_UUID_TX "0000ffe9-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_RX_temp "0000ffd1-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_TX_temp "0000ffe1-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_RX_turb "0000ffd4-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_TX_turb "0000ffe4-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_RX_ph "0000ffd2-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_TX_ph "0000ffe2-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_RX_date "0000ffd3-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_TX_date "0000ffe3-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_RX_name "0000ffd5-0000-1000-8000-73a310a5bbfd"
#define CHARACTERISTIC_UUID_TX_name "0000ffe5-0000-1000-8000-73a310a5bbfd"

/********************************************************************/
// Configurar una instancia de OneWire para comunicarse con cualquier dispositivo OneWire  
OneWire oneWire(ONE_WIRE_BUS);

// Pasa nuestra referencia OneWire a la temperatura de Dallas. 
DallasTemperature DS18B20(&oneWire);
/********************************************************************/ 

// Clases bluetooth
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    };
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {

    
      std::string rxValue = pCharacteristic->getValue();
      if (rxValue.length() > 0) {
        for (int i = 0; i < rxValue.length(); i++)
        {
          inputString_BT += rxValue[i];
        }
        Serial.println("peticion: "+inputString_BT);
      }
      BTstring_Complete = 1;

    }
};

/********************************************************************/
  
//Métodos de escritura y lectura para SD card
void readFile(fs::FS &fs, const char * path){
    if(SD.exists(path)){
      String dataLINE;
      int httpcode=500;
    
      Serial.printf("Reading file: %s\n", path);

      File file = SD.open(path); // Abrir fichero y mostrar el resultado
      if (file ) {
         while (file.available()) { 
           int dataBYTE = file.read(); 
           if (dataBYTE==13) { // Fin de linea
             httpcode=actualizarBD(dataLINE);
             dataLINE = "";
           
           }else
              dataLINE += char(dataBYTE);// Vamos completando la linea 
         }
       
         file.close(); // Cierra fichero
         
         if(httpcode!=500)
           deleteFile(SD, "/datosErrorBD.json");
         
       } else {
          Serial.println(F("Error al abrir el archivo"));
       }  
       
    }else
      Serial.println(F("No hay datos que guardar en BD"));
}

void appendFile(fs::FS &fs, const char * path, String message, boolean finLinea){
    //Serial.printf("Appending to file: %s\n", path);

    File file = fs.open(path, FILE_APPEND);
    if(!file){
        Serial.println("Failed to open file for appending");
        return;
    }
    if(finLinea){
      if(!file.println(message)){
         Serial.println("Append failed finLinea");
      }
    }else
      if(!file.print(message+String(" "))){
         Serial.println("Append failed");
      }
    file.close();
}

void deleteFile(fs::FS &fs, const char * path){
    Serial.printf("Deleting file: %s\n", path);
    if(fs.remove(path)){
        Serial.println("File deleted");
    } else {
        Serial.println("Delete failed");
    }
}


//Método para salvar los datos en la BD
int  actualizarBD(String json){
  HTTPClient http;
  String type = "POST";
  http.begin(API_HOST);
  http.addHeader(API_HEADER);  //Specify content-type header
  String post= "data="+json+"&uid=XxxXXxxxXXxXxXXxxXXx";
  int httpCode = http.POST(post);

  String resultado = http.getString();

  Serial.println(resultado);
  http.end(); 
  
  return(httpCode);
}

//Método para crear y iniciar el serivicio BLE
void createServiceBle(){
   // Definiciones Bluetooth 
  std::string nombre;
  nombre = readEEPROM_String(DireccionNombre);
  Serial.println(String("Nombre del dispositivo ")+nombre.c_str());
  BLEDevice::init(nombre);
  
  // Creación de servidor
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Creación de servicio
  BLEService *pService = pServer->createService(SERVICE_UUID);
  BLEService *pService1 = pServer->createService(SERVICE_UUID2);

  // Definición de características //
  pCharacteristic_TX = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_TX,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
  pCharacteristic_TX->setCallbacks(new MyCallbacks());  
  //////////////////////
  pCharacteristic_TX_temp = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_TX_temp,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
  pCharacteristic_RX_temp = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_RX_temp,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_NOTIFY
                                       );
  pCharacteristic_TX_temp->setCallbacks(new MyCallbacks());
  //////////////////////
  pCharacteristic_TX_turb = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_TX_turb,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE  
                                       );
  pCharacteristic_RX_turb = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_RX_turb,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_NOTIFY
                                       );
  pCharacteristic_TX_turb->setCallbacks(new MyCallbacks());
  //////////////////////
  pCharacteristic_TX_ph = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_TX_ph,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       ); 
  pCharacteristic_RX_ph = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_RX_ph,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_NOTIFY
                                       ); 
  pCharacteristic_TX_ph->setCallbacks(new MyCallbacks());
  //////////////////////////
  pCharacteristic_TX_date = pService1->createCharacteristic(
                                         CHARACTERISTIC_UUID_TX_date,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
  pCharacteristic_RX_date = pService1->createCharacteristic(
                                         CHARACTERISTIC_UUID_RX_date,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_NOTIFY
                                       );
  pCharacteristic_TX_date->setCallbacks(new MyCallbacks());                              
  //////////////////////////
  pCharacteristic_TX_name = pService1->createCharacteristic(
                                         CHARACTERISTIC_UUID_TX_name,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
  pCharacteristic_RX_name = pService1->createCharacteristic(
                                         CHARACTERISTIC_UUID_RX_name,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_NOTIFY
                                       );
  pCharacteristic_TX_name->setCallbacks(new MyCallbacks());
 
   // Arranque del servicio BLE
  pService->start();
  pService1->start();
  pServer->getAdvertising()->start();
  BLEAddress address = BLEDevice::getAddress(); 
  MAC_BLE= address.toString().c_str();
  MAC_BLE.toUpperCase(); 
  
}

//Método para realizar la conexión wifi 
boolean conectarWifi(){
  boolean conectado=false;
  Serial.printf("Connecting to %s ", ssid.c_str());

  WiFi.begin(ssid.c_str(), password.c_str());
  while (WiFi.status() != WL_CONNECTED && Texceso<Tmax) {
      delay(500);
      Texceso= Texceso + millis();
      Serial.print(".");
  }
  if(Texceso>=Tmax){
      Serial.println("Intentalo de nuevo más tarde");
      conectado=false;}
  else{
    Serial.println(" Conectado al wifi");
    conectado=true;
  
  }

  return conectado;
  
}

//Método donde se inician todos los servicios
void setup() {

  //Iniciar puerto serie 
  Serial.begin(115200);
  
  //Deshabilitar BT clásico para liberar 30k de RAM
  esp_bt_controller_mem_release(ESP_BT_MODE_CLASSIC_BT);
   
   // Arranque EEPROM
  if (!EEPROM.begin(EEPROM_SIZE))
  {
    Serial.println("failed to initialise EEPROM"); delay(1000000);
  }

  //clearEEPROM();
  
  //Leemos el ssid y la pass de la eeprom 
  ssid = readEEPROM_String(DireccionSSID);
  password =readEEPROM_String(DireccionPass);

  //Iniciamos servicio de Reloj y leemos la proxima hora de testeo de la eeprom
  Wire.begin();

  // Si el esp32 se reinicia ajustamos la hora
  if (RTC.lostPower()) {
    Serial.println("RTC lost power, lets set the time!");
    RTC.adjust(DateTime(F(__DATE__), F(__TIME__)));
  }
  
  hora_futura=readEEPROM(DireccionHora_fut);
  Serial.println(String("Hora Futura= ")+hora_futura);
  
  //Creamos y levantamos el servicio Bluetooth
  createServiceBle();
  
  delay(1000);
   
  //Preparamos el montahe de la SD card
  if(!SD.begin()){
      Serial.println("Card Mount Failed");
      return;
  }
  uint8_t cardType = SD.cardType();
  
  if(cardType == CARD_NONE){
      Serial.println("No SD card attached");
      return;
  }
  
  Serial.print("SD Card Type: ");
  if(cardType == CARD_MMC){
      Serial.println("MMC");
  } else if(cardType == CARD_SD){
      Serial.println("SDSC");
  } else if(cardType == CARD_SDHC){
      Serial.println("SDHC");
  } else {
      Serial.println("UNKNOWN");
  }
  
  uint64_t cardSize = SD.cardSize() / (1024 * 1024);
  Serial.printf("SD Card Size: %lluMB\n", cardSize);
  Serial.printf("Used space: %lluMB\n", SD.usedBytes() / (1024 * 1024));

  //Iniciamos la conexión WiFi y leemos el fichero "datosErrorBD.json" para recuperar posibles lecturas que se hayan guardado en estado de desconexión
  if(conectarWifi())
     readFile(SD, "/datosErrorBD.json");   

  // Arrancar el bus 1-Wire
  DS18B20.begin(); 
 
}

void loop() { 
  delay(1000);
  Serial.println();
  Serial.println("*******************************************");
  Serial.println(" ");
  
  //Se toman la fecha actual, se imprime por la terminal y se envia por BLE
  DateTime now = RTC.now();

  fecha_hora=now.unixtime();
  String fh=String(now.unixtime());
  Serial.println(String("Hora actual=  ")+fecha_hora);
  if (deviceConnected) {
      pCharacteristic_RX_date->setValue(fh.c_str());
      //delay(10);
      pCharacteristic_RX_date->notify(); 
   }

  Serial.print(now.year(), DEC);
  Serial.print('/');
  Serial.print(now.month(), DEC);
  Serial.print('/');
  Serial.print(now.day(), DEC);
  Serial.print(' ');
  Serial.print(now.hour(), DEC);
  Serial.print(':');
  Serial.print(now.minute(), DEC);
  Serial.print(':');
  Serial.print(now.second(), DEC);
  Serial.println();
  
  //Se toman las lecturas de los sensores
  temp = leerTemperatura();
  turbi = leerTurbidez();
  ph =leerPh();
  
  // Cuando se recibe un string por bluetooth //
  // Si el string recibido comienza por ble se cambia el nombre del bluetooth
  // Si comienza por ssid se cambia la configuración del wifi
  if (BTstring_Complete == 1)
  {     
   
    BTstring_Complete = 0; 
    Serial.println(inputString_BT);
  
    if(inputString_BT.startsWith("ble", 0))
      {
        writeEEPROM_String(DireccionNombre, "");
        Serial.println(String("cambiando el nombre por ")+inputString_BT);
        writeEEPROM_String(DireccionNombre, inputString_BT.substring( 3)); 
        delay(1000);
        ESP.restart();
      }else if (inputString_BT.startsWith("ssid", 0)){
            Serial.println("*******************************************");

            int ssidItem = inputString_BT.indexOf("ssid");            
            int passItem = inputString_BT.indexOf("pass", ssidItem + 1);
            String ssid_new=inputString_BT.substring(4,inputString_BT.indexOf("pass"));
            String pass_new=inputString_BT.substring( passItem + 4);
            writeEEPROM_String(DireccionSSID, "");
            writeEEPROM_String(DireccionPass, "");

            Serial.println(String("cambiando el ssid por ")+inputString_BT);
            writeEEPROM_String(DireccionSSID, ssid_new);
            
            writeEEPROM_String(DireccionPass, pass_new);  
            delay(12000);
            ESP.restart();
        
        }
       
  }


  // Se calcula la proxima hora.
  Serial.println(String("Proxima lectura (actual + 1 hora): ")+future.unixtime());

  //Si es la primera vez o es la hora de tomar test, se guarda el test en la base de datos y en la microSD,
  //También se establece la proxima hora de test
  if(hora_futura == 0 || fecha_hora >=hora_futura){

    // Se calcula la próxima hora de Test
    future = (now + TimeSpan(0,1,0,0));    
    hora_futura=future.unixtime();
    fecha=(String)(fecha_hora-(3600*2));
    writeEEPROM(DireccionHora_fut, hora_futura);
    Serial.println(String("Fecha de salvar datos ")+fecha+String(" Hora Futura= ")+future.unixtime());
    
    // Se salvan los datos en la BD
    datos="{'"+MAC_BLE+"' : {'"+fecha+"' : {'temp':{'value': " + temp + ",'name':'Temperatura', 'unit':'ºC'},'turbi':{'value': "+turbi+",'name':'Turbidez', 'unit':'%'},'ph':{'value': "+ph+",'name':'PH', 'unit':''}}}}";
    
    if(WiFi.status()== WL_CONNECTED){   //Check WiFi estado de la conexión
    
      int  httpResponseCode = actualizarBD(datos); 
      
      if(httpResponseCode>0){
        Serial.println(httpResponseCode);   
      }else{
        Serial.println(httpResponseCode);
      }
      
    }else{
      //En caso de error de conexión, se salvan los datos en la microSD
      if(!conectarWifi()){
        appendFile(SD, "/datosErrorBD.json",datos,true);
        Serial.println("Error wifi desconectado");
      }else
        readFile(SD, "/datosErrorBD.json");   
    }

    // Se salvan los datos en la microSD y hacemos una copia de seguridad de la petición enviada por método post en la microSD
    appendFile(SD, "/datosBD.json",datos,true);
    String fechaSD=String(now.day(), DEC)+"-"+String(now.month(), DEC)+"-"+String(now.year(), DEC)+" "+String(now.hour(), DEC)+":"+String(now.minute(), DEC)+":"+String(now.second(), DEC);
    appendFile(SD, "/datosSD.txt",fechaSD+" "+temp+"  "+ turbi +"  "+ ph,true);
       
  }
  
}

//Función para leer por el sensor( DS18B20 )
String leerTemperatura(){
 DS18B20.requestTemperatures(); // Petición para obtener las lecturas
 String temp=String(DS18B20.getTempCByIndex(0),2);
 Serial.println(String("temperatura ")+ temp+"ºC") ;
 if (deviceConnected) {
    pCharacteristic_RX_temp->setValue(temp.c_str());
    //delay(10);
    pCharacteristic_RX_temp->notify(); 
 }
 return temp;
}

//Función para leer por el sensor( SEN0189 )
String leerTurbidez(){
  sensorTurbValue = analogRead(ANALOG_PIN_0);
  float voltage = sensorTurbValue * (5.0 / 4096.0); // Conversión de la lectura analógica (que va de 0 - 4096) a una tensión (0 - 5V)
  float porcentaje = 100-(voltage*100.0)/5.0; // Conversión a porcentaje
  String turbity=String(porcentaje,2);
  Serial.println("Nivel de turbidez = " + turbity+"%");
  if (deviceConnected) {
      pCharacteristic_RX_turb->setValue(turbity.c_str());
      //delay(10);
      pCharacteristic_RX_turb->notify(); 
   }
  return turbity;
}

//Función para leer por la sonda hidropónica BNC( ph 4502c )
String leerPh(){
  //Lectura ideal entre 7.2 y 7.6

  //Hacemos 10 lecturas
  for(int i=0;i<10;i++) { 
    buf[i]=analogRead(ANALOG_PIN_3);
    delay(10);
 }

 //Ordenamos las medidas obtenidas de menor a mayor
 for(int i=0;i<9;i++){
  for(int j=i+1;j<10;j++){
    if(buf[i]>buf[j]){
      aux=buf[i];
      buf[i]=buf[j];
      buf[j]=aux;
   }
  }
 }
 avgValue=0;
 
//Descartamos las 2 primeras y las 2 ultimas lecturas, y las sumamos
 for(int i=2;i<8;i++)
  avgValue+=buf[i];
  
 //Convierte la lectura analógica (que va de 0 - 4096) a una tensión (0 - 5V) y lo dividimos entre las 6 muestras tomadas.
 float pHVol=(float)avgValue*5.0/4096.0/6; 

 //Para obtener voltageRef_4.01 y voltageRef_7.01 tenemos que hacer dos ecuaciones de dos incognitas
 // voltageRef_4.01 * x + y = 4.01
 //voltageRef_7.01 * x + y = 7.01
 
 //Formula voltageRef_4.01 * voltageActual + voltageRef_7.01
 //float phValue = -2.4 * pHVol + 15.17;
 //float phValue = -2.4 * pHVol + 15.57;
 float phValue = -3.125 * pHVol + 18.0725;
 
 Serial.println(String("Lectura ")+analogRead(ANALOG_PIN_3)+String(" voltage ")+pHVol +" nivel pH = " + phValue);

 
 // pH           Multimetro  Arduino
 // 4.01 --> voltage   2.98      4.48|4.89|4.55
 // 7.01 --> voltage   2.44      3.55|3.55|3.46
 String ph=String(phValue,2);
 if (deviceConnected) {
    pCharacteristic_RX_ph->setValue(ph.c_str());
    //delay(10);
    pCharacteristic_RX_ph->notify(); 
 }
  
  return ph;
}

// Funciones para escribir y leer unsigned long en EEPROM ///////////////
void writeEEPROM(int addr, unsigned long val)
{
  byte B;
  unsigned long aux = val;
  
  for (int i = addr; i < addr + sizeof(val); i++)
  {
      B = byte(aux);
      EEPROM.write(i, B);
      EEPROM.commit();
      aux = aux >> 8; 
      
  }
}
unsigned long readEEPROM(int addr)
{
  byte B;
  unsigned long aux = 0;
  for (int i = addr + sizeof(aux) - 1; i > addr - 1; i--)
  {
      B = EEPROM.read(i);
      aux = aux | B; 
      
      if(i != addr)
      {
       aux = aux << 8; 
      }
  }
  return aux;
}
// Escribir y leer string en EEPROM ///////////////////////////
void writeEEPROM_String(int addr, String val)
{
  byte B;
  String aux = val;
  int k = 0;
  for (int i = addr; i < addr + val.length() + 1; i++)
  {
    if (i == addr)
      {
        EEPROM.write(i, val.length());
        EEPROM.commit();
      }else
      {
      B = byte(val[k]);
      
      EEPROM.write(i, B);
      EEPROM.commit();
      k = k + 1;
      }
   }
}

std::string readEEPROM_String(int addr)
{
  byte B;
  int k = 0;
  int sizeString = EEPROM.read(addr);
  char aux;
  std::string salida;
      
  for (int i = addr + 1; i < addr + sizeString + 1; i++)
  {
      aux = EEPROM.read(i); 
      salida += aux;
      k = k + 1;
   }
   return salida;
}

// Funcion para limpiar la EEPROM
void clearEEPROM()
{
  writeEEPROM_String(DireccionNombre, "MyPool");
  writeEEPROM(DireccionHora_fut, 0);
}
