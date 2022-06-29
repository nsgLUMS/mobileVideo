const express = require('express');
const cors = require(`cors`);
const WebSocket = require(`ws`);
const net = require('net');

const IP_ADDRESS = `192.168.1.106`;

// port to listen at
const PORT_LISTEN = 4333;
// port to open socket at
const PORT_SOCKET = 4334;

// port to open socket with Android App:
const ANDROID_PORT_SOCKET = 4335;

// set up express app
const app = express();

// set up the client socket
const wss = new WebSocket.Server({ port: PORT_SOCKET });

// global variables
let currDuration = 1000;
let currPressure = 0;

app.use(cors({ origin: true, credentials: true }));

app.use(express.static("./"));

// use parser middleware
app.use(express.json()); // Parse JSON

// initialize routes
app.post('/api', (req, res) => {
  // console.log({"req.body": req.body});

  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(req.body));
    }
  })
  // console.log({currDuration, currPressure});
  res.send(JSON.stringify({ currDuration, currPressure }));
});

// error handling (from routes) middleware
app.use((err, req, res, next) => {
    console.log(err); // to see properties of message in our console
    res.status(422).send({ error: err.message });
});

// listen for requests
app.listen(PORT_LISTEN, () => {
  console.log(`Listening for requests at port ${PORT_LISTEN}`);
});

const phonesList = [];

const sendToPhone = (sendObj) => {
  phonesList.forEach((sock) => {
    sock.write(JSON.stringify(sendObj) + `\n`);
  })
}

const addPressure = (pressure) => {
  sendToPhone({ type: `addPressure`, pressure });
}

wss.on(`connection`, ws => {
  console.log(`A user connected`);
  // ws.send(JSON.stringify({hello: 65, decimal: 34}));

  ws.on(`message`, message => {
    // console.log(`received: ${message}`);
    const dataReceived = JSON.parse(message);
    if ("currDuration" in dataReceived) currDuration = dataReceived.currDuration;
    if ("currPressure" in dataReceived) currPressure = dataReceived.currPressure;

    if ("type" in dataReceived) {
      if (dataReceived.type === `addPressure`) addPressure(dataReceived.pressure);
    }
  });
});

const delay = ms => new Promise(
    resolve => setTimeout(resolve, ms)
  );

const androidSocketServer = net.createServer((sock) => {

  // Receives a connection - a socket object is associated to the connection automatically
  console.log(`CONNECTED: ` + sock.remoteAddress +`:`+ sock.remotePort);

  sock.setEncoding('utf8');
  
  phonesList.push(sock);

  sock.on(`data`, (data) => {
    console.log(`recieved:`, data);

    wss.clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    });
  });

  sock.on(`close`, (hadError) => {
    // closed connection
    console.log(`CLOSED: ` + sock.remoteAddress + `:` + sock.remotePort);
    phonesList.splice(phonesList.findIndex(lSock => lSock === sock), 1);
  })

});

androidSocketServer.listen(ANDROID_PORT_SOCKET, IP_ADDRESS, () => {
  console.log(`Listening for sockets at port ${ANDROID_PORT_SOCKET}`);
});
