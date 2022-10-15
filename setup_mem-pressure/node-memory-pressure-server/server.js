const express = require('express');
const cors = require(`cors`);
const WebSocket = require(`ws`);
const net = require('net');
const fs = require(`fs`);

const IP_ADDRESS = `192.168.0.188`;
const LOG_FILE = `/home/motamid/Documents/Research/memory_pressure_server/mem_pressure_app_log`;

// port to listen at
const PORT_LISTEN = 4333;
// port to open socket with webpage
const PORT_SOCKET = 4334;

// port to open socket with Android App:
const ANDROID_PORT_SOCKET = 4335;

// port to communicate a particular state's arrival to
const RUN_TESTS_PORT = 4336;

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

const delay = ms => new Promise(
  resolve => setTimeout(resolve, ms)
);

let haltApproach = false;
let stateToAchieve = '';
let isStateBeingApproached = false;
let appData = undefined;

const isStateReached = () => {
  if (stateToAchieve === 'moderate') {
    return appData.stateMsg === 'Moderate' || appData.stateMsg === 'Low' || appData.stateMsg === 'Critical';
  } else if (stateToAchieve === 'low') {
    return appData.stateMsg === 'Low' || appData.stateMsg === 'Critical';
  } else if (stateToAchieve === 'critical') {
    return appData.stateMsg === 'Critical';
  } else {
    return true;
  }
}

// const isStateModerate = () => {
//   console.log({ stateMsg: appData.stateMsg });
//   return appData.stateMsg == 'Moderate' || appData.stateMsg == 'Low' || appData.stateMsg == 'Critical';
// }

// For Nexus 6P
const pressureBoundaries = [
  { threshold: 50, pressure: 1000, delay: 15000, done: false, applyOnce: true }, // this done is set by the applymoderate function
  { threshold: 1200, pressure: 25, delay: 2000, done: false, applyOnce: false }, // the rest fo the dones are set by setAppData
  { threshold: 1250, pressure: 5, delay: 2000, done: false, applyOnce: false },
  { threshold: 3000, pressure: 2, delay: 2000, done: false, applyOnce: false }
]

// For Nexus 5
// const pressureBoundaries = [
//   { threshold: 50, pressure: 700, delay: 15000, done: false, applyOnce: true }, // this done is set by the applymoderate function
//   { threshold: 975, pressure: 25, delay: 2000, done: false, applyOnce: false }, // the rest fo the dones are set by setAppData
//   { threshold: 1040, pressure: 5, delay: 2000, done: false, applyOnce: false },
//   { threshold: 2000, pressure: 1, delay: 2000, done: false, applyOnce: false }
// ]

// // For Nokia 1
// const pressureBoundaries = [
//   { threshold: 2000, pressure: 1, delay: 2000, done: false, applyOnce: false }
// ]

const approachState = (firstCall) => {

  if (haltApproach) {
    isStateBeingApproached = false;
    haltApproach = false;
    return;
  }

  // check if function is already invoked:
  if (!(firstCall && isStateBeingApproached)) {

    isStateBeingApproached = true;
    
    if (isStateReached()) {
      
      console.log(`1. State Reached!!!`);
      console.log(`2. State Reached!!!`);
      console.log(`3. State Reached!!!`);

      isStateBeingApproached = false;

    } else {
      
      const i = pressureBoundaries.findIndex(boundary => boundary.done == false);
      
      addPressure(pressureBoundaries[i].pressure);
      
      if (pressureBoundaries[i].applyOnce) pressureBoundaries[i].done = true;

      delay(pressureBoundaries[i].delay).then(() => {
        approachState(false);
      });
      
    }

  }

}

// const approachModerate = () => {

//   // check if function is already invoked:
//   if (!getToModerate) {

//     getToModerate = true;

//     if (isStateModerate()) {

//       getToModerate = false;

//     } else {
      
//       const i = pressureBoundaries.findIndex(boundary => boundary.done == false);
      
//       addPressure(pressureBoundaries[i].pressure);
      
