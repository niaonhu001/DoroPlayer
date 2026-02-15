#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

class MyCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    std::string value = pCharacteristic->getValue();
    if (value.length() > 0) {
      Serial.print("Received message: ");
      for (size_t i = 0; i < value.length(); i++) {
        Serial.print(value[i]);
      }
      Serial.println();
    }
  }
};

// 断开连接时重新开始广播，这样 App 里才能再次扫到设备
class MyServerCallbacks: public BLEServerCallbacks {
  void onDisconnect(BLEServer* pServer) {
    Serial.println("Client disconnected, restart advertising...");
    BLEDevice::startAdvertising();
  }
};

BLEServer* pServer = nullptr;

void setup() {
  Serial.begin(115200);
  Serial.println("Starting BLE Server...");

  BLEDevice::init("The Handy");

  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());  // 设置断开回调

  BLEService *pService = pServer->createService(SERVICE_UUID);

  BLECharacteristic *pCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
  pCharacteristic->setCallbacks(new MyCallbacks());

  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  Serial.println("BLE server is ready. Waiting for writes...");
}

void loop() {
  delay(2000);
}
