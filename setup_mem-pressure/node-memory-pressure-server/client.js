const ws = new WebSocket(`ws://localhost:4334`);

const TOAST_DURATION = 2000;

const delay = ms => new Promise(
  resolve => setTimeout(resolve, ms)
);

const Main = () => {
  const [appData, setAppData] = React.useState({});
  const [customPressToAdd, setCustomPressToAdd] = React.useState(0);
  const [customPressToRemove, setCustomPressToRemove] = React.useState(0);
  const [toast, setToast] = React.useState(``);
  const [numOfToasts, setNumOfToasts] = React.useState(0);
  const [maxPressure, setMaxPressure] = React.useState(0);
  const [minPressure, setMinPressure] = React.useState(0);
  const [hopPressure, setHopPressure] = React.useState(0);
  const [isMaintaining, setIsMaintaining] = React.useState(false);
  const [maxToMaintain, setMaxToMaintain] = React.useState(0);
  const [minToMaintain, setMinToMaintain] = React.useState(0);
  const [hopToMaintain, setHopToMaintain] = React.useState(0);
  const [maintainMsg, setMaintainMsg] = React.useState(``);

  const generateToast = toast => {
    setNumOfToasts(numOfToasts + 1);
    setToast(toast);
    delay(TOAST_DURATION).then(() => {
      if (numOfToasts <= 1) {
        setToast(``);
      }
      setNumOfToasts(numOfToasts - 1);
    })
  }

  ws.onmessage = ev => {
    console.log(`received: ${ev.data}`);
    const data = JSON.parse(ev.data);
    if (`responseTo` in data) {
      generateToast(data.responseTo);
    } else {
      if (isMaintaining) {
        const availMem = parseInt(appData.cached) + parseInt(appData.free);
        if (availMem < minToMaintain) {
          addPressure(hopToMaintain*-1);
        } else if (availMem > maxToMaintain) {
          addPressure(hopPressure);
        }
      }
    }
    setAppData(data);
  }

  const changeVal = (ev, setFn) => {
    ev.preventDefault();
    if (ev.target.value !== "") {
      setFn(ev.target.value);
    }
  }

  const maintainPressure = (ev) => {
    ev.preventDefault();
    if (!isMaintaining) {
      setIsMaintaining(true);
      setMaxToMaintain(maxPressure);
      setMinToMaintain(minPressure);
      setHopToMaintain(hopPressure);
      setMaintainMsg(`Maintaining Pressure between ${maxPressure}-${minPressure} MB with a jump size of ${hopPressure} MB`);
    } else {
      setIsMaintaining(false);
    }
  };

  const addPressure = (pressure, ev) => {
    if (ev) ev.preventDefault();
    ws.send(JSON.stringify({
      type: `addPressure`,
      pressure: pressure
    }));
  }

  return (
    <div>
      <h2 style={{ display: "inline-block"}}>Apply Pressure&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</h2>
      {toast.length > 0 ? 
        <div style={{ display: "inline-block", padding:"10px", backgroundColor:(toast.length && toast[0] === 'a' ? "#DC0112" : "#3BDA00") }}>
          <span style={{ padding:"0px", color:"#FFFFFF", fontSize:"18px" }}>{toast}</span>
        </div> 
      : undefined}
      <form onSubmit={ev => addPressure(parseInt(customPressToAdd), ev)}>
        <label htmlFor="customPressToAdd">Add:&nbsp;</label>
        <input type="number" id="customPressToAdd" max="2000" min="0" value={customPressToAdd} name="customPressToAdd" onChange={ev => changeVal(ev, setCustomPressToAdd)} />
        <input type="submit" value="+" />
      </form>
      <div style={{ padding:"5px" }}></div>
      {
        [1, 2, 5, 10, 25, 150, 200, 750, 900].map((val, i) => (
          <button key={i} style={{ margin: "3px" }} onClick={() => addPressure(val)}>+{val}MB</button>
        ))
      }
      <div style={{ padding:"10px" }}></div>
      <form onSubmit={ev => addPressure(parseInt(customPressToRemove)*-1, ev)}>
        <label htmlFor="customPressToRemove">Remove:&nbsp;</label>
        <input type="number" id="customPressToRemove" max="2000" min="0" value={customPressToRemove} name="customPressToRemove" onChange={ev => changeVal(ev, setCustomPressToRemove)} />
        <input type="submit" value="-" />
      </form>
      <div style={{ padding:"5px" }}></div>
      {
        [-1, -2, -5, -10, -25, -50, -100, -250, -500].map((val, i) => (
          <button key={i} style={{ margin: "3px" }} onClick={() => addPressure(val)}>{val}MB</button>
        ))
      }
      <br></br>
      <h3>Maintain Pressure</h3>
      <form style={{ display: "inline-block" }} onSubmit={ev => maintainPressure(ev)}>
        <label htmlFor="maxPressure">Max:&nbsp;</label>
        <input type="number" id="maxPressure" max="1000" min="0" value={maxPressure} name="maxPressure" onChange={ev => changeVal(ev, setMaxPressure)} />
        <label htmlFor="minPressure">&nbsp;&nbsp;Min:&nbsp;</label>
        <input type="number" id="minPressure" max="1000" min="0" value={minPressure} name="minPressure" onChange={ev => changeVal(ev, setMinPressure)} />
        <label htmlFor="hopPressure">&nbsp;&nbsp;Jump:&nbsp;</label>
        <input type="number" id="hopPressure" max="1000" min="0" value={hopPressure} name="hopPressure" onChange={ev => changeVal(ev, setHopPressure)} />
        &nbsp;&nbsp;&nbsp;
        <input type="submit" value={isMaintaining ? "Stop Maintaining" : "Maintain"} />
      </form>
      <div style={{ padding:"7px" }}></div>
      {isMaintaining ? 
        <div style={{ display: "inline-block", padding:"5px", backgroundColor:"#FFF93D" }}>
          <span style={{ padding:"0px", color:"#000000", fontSize:"15px" }}>{maintainMsg}</span>
        </div> 
      : undefined}
      
      <div style={{ padding:"5px" }}></div>
      <h2 style={{ display: "inline-block"}}>Statistics&nbsp;&nbsp;&nbsp;</h2>
      {appData.timeStamp ? <span>Last Logged: {new Date(parseInt(appData.timeStamp)).toLocaleTimeString("en-US")}</span> : undefined}
      <br></br>
      {[
        ["Available Mem", () => appData.cached ? `${parseInt(appData.cached) + parseInt(appData.free)} MB` : undefined],
        ["Mem State", () => appData.stateMsg ? appData.stateMsg : undefined],
        ["Mem Pr. App PSS", () => appData.pressure ? `${appData.pressure} MB` : undefined],
        ["Cached", () => appData.cached ? `${appData.cached} MB` : undefined],
        ["Free", () => appData.free ? `${appData.free} MB` : undefined]
      ].map((val, i) => (
        <div key={i} style={{ paddingTop:"12px", paddingBottom:"12px", margin:"5px", display: "inline-block", width:"150px", backgroundColor:"#DEDEDE" }}>
          <span style={{ marginBottom:"5px", display: "block", textAlign:"center", fontSize:"18px" }}><strong>{val[0]}</strong></span>
          {val[1]() ? <div style={{ textAlign:"center", fontSize:"18px" }}>{val[1]()}</div> : undefined}
        </div>
      ))}
      {Object.keys(appData).map((key, i) => (
        <div key={i}>
          <p><strong>{`${key}: `}</strong>{`${appData[key]}`}</p>
        </div>
      ))}
    </div>
  );

}

ReactDOM.render(<Main />, document.querySelector(`#root`));