//       if (pressureBoundaries[i].applyOnce) {
//         pressureBoundaries[i].done = true;
//       }

//       delay(pressureBoundaries[i].delay).then(() => {
//         getToModerate = false;
//         approachModerate();
//       });
      
//     }

//   }

// }

const setAppData = newAppData => {
  appData = newAppData;
  for (let i = 0; i < pressureBoundaries.length; i++) {
    if (!pressureBoundaries[i].done && appData.pressure > pressureBoundaries[i].threshold) {
      pressureBoundaries[i].done = true;
    }
  }
}

const logData = (data) => {
  fs.appendFile(LOG_FILE, `${data}\n`, (writeErr) => {
    if (writeErr) {
      console.log(`Error while writing '${data}' in '${LOG_FILE}': ${writeErr}`);
    }
  })
}

// initialize routes
app.post('/api', (req, res) => {
  // console.log({"req.body": req.body});
  logData(JSON.stringify(req.body) + `\n`);
  setAppData(req.body);
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(req.body));
    }
  })
  // console.log({currDuration, currPressure});
  res.send(JSON.stringify({ currDuration, currPressure }));
});

app.post('/approach/:state', (req, res) => {
  haltApproach = false;
  stateToAchieve = req.params.state;
  if (!isStateBeingApproached) {
    // refresh state
    for (let i = 0; i < pressureBoundaries.length; i++) {
      pressureBoundaries[i].done = false;
    }
    // apply initial stuff
    if (appData.pressure) {
      for (let i = 0; i < pressureBoundaries.length; i++) {
        if (!pressureBoundaries[i].done && appData.pressure > pressureBoundaries[i].threshold) {
          pressureBoundaries[i].done = true;
        }
      }
    }
    approachState(true);
  }
  res.status(200).send(`Approaching ${stateToAchieve}`);
});

app.post('/halt-approach', (req, res) => {
  if (isStateBeingApproached) {
    haltApproach = true;
    res.status(200).send(`Halted approach to ${stateToAchieve}`);
  } else {
    res.status(200).send(`No approach in progress`);
  }
});

app.post('/get-app-data', (req, res) => {
  if (appData) {
    res.status(200).send(JSON.stringify(appData));
  } else {
    res.status(503).send(JSON.stringify({ error: `Do not have data yet` }));
  }
});

app.post(`/apply-pressure/:pressure`, (req, res) => {
  console.log(`Applying pressure of ${req.params.pressure}`);
  addPressure(parseInt(req.params.pressure));
  res.status(200).send(`Applying pressure of ${req.params.pressure}`);
});

// app.get('/apply-moderate', (req, res) => {
//   for (let i = 0; i < pressureBoundaries.length; i++) {
//     pressureBoundaries[i].done = false;
//   }
//   approachModerate();
//   res.status(200).send('applying');
// });

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
  console.log(`A webpage connected`);
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


const androidSocketServer = net.createServer((sock) => {

  // Receives a connection - a socket object is associated to the connection automatically
  console.log(`CONNECTED: ` + sock.remoteAddress +`:`+ sock.remotePort);

  sock.setEncoding('utf8');
  
  phonesList.push(sock);

  sock.on(`data`, (data) => {
    console.log(`recieved:`, data);

    setAppData(JSON.parse(data));

    logData(data + `\n`);

    wss.clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    });
  });

  sock.on('error', (err) => {
    console.log(`error in socket:`, err);
  })

  .on(`close`, (hadError) => {
    // closed connection
    console.log(`CLOSED: ` + sock.remoteAddress + `:` + sock.remotePort);
    phonesList.splice(phonesList.findIndex(lSock => lSock === sock), 1);
  })

});

androidSocketServer.on('error', (err) => {
  console.log(`error in socket server:`, err);
})

androidSocketServer.listen(ANDROID_PORT_SOCKET, IP_ADDRESS, () => {
  console.log(`Listening for sockets at port ${ANDROID_PORT_SOCKET}`);
});
