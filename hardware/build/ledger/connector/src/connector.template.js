import TransportWebUSB from "@ledgerhq/hw-transport-webusb";

function getTransport() {
    return TransportWebUSB;
}

function getNodeJsBufferClass() {
    return Buffer;
}

module.exports = {
    getTransport: getTransport,
    getNodeJsBufferClass: getNodeJsBufferClass
};
