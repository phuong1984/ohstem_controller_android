import bluetooth
import machine
import time
from micropython import const

_IRQ_CENTRAL_CONNECT = const(1)
_IRQ_CENTRAL_DISCONNECT = const(2)
_IRQ_GATTS_WRITE = const(3)

_UART_UUID = bluetooth.UUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
_UART_TX = (bluetooth.UUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"), bluetooth.FLAG_NOTIFY,)
_UART_RX = (bluetooth.UUID("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"), bluetooth.FLAG_WRITE | bluetooth.FLAG_WRITE_NR,)
_UART_SERVICE = (_UART_UUID, (_UART_TX, _UART_RX),)

class ESP32BleRobot:
    def __init__(self, name="OhStem-Robot"):
        self._ble = bluetooth.BLE()
        self._ble.active(True)
        self._ble.irq(self._irq)
        ((self._handle_tx, self._handle_rx),) = self._ble.gatts_register_services((_UART_SERVICE,))
        self._connections = set()
        self._payload = self._advertising_payload(name=name, services=[_UART_UUID])
        self._advertise()
        
        # Hardware Setup (Example)
        self.motor_a = machine.PWM(machine.Pin(12), freq=1000)
        self.motor_b = machine.PWM(machine.Pin(13), freq=1000)

    def _irq(self, event, data):
        if event == _IRQ_CENTRAL_CONNECT:
            conn_handle, _, _ = data
            self._connections.add(conn_handle)
            print("Connected")
        elif event == _IRQ_CENTRAL_DISCONNECT:
            conn_handle, _, _ = data
            self._connections.remove(conn_handle)
            self._advertise()
            print("Disconnected")
        elif event == _IRQ_GATTS_WRITE:
            conn_handle, value_handle = data
            value = self._ble.gatts_read(value_handle)
            if value_handle == self._handle_rx:
                self.on_command_received(value.decode().strip())

    def on_command_received(self, cmd):
        print("Command received:", cmd)
        # Parse command: e.g., "M1=100"
        if cmd.startswith("M1="):
            speed = int(cmd[3:])
            self.motor_a.duty(speed)
        elif cmd.startswith("M2="):
            speed = int(cmd[3:])
            self.motor_b.duty(speed)

    def _advertise(self, interval_us=500000):
        self._ble.gap_advertise(interval_us, adv_data=self._payload)

    def _advertising_payload(self, limited_disc=False, br_edr=False, name=None, services=None):
        payload = bytearray()
        def _append(adv_type, value):
            nonlocal payload
            payload += struct.pack("BB", len(value) + 1, adv_type) + value
        _append(0x01, struct.pack("B", (0x01 if limited_disc else 0x02) + (0x00 if br_edr else 0x04)))
        if name:
            _append(0x09, name)
        if services:
            for s in services:
                _append(0x03, bytes(s))
        return payload

import struct

if __name__ == "__main__":
    robot = ESP32BleRobot()
    while True:
        time.sleep(1)
