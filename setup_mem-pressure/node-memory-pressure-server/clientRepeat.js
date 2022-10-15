const ws = new WebSocket(`ws://localhost:4334`);

const Main = () => {
  const [appData, setAppData] = React.useState({});
  const [currDuration, setCurrDuration] = React.useState(1000);
  const [currPressure, setCurrPressure] = React.useState(0);
  const [canModify, setCanModify] = React.useState(false);

  ws.onmessage = (event) => {
    console.log(`received: ${event.data}`);
    setAppData(JSON.parse(event.data));
  }

  // ws.send('sent from client')
  const changeVal = (ev, setFn, key, min, max) => {
    console.log({val: ev.target.value});
    if (ev.target.value !== "") {
      console.log("Came here");
      const newVal = parseInt(ev.target.value > max ? max : (ev.target.value < min ? min : ev.target.value));
      setFn(newVal);
      const sendObj = {};
      sendObj[key] = newVal;
      ws.send(JSON.stringify(sendObj));
    }
  }

  return (
    <div>
      <button></button>
      <form>
        <label htmlFor="duration">Duration:&nbsp;</label>
        <input type="range" min="1000" max="10000" step="500" id="duration" value={currDuration} name="duration" onChange={ev => changeVal(ev, setCurrDuration, `currDuration`, 100, 10000)} />
        {currDuration} ms
        <br></br>
        <label htmlFor="pressure">Pressure:&nbsp;</label>
        <input type="range" min="0" max="100" id="pressure" value={currPressure} name="pressure" onChange={ev => changeVal(ev, setCurrPressure, `currPressure`, 0, 5000)} />
        {currPressure} MB
      </form>
      <h2>Current Stats</h2>
      {Object.keys(appData).map((key, i) => (
        <div key={i}>
          <p><strong>{`${key}: `}</strong>{`${appData[key]}`}</p>
        </div>
      ))}
    </div>
  );
}

ReactDOM.render(<Main />, document.querySelector(`#root`));
